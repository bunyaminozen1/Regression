package fpi.paymentrun.identities.buyers;

import commons.models.CompanyPosition;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.paymentrun.models.AdminUserModel;
import fpi.paymentrun.models.CompanyModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.BuyersService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CountryCode;
import opc.models.shared.AddressModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static opc.enums.opc.CountryCode.GB;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class CreateBuyerTests extends BasePaymentRunSetup {

    /**
     * Test cases for POST /payment-run/1.0/buyers
     * Currently accepted country: GB, currency: GBP
     */

    @ParameterizedTest
    @EnumSource(value = CompanyType.class, mode = EnumSource.Mode.EXCLUDE, names = {"NON_PROFIT_ORGANISATION"})
    public void CreateBuyer_CompanyType_Success(final CompanyType companyType) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel
                                .gbCompanyModel()
                                .type(companyType.name())
                                .build())
                        .build();

        final ValidatableResponse response =
                BuyersService.createBuyer(createBuyerModel, secretKey)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(createBuyerModel, response);
    }

    @Test
    public void CreateBuyer_TagNotRequired_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .tag(null)
                        .build();

        final ValidatableResponse response =
                BuyersService.createBuyer(createBuyerModel, secretKey)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(createBuyerModel, response);
    }

    @Test
    public void CreateBuyer_RequiredOnlyInCompanyDetails_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.builder()
                                .type(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .name(RandomStringUtils.randomAlphabetic(10))
                                .countryOfRegistration(GB.name())
                                .build())
                        .build();

        final ValidatableResponse response =
                BuyersService.createBuyer(createBuyerModel, secretKey)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(createBuyerModel, response);
    }

    @Test
    public void CreateBuyer_RequiredOnlyInBusinessAddress_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .businessAddress(AddressModel.DefaultAddressModel()
                                        .setAddressLine2(null)
                                        .setState(null)
                                        .build())
                                .build())
                        .build();

        final ValidatableResponse response =
                BuyersService.createBuyer(createBuyerModel, secretKey)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(createBuyerModel, response);
    }

    @Test
    //  Currently only GB is supported
    public void CreateBuyer_AllowedCountryChecks_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.defaultCompanyModel()
                                .type(CompanyType.SOLE_TRADER.name())
                                .countryOfRegistration(GB.name())
                                .build())
                        .build();

        final ValidatableResponse response =
                BuyersService.createBuyer(createBuyerModel, secretKey)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(createBuyerModel, response);
    }

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, mode = EnumSource.Mode.EXCLUDE, names = {"GB"})
    public void CreateBuyer_NotAllowedCountryChecks_BadRequest(final CountryCode countryCode) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.defaultCompanyModel()
                                .type(CompanyType.SOLE_TRADER.name())
                                .countryOfRegistration(countryCode.name())
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params[0]", equalTo("company"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryOfRegistration"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateBuyer_RequiredOnlyAdminUserDetails_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.builder()
                                .name(RandomStringUtils.randomAlphabetic(6))
                                .surname(RandomStringUtils.randomAlphabetic(6))
                                .email(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                                        RandomStringUtils.randomAlphabetic(5)))
                                .mobile(MobileNumberModel.random())
                                .companyPosition(CompanyPosition.getRandomCompanyPosition())
                                .build())
                        .build();

        final ValidatableResponse response =
                BuyersService.createBuyer(createBuyerModel, secretKey)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(createBuyerModel, response);
    }

    @Test
    public void CreateBuyer_UnknownCompanyPosition_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .companyPosition(CompanyPosition.UNKNOWN)
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params[0]", equalTo("adminUser"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("companyPosition"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateBuyer_NoCompanyPosition_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .companyPosition(null)
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params[0]", equalTo("adminUser"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("companyPosition"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateBuyer_InvalidDateOfBirthMonth_BadRequest() {

        final DateOfBirthModel dateOfBirthModel = new DateOfBirthModel(2000, 13, 11);
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .dateOfBirth(dateOfBirthModel)
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("month"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void CreateBuyer_InvalidDateOfBirthDay_BadRequest() {

        final DateOfBirthModel dateOfBirthModel = new DateOfBirthModel(2000, 12, 32);
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .dateOfBirth(dateOfBirthModel)
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("day"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void CreateBuyer_InvalidDateOfBirthYearMoreThanExpected_BadRequest() {

        final DateOfBirthModel dateOfBirthModel = new DateOfBirthModel(2101, 12, 12);
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .dateOfBirth(dateOfBirthModel)
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("year"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void CreateBuyer_InvalidDateOfBirthYearLessThanExpected_BadRequest() {

        final DateOfBirthModel dateOfBirthModel = new DateOfBirthModel(1899, 12, 12);
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .dateOfBirth(dateOfBirthModel)
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("year"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_LEAST"));
    }

    @Test
    public void CreateBuyer_InvalidEmailFormat_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .email(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CreateBuyer_InvalidEmailDomain_EmailDomainNotAllowed() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .email(String.format("%s%s@gav0.com", System.currentTimeMillis(),
                                        RandomStringUtils.randomAlphabetic(5)))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @Test()
    public void CreateBuyer_InvalidMobileNumber_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", "abc"))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test()
    public void CreateBuyer_EmptyMobileNumber_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", ""))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test()
    public void CreateBuyer_NullMobileNumber_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", null))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+", "49"})
    public void CreateBuyer_InvalidMobileNumberCountryCode_BadRequest(final String mobileNumberCountryCode) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test()
    public void CreateBuyer_EmptyMobileNumberCountryCode_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("", "123456"))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test()
    public void CreateBuyer_NullMobileNumberCountryCode_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel(null, "123456"))
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("adminUser", "mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateBuyer_CountryCodeNotSupported_BadRequest() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .countryOfRegistration(CountryCode.AF.name())
                                .build())
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("company")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryOfRegistration"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateBuyer_AcceptedTermsFalse_TermsNotAccepted() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .acceptedTerms(false)
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TERMS_NOT_ACCEPTED"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void CreateBuyer_InvalidIpAddress_BadRequest(final String ipAddress) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .ipAddress(ipAddress)
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("ipAddress"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @ParameterizedTest()
    @NullSource
    public void CreateBuyer_NoIpAddress_BadRequest(final String ipAddress) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .ipAddress(ipAddress)
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("ipAddress"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));

    }

    @ParameterizedTest()
    @ValueSource(strings = {"ABCD", ""})
    public void CreateBuyer_InvalidCurrency_BadRequest(final String currency) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .baseCurrency(currency)
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("baseCurrency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateBuyer_UnknownCurrency_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .baseCurrency("ABC")
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("baseCurrency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ABCD", ""})
    public void CreateBuyer_InvalidSupportedCurrencies_BadRequest(final String currency) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .supportedCurrencies(List.of(currency))
                        .build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("supportedCurrencies", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("supportedCurrencies"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateBuyer_InvalidApiKey_Unauthorised() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .build();

        BuyersService.createBuyer(createBuyerModel, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateBuyer_NullApiKey_Unauthorised() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .build();

        BuyersService.createBuyer(createBuyerModel, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateBuyer_NoApiKey_BadRequest() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .build();

        BuyersService.createBuyerNoApiKey(createBuyerModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateBuyer_AlreadyCreated_AdminEmailNotUnique() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_OK);

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ADMIN_EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void CreateBuyer_MobileNotUnique_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final MobileNumberModel mobile = createBuyerModel.getAdminUser().getMobile();

        BuyersService.createBuyer(createBuyerModel, secretKey)
                .then()
                .statusCode(SC_OK);

        final CreateBuyerModel createBuyerModelSameMobile =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .adminUser(AdminUserModel.defaultAdminUserModel().mobile(mobile).build())
                        .build();

        BuyersService.createBuyer(createBuyerModelSameMobile, secretKey)
                .then()
                .statusCode(SC_OK);
    }

    private void assertSuccessfulResponse(final CreateBuyerModel createBuyerModel,
                                          final ValidatableResponse response) {
        response.body("id", notNullValue())
                .body("tag", equalTo(createBuyerModel.getTag()))
                .body("adminUser.id", notNullValue())
                .body("adminUser.name", equalTo(createBuyerModel.getAdminUser().getName()))
                .body("adminUser.surname", equalTo(createBuyerModel.getAdminUser().getSurname()))
                .body("adminUser.roles[0]", equalTo("ADMIN"))
                .body("adminUser.email", equalTo(createBuyerModel.getAdminUser().getEmail()))
                .body("adminUser.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(createBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.companyPosition", equalTo(createBuyerModel.getAdminUser().getCompanyPosition().name()))
                .body("adminUser.active", equalTo(true))
                .body("adminUser.emailVerified", equalTo(false))
                .body("adminUser.mobileNumberVerified", equalTo(false))
                .body("adminUser.dateOfBirth.year", createBuyerModel.getAdminUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getYear()))
                .body("adminUser.dateOfBirth.month", createBuyerModel.getAdminUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getMonth()))
                .body("adminUser.dateOfBirth.day", createBuyerModel.getAdminUser().getDateOfBirth() == null ? nullValue() :
                        equalTo(createBuyerModel.getAdminUser().getDateOfBirth().getDay()))
                .body("company.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("company.type", equalTo(createBuyerModel.getCompany().getType()))
                .body("company.registrationNumber", equalTo(createBuyerModel.getCompany().getRegistrationNumber()))
                .body("company.businessAddress.addressLine1", createBuyerModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()))
                .body("company.businessAddress.addressLine2", createBuyerModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()))
                .body("company.businessAddress.city", createBuyerModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createBuyerModel.getCompany().getBusinessAddress().getCity()))
                .body("company.businessAddress.postCode", createBuyerModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("company.businessAddress.state", createBuyerModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createBuyerModel.getCompany().getBusinessAddress().getState()))
                .body("company.businessAddress.country", createBuyerModel.getCompany().getBusinessAddress() == null ? nullValue() :
                        equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("company.countryOfRegistration", equalTo(createBuyerModel.getCompany().getCountryOfRegistration()))
                .body("company.incorporatedOn.year", nullValue())
                .body("company.incorporatedOn.month", nullValue())
                .body("company.incorporatedOn.day", nullValue())
                .body("acceptedTerms", equalTo(true))
                .body("ipAddress", equalTo(createBuyerModel.getIpAddress()))
                .body("baseCurrency", equalTo(createBuyerModel.getBaseCurrency()))
                .body("creationTimestamp", notNullValue())
                .body("supportedCurrencies[0]", equalTo(createBuyerModel.getSupportedCurrencies().get(0)));
    }
}
