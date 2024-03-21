package opc.junit.multi.corporates;

import commons.models.CompanyModel;
import commons.models.CompanyPosition;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CorporateSourceOfFunds;
import opc.enums.opc.CountryCode;
import opc.enums.opc.Industry;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.AddressModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CreateCorporatesTests extends BaseCorporatesSetup {

    final private static int VERIFICATION_TIME_LIMIT = 90;

    @ParameterizedTest
    @EnumSource(value = CompanyType.class, mode = EnumSource.Mode.EXCLUDE, names = {"NON_PROFIT_ORGANISATION"})
    public void CreateCorporate_Success(final CompanyType companyType) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel
                                .defaultCompanyModel()
                                .setType(companyType.name())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    /**
     * It is not allowed to NonProfitOrganization company type anymore, except companies that are registered from Germany
     */
    @Test
    public void CreateCorporate_NonProfitOrganization_CompanyTypeUnsupported() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel
                                .defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.getRandomWithExcludedCountry(CountryCode.DE).name())
                                .setType(CompanyType.NON_PROFIT_ORGANISATION.name())
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COMPANY_TYPE_UNSUPPORTED"));
    }

    /**
     * In this test, we are checking if a company that is registered from Germany is created successfully.
     */
    @Test
    public void CreateCorporate_AllowGermanNonProfitOrganization_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.DE.name())
                                .setType(CompanyType.NON_PROFIT_ORGANISATION.name())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @ParameterizedTest
    @EnumSource(value = Industry.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void CreateCorporate_Success(final Industry industry) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setIndustry(industry)
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_TagNotRequired_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setTag(null)
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_RequiredOnly_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setTag(null)
                        .setFeeGroup(null)
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_RequiredOnlyInCompanyDetails_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel.newBuilder()
                                .setName(RandomStringUtils.randomAlphabetic(10))
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .setRegistrationCountry(CountryCode.MT.name())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_RequiredOnlyInBusinessAddress_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel.newBuilder()
                                .setName(RandomStringUtils.randomAlphabetic(10))
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .setRegistrationCountry(CountryCode.MT.name())
                                .setRegistrationNumber(RandomStringUtils.randomAlphanumeric(10))
                                .setBusinessAddress(AddressModel.DefaultAddressModel()
                                        .setAddressLine2(null)
                                        .setState(null)
                                        .build())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, mode = EnumSource.Mode.EXCLUDE, names = {"AF", "GB", "GG", "IM", "JE"})
    public void CreateCorporate_CountryChecks_Success(final CountryCode countryCode) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(countryCode.name())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, mode = EnumSource.Mode.INCLUDE, names = {"GB", "GG", "IM", "JE"})
    public void CreateCorporate_UkJurisdictionCountryChecks_Success(final CountryCode countryCode) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOneUk.getCorporatesProfileId())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(countryCode.name())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, applicationOneUk.getSecretKey(), Optional.empty()), createCorporateModel, applicationOneUk.getCorporatesProfileId());
    }

    @Test
    public void CreateCorporate_NoRegistrationNumber_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel.newBuilder()
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .setRegistrationCountry(CountryCode.MT.name())
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_RequiredOnlyInRootUserDetails_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(null)
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_MissingSourceOfFundsOtherRequiredIfOtherSourceOfFundsChosen_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setSourceOfFunds(CorporateSourceOfFunds.OTHER)
                        .setSourceOfFundsOther(null)
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_IndustryMissing_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setIndustry(null)
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }

    @Test
    public void CreateCorporate_SourceOfFundsMissing_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setSourceOfFunds(null)
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), createCorporateModel);
    }


    @Test
    public void CreateCorporate_UnknownIndustry_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setIndustry(Industry.UNKNOWN)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_UnknownSourceOfFunds_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setSourceOfFunds(CorporateSourceOfFunds.UNKNOWN)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_UnknownCompanyPosition_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setCompanyPosition(CompanyPosition.UNKNOWN)
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_NoCompanyPosition_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setCompanyPosition(null)
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_TermsNotAccepted_TermsNotAccepted() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setAcceptedTerms(false)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TERMS_NOT_ACCEPTED"));
    }

    @Test
    public void CreateCorporate_InvalidApiKey_Unauthorised() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        CorporatesService.createCorporate(createCorporateModel, "abc", Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateCorporate_NoApiKey_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        CorporatesService.createCorporate(createCorporateModel, "", Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_UnknownProfileId_ProfileNotFound() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setProfileId("123")
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void CreateCorporate_DifferentIdentityProfileId_ProfileNotFound() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setProfileId(consumerProfileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void CreateCorporate_DifferentInnovatorApiKey_Forbidden() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        InnovatorHelper.createCorporateProfileWithPassword(innovator.getRight(), innovator.getMiddle());
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");
        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void CreateCorporate_InvalidProfileId_ProfileNotFound(final String profileId) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setProfileId(profileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @NullSource
    public void CreateCorporate_NoProfileId_BadRequest(final String profileId) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setProfileId(profileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.profileId: must not be blank"));
    }

    @Test
    public void CreateCorporate_SameEmail_UserAlreadyCreated() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ABCD", ""})
    public void CreateCorporate_InvalidCurrency_BadRequest(final String currency) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(currency)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_UnknownCurrency_CurrencyUnsupported() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency("ABC")
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_UNSUPPORTED"));
    }

    @Test
    public void CreateCorporate_UnknownFeeGroup_FeeGroupInvalid() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setFeeGroup("ABC")
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FEE_GROUP_INVALID"));
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void CreateCorporate_EmailHavingApostropheOrSingleQuotes_Success(final String email) {


        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(email)
                                .build())
                        .build();

        final String corporateId = CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id.id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD)).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_OK);

        CorporatesHelper.verifyEmail(email, secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(email, new PasswordModel(TestHelper.DEFAULT_PASSWORD)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void CreateCorporate_InvalidEmailFormat_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void CreateCorporate_InvalidMobileNumber_BadRequest(final String mobileNumber) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+356", mobileNumber))
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+"})
    public void CreateCorporate_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_NoMobileNumberCountryCode_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("", "123456"))
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateCorporate_CountryCodeNotSupported_CountryUnsupported() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry(CountryCode.AF.name())
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
    }

    @Test
    public void CreateCorporate_MobileNotUnique_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final CreateCorporateModel mobileNumberNotUniqueModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(createCorporateModel.getRootUser().getMobile())
                                .build())
                        .build();

        assertSuccessfulResponse(CorporatesService.createCorporate(mobileNumberNotUniqueModel, secretKey, Optional.empty()), mobileNumberNotUniqueModel);
    }

    @Test
    public void CreateCorporate_InvalidDateOfBirthYear_BadRequest() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(2101, 1, 1)).build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.year: must be less than or equal to 2100"));

        final CreateCorporateModel createCorporateModel2 =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1899, 1, 1)).build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel2, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.year: must be greater than or equal to 1900"));
    }

    @Test
    public void CreateCorporate_InvalidDateOfBirthMonth_BadRequest() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 13, 1)).build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.month: must be less than or equal to 12"));

        final CreateCorporateModel createCorporateModel2 =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 0, 1)).build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel2, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.month: must be greater than or equal to 1"));
    }

    @Test
    public void CreateCorporate_InvalidDateOfBirthDay_BadRequest() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 1, 32)).build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.day: must be less than or equal to 31"));

        final CreateCorporateModel createCorporateModel2 =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 1, 0)).build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel2, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.day: must be greater than or equal to 1"));
    }

    @Test
    public void CreateCorporate_NotAllowedDomain_EmailDomainNotAllowed() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setCompany(CompanyModel
                                .defaultCompanyModel()
                                .build())
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(String.format("%s%s@gav0.com", System.currentTimeMillis(),
                                        RandomStringUtils.randomAlphabetic(5)))
                                .build())
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @Test
    public void CreateCorporate_SameEmailWithinVerificationExpiry_RootEmailNotUnique() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
    }

    /**
     * If first corporate does not validate its email within 60 min(it is set to 3 sec for test environments)
     * another corporate can be created with the same email
     */
    @Test
    public void CreateCorporate_SameEmailAfterEmailVerificationExpiry_Success() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * If first corporate validate its email within 60 min(it is set to 15 sec for test environments)
     * another corporate cannot be created with the same email even if 60 min passed
     */
    @Test
    public void CreateCorporate_SameEmailAfterEmailVerification_RootEmailNotUnique() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), secretKey);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitOne_BadRequest() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("1", String.format("82923%s", RandomStringUtils.randomNumeric(5))))
                        .build())
                .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitTwo_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+49", String.format("30%s", RandomStringUtils.randomNumeric(6))))
                        .build())
                .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+"})
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitFourToSix_Success(final String countryCode) {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel(String.format("%s1829", countryCode), String.format("23%s", RandomStringUtils.randomNumeric(5))))
                        .build())
                .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1-1829", "001-1829"})
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitMoreThanSix_BadRequest(final String countryCode) {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel(countryCode, String.format("23%s", RandomStringUtils.randomNumeric(5))))
                        .build())
                .build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.rootUser.mobile.countryCode: must match \"^\\+[0-9]+$\""))
                .body("_embedded.errors[1].message", equalTo("request.rootUser.mobile.countryCode: size must be between 1 and 6"));
    }

    // TODO Idempotency tests to be updated when this operation becomes idempotent

//    @Test
//    public void CreateCorporate_SameIdempotencyRefDifferentPayload_BadRequest() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final CreateCorporateModel firstCreateCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
//                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
//                                .setName(RandomStringUtils.randomAlphabetic(5))
//                                .build())
//                        .build();
//
//        CorporatesService.createCorporate(firstCreateCorporateModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        final CreateCorporateModel secondCreateCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
//                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
//                                .setName(RandomStringUtils.randomAlphabetic(5))
//                                .build())
//                        .build();
//
//        CorporatesService.createCorporate(secondCreateCorporateModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//    }
//
//    @Test
//    public void CreateCorporate_SameIdempotencyRefSamePayload_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        responses.add(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference)));
//        responses.add(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CORPORATE"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(corporateProfileId))
//                        .body("tag", equalTo(createCorporateModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CORPORATE"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
//                        .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
//                        .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.emailVerified", equalTo(false))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.dateOfBirth.year", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
//                                equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
//                                equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
//                                equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
//                        .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
//                        .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
//                        .body("company.businessAddress.addressLine1", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
//                        .body("company.businessAddress.addressLine2", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
//                        .body("company.businessAddress.city", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
//                        .body("company.businessAddress.postCode", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
//                        .body("company.businessAddress.state", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
//                        .body("company.businessAddress.country", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
//                        .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
//                        .body("industry", createCorporateModel.getIndustry() == null ? nullValue() :
//                                equalTo(createCorporateModel.getIndustry().toString()))
//                        .body("sourceOfFunds", createCorporateModel.getSourceOfFunds() == null ? nullValue() :
//                                equalTo(createCorporateModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFundsOther()))
//                        .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//    }
//
//    @Test
//    public void CreateCorporate_SameIdempotencyRefSamePayloadWithChange_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        responses.add(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference)));
//
//        CorporatesHelper.createCorporatePassword(responses.get(0).then().extract().jsonPath().getString("id.id"), secretKey);
//        CorporatesHelper.verifyEmail(responses.get(0).then().extract().jsonPath().getString("rootUser.email"), secretKey);
//        final String corporateToken = AuthenticationHelper.login(responses.get(0).then().extract().jsonPath().getString("rootUser.email"), secretKey);
//        final PatchCorporateModel updateCorporateModel = PatchCorporateModel.newBuilder().setName(RandomStringUtils.randomAlphabetic(5)).build();
//        CorporatesHelper.patchCorporate(updateCorporateModel, secretKey, corporateToken);
//
//        responses.add(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CORPORATE"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(corporateProfileId))
//                        .body("tag", equalTo(createCorporateModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CORPORATE"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
//                        .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.emailVerified", equalTo(false))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.dateOfBirth.year", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
//                                equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
//                                equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
//                                equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
//                        .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
//                        .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
//                        .body("company.businessAddress.addressLine1", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
//                        .body("company.businessAddress.addressLine2", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
//                        .body("company.businessAddress.city", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
//                        .body("company.businessAddress.postCode", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
//                        .body("company.businessAddress.state", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
//                        .body("company.businessAddress.country", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
//                                equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
//                        .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
//                        .body("industry", createCorporateModel.getIndustry() == null ? nullValue() :
//                                equalTo(createCorporateModel.getIndustry().toString()))
//                        .body("sourceOfFunds", createCorporateModel.getSourceOfFunds() == null ? nullValue() :
//                                equalTo(createCorporateModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFundsOther()))
//                        .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), createCorporateModel.getRootUser().getName());
//        assertEquals(responses.get(1).jsonPath().getString("rootUser.name"), updateCorporateModel.getName());
//    }
//
//    @Test
//    public void CreateCorporate_DifferentIdempotencyRefSamePayload_RootEmailNotUnique() {
//
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(RandomStringUtils.randomAlphanumeric(20)))
//                .then()
//                .statusCode(SC_OK);
//
//        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(RandomStringUtils.randomAlphanumeric(20)))
//                .then()
//                .statusCode(SC_CONFLICT)
//                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
//    }
//
//
//
//    @Test
//    public void CreateCorporate_DifferentIdempotencyRefDifferentPayload_Success() {
//
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(RandomStringUtils.randomAlphanumeric(20))), createCorporateModel);
//
//        final CreateCorporateModel createCorporateModel1 =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel1, secretKey, Optional.of(RandomStringUtils.randomAlphanumeric(20))), createCorporateModel1);
//    }
//
//    @Test
//    public void CreateCorporate_LongIdempotencyRef_RequestTooLong() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);
//
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_REQUEST_TOO_LONG);
//    }
//
//    @Test
//    public void CreateCorporate_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final CreateCorporateModel firstCreateCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
//                        .setIndustry(Industry.UNKNOWN)
//                        .build();
//
//        CorporatesService.createCorporate(firstCreateCorporateModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//
//        final CreateCorporateModel secondCreateCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        assertSuccessfulResponse(CorporatesService.createCorporate(secondCreateCorporateModel, secretKey, Optional.of(idempotencyReference)), secondCreateCorporateModel);
//    }
//
//    @Test
//    public void CreateCorporate_SameIdempotencyRefSamePayloadReferenceExpired_RootEmailNotUnique() throws InterruptedException {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
//
//        assertSuccessfulResponse(CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference)), createCorporateModel);
//
//        TimeUnit.SECONDS.sleep(18);
//
//        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_CONFLICT)
//                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
//    }

    private void assertSuccessfulResponse(final Response response,
                                          final CreateCorporateModel createCorporateModel) {

        assertSuccessfulResponse(response, createCorporateModel, corporateProfileId);
    }

    private void assertSuccessfulResponse(final Response response,
                                          final CreateCorporateModel createCorporateModel,
                                          final String corporateProfileId) {

        response
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("tag", equalTo(createCorporateModel.getTag()))
                .body("rootUser.id.type", equalTo("CORPORATE"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
                .body("rootUser.mobile.countryCode", equalTo(createCorporateModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createCorporateModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.companyPosition", equalTo(createCorporateModel.getRootUser().getCompanyPosition().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(false))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.dateOfBirth.year", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createCorporateModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createCorporateModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", createCorporateModel.getRootUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createCorporateModel.getRootUser().getDateOfBirth().getDay()))
                .body("company.name", equalTo(createCorporateModel.getCompany().getName()))
                .body("company.type", equalTo(createCorporateModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createCorporateModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createCorporateModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createCorporateModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createCorporateModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", createCorporateModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createCorporateModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createCorporateModel.getCompany().getRegistrationCountry()))
                .body("industry", createCorporateModel.getIndustry() == null ? nullValue() :
                        equalTo(createCorporateModel.getIndustry().toString()))
                .body("sourceOfFunds", createCorporateModel.getSourceOfFunds() == null ? nullValue() :
                        equalTo(createCorporateModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createCorporateModel.getSourceOfFundsOther()))
                .body("acceptedTerms", equalTo(createCorporateModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createCorporateModel.getIpAddress()))
                .body("baseCurrency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
