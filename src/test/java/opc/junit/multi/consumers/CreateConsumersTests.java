package opc.junit.multi.consumers;

import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import opc.enums.opc.ConsumerSourceOfFunds;
import opc.enums.opc.CountryCode;
import opc.enums.opc.Occupation;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.AddressModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ConsumersService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
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

public class CreateConsumersTests extends BaseConsumersSetup {

    final private static int VERIFICATION_TIME_LIMIT = 90;

    @Test
    public void CreateConsumer_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @ParameterizedTest
    @EnumSource(value = Occupation.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void CreateConsumer_OccupationChecks_Success(final Occupation occupation) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setOccupation(occupation).build())
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @Test
    public void CreateConsumer_TagNotRequired_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setTag(null)
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, mode = EnumSource.Mode.EXCLUDE, names = {"AF", "GB", "GG", "IM", "JE"})
    public void CreateConsumer_CountryChecks_Success(final CountryCode countryCode) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(countryCode)
                                        .build()).build())
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, mode = EnumSource.Mode.INCLUDE, names = {"GB", "GG", "IM", "JE"})
    public void CreateConsumer_UkJurisdictionCountryChecks_Success(final CountryCode countryCode) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationOneUk.getConsumersProfileId())
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(countryCode)
                                        .build()).build())
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, applicationOneUk.getSecretKey(), Optional.empty()), createConsumerModel, applicationOneUk.getConsumersProfileId());
    }

    @Test
    public void CreateConsumer_UnknownOccupation_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setOccupation(Occupation.UNKNOWN).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    /**
     * With the new change, providing the country of the root user's address became mandatory.
     */

    @Test
    public void CreateConsumer_MissingRootUserCountryInAddress_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(null)
                                        .build())
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.address.country: must not be blank"));
    }

    @Test
    public void CreateConsumer_MissingDateOfBirth_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(null)
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsumer_RequiredOnly_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setTag(null)
                        .setFeeGroup(null)
                        .setBaseCurrency(null)
                        .setSourceOfFunds(null)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setPlaceOfBirth(null)
                                .setNationality(null)
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateConsumer_RequiredOnlyInAddress_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.builder()
                                        .setAddressLine1(null)
                                        .setAddressLine2(null)
                                        .setCity(null)
                                        .setCountry(CountryCode.MT)
                                        .setPostCode(null)
                                        .setState(null)
                                        .build())
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.address.addressLine1", nullValue())
                .body("rootUser.address.addressLine2", nullValue())
                .body("rootUser.address.city", nullValue())
                .body("rootUser.address.postCode", nullValue())
                .body("rootUser.address.state", nullValue())
                .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()));
    }

    @Test
    public void CreateConsumer_UnknownSourceOfFunds_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setSourceOfFunds(ConsumerSourceOfFunds.UNKNOWN)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsumer_SourceOfFundsOtherRequiredIfOtherSourceOfFundsChosen_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setSourceOfFunds(ConsumerSourceOfFunds.SALE_OF_REAL_ESTATE)
                        .setSourceOfFundsOther(null)
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @Test
    public void CreateConsumer_MissingSourceOfFundOther_Success() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setSourceOfFunds(ConsumerSourceOfFunds.OTHER)
                        .setSourceOfFundsOther(null)
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @Test
    public void CreateConsumer_NoOccupation_Success() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.
                NoOccupationCreateConsumerModel(consumerProfileId).build();

        assertSuccessfulResponse(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), createConsumerModel);
    }

    @Test
    public void CreateConsumer_TermsNotAccepted_TermsNotAccepted() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setAcceptedTerms(false)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TERMS_NOT_ACCEPTED"));
    }

    @Test
    public void CreateConsumer_InvalidApiKey_Unauthorised() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        ConsumersService.createConsumer(createConsumerModel, "abc", Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateConsumer_NoApiKey_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        ConsumersService.createConsumer(createConsumerModel, "", Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsumer_UnknownProfileId_ProfileNotFound() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setProfileId("123")
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void CreateConsumer_DifferentIdentityProfileId_ProfileNotFound() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setProfileId(corporateProfileId)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void CreateConsumer_DifferentInnovatorApiKey_Forbidden() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        ConsumersService.createConsumer(createConsumerModel, nonFpsTenant.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void CreateConsumer_InvalidProfileId_ProfileNotFound(final String profileId) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setProfileId(profileId)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @NullSource
    public void CreateConsumer_NoProfileId_BadRequest(final String profileId) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setProfileId(profileId)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.profileId: must not be blank"));
    }

    @Test
    public void CreateConsumer_SameEmail_UserAlreadyCreated() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
    }

    /**
     * If first consumer does not validate its email within 60 min another consumer can be created with the same email
     * config for test environment:
     * consumer:
     * clean:
     * unverified-email:
     * initialDelay: "5s"
     * fixedDelay: "15s"
     * validate-email-lifetime-second: 15
     */
    @Test
    public void CreateConsumer_SameEmailAfterEmailVerificationExpiry_Success() throws InterruptedException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        //Waiting 90 sec because of delay time in the config (config: 60s-120s-15s)
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * If first consumer validate its email within 60 min
     * another consumer cannot be created with the same email even if 60 min passed
     * config for test environment:
     * consumer:
     * clean:
     * unverified-email:
     * initialDelay: "5s"
     * fixedDelay: "15s"
     * validate-email-lifetime-second: 15
     */
    @Test
    public void CreateConsumer_SameEmailAfterEmailVerification_UserAlreadyCreated() throws InterruptedException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);

        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), secretKey);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void CreateConsumer_InvalidEmailFormat_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void CreateConsumer_EmailHavingApostropheOrSingleQuotes_Success(final String email) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(email).build())
                        .build();

        final String consumerId = ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id.id");

        ConsumersHelper.verifyEmail(email, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD)).build();

        PasswordsService.createPassword(createPasswordModel, consumerId, secretKey)
                .then()
                .statusCode(SC_OK);

        AuthenticationService.loginWithPassword(new LoginModel(email, new PasswordModel(TestHelper.DEFAULT_PASSWORD)), secretKey)
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void CreateConsumer_InvalidMobileNumber_BadRequest(final String mobileNumber) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+356", mobileNumber))
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "+"})
    public void CreateConsumer_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsumer_NoMobileNumberCountryCode_BadRequest() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("", "123456"))
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "", "A"})
    public void CreateConsumer_InvalidNationality_BadRequest(final String nationality) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setNationality(nationality)
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsumer_CountryCodeNotSupported_CountryUnsupported() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.AF)
                                        .build()).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
    }

    @Test
    public void CreateConsumer_MobileNotUnique_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final CreateConsumerModel mobileNumberNotUniqueModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setMobile(createConsumerModel.getRootUser().getMobile())
                                .build())
                        .build();

        assertSuccessfulResponse(ConsumersService.createConsumer(mobileNumberNotUniqueModel, secretKey, Optional.empty()), mobileNumberNotUniqueModel);
    }

    @Test
    public void CreateConsumer_InvalidYearForBirthday_BadRequest() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(2101, 1, 1)).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.year: must be less than or equal to 2100"));

        final CreateConsumerModel createConsumerModel2 =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1899, 1, 1)).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel2, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.year: must be greater than or equal to 1900"));
    }

    @Test
    public void CreateConsumer_InvalidMonthForBirthday_BadRequest() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 13, 1)).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.month: must be less than or equal to 12"));

        final CreateConsumerModel createConsumerModel2 =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 0, 1)).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel2, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.month: must be greater than or equal to 1"));
    }

    @Test
    public void CreateConsumer_InvalidDayForBirthday_BadRequest() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 1, 32)).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.day: must be less than or equal to 31"));

        final CreateConsumerModel createConsumerModel2 =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setDateOfBirth(new DateOfBirthModel(1980, 1, 0)).build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel2, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.rootUser.dateOfBirth.day: must be greater than or equal to 1"));
    }

    @Test
    public void CreateConsumer_NoIpAddress_BadRequest() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setIpAddress(null)
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.ipAddress: must not be blank"));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 46})
    public void CreateConsumer_InvalidIpAddress_BadRequest(final int characterLength) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setIpAddress(RandomStringUtils.randomAlphanumeric(characterLength))
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.ipAddress: size must be between 5 and 45"));
    }

    @Test
    public void CreateConsumer_NotAllowedDomain_EmailDomainNotAllowed() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(String.format("%s%s@gav0.com", System.currentTimeMillis(),
                                        RandomStringUtils.randomAlphabetic(5)))
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @Test
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitOne_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+1", String.format("82923%s", RandomStringUtils.randomNumeric(5))))
                        .build())
                .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
    }

    @Test
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitTwo_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+49", String.format("30%s", RandomStringUtils.randomNumeric(6))))
                        .build())
                .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
    }

    @Test
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitFour_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+1829", String.format("23%s", RandomStringUtils.randomNumeric(5))))
                        .build())
                .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1-1829", "001-1829"})
    public void CreateConsumer_PatchMobileNumberCountryCodeCharacterLimitMoreThanSix_BadRequest(final String countryCode) {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel(countryCode, String.format("23%s", RandomStringUtils.randomNumeric(5))))
                        .build())
                .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.rootUser.mobile.countryCode: must match \"^\\+[0-9]+$\""))
                .body("_embedded.errors[1].message", equalTo("request.rootUser.mobile.countryCode: size must be between 1 and 6"));
    }

    // TODO Idempotency tests to be updated when this operation becomes idempotent

//    @Test
//    public void CreateConsumer_SameIdempotencyRefDifferentPayload_BadRequest() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final CreateConsumerModel firstCreateConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        ConsumersService.createConsumer(firstCreateConsumerModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        final CreateConsumerModel secondCreateConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        ConsumersService.createConsumer(secondCreateConsumerModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//    }
//
//    @Test
//    public void CreateConsumer_SameIdempotencyRefSamePayload_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final CreateConsumerModel createConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        responses.add(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference)));
//        responses.add(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CONSUMER"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(consumerProfileId))
//                        .body("tag", equalTo(createConsumerModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CONSUMER"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
//                        .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
//                        .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation() == null ? null : createConsumerModel.getRootUser().getOccupation().toString()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.emailVerified", equalTo(false))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
//                        .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
//                        .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
//                        .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
//                        .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
//                        .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
//                        .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
//                        .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()))
//                        .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds() == null ? null : createConsumerModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther",
//                                equalTo(createConsumerModel.getSourceOfFunds().equals(ConsumerSourceOfFunds.OTHER) ?
//                                        createConsumerModel.getSourceOfFundsOther() : null))
//                        .body("acceptedTerms", equalTo(createConsumerModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createConsumerModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//    }
//
//    @Test
//    public void CreateConsumer_SameIdempotencyRefSamePayloadWithChange_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final CreateConsumerModel createConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        responses.add(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference)));
//
//        ConsumersHelper.createConsumerPassword(responses.get(0).then().extract().jsonPath().getString("id.id"), secretKey);
//        ConsumersHelper.verifyEmail(responses.get(0).then().extract().jsonPath().getString("rootUser.email"), secretKey);
//        final String token = AuthenticationHelper.login(responses.get(0).then().extract().jsonPath().getString("rootUser.email"), secretKey);
//        final PatchConsumerModel patchConsumerModel = PatchConsumerModel.newBuilder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        ConsumersHelper.patchConsumer(patchConsumerModel, secretKey, token);
//
//        responses.add(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CONSUMER"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(consumerProfileId))
//                        .body("tag", equalTo(createConsumerModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CONSUMER"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
//                        .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation() == null ? null : createConsumerModel.getRootUser().getOccupation().toString()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
//                        .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
//                        .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
//                        .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
//                        .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
//                        .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
//                        .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
//                        .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()))
//                        .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds() == null ? null : createConsumerModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther",
//                                equalTo(createConsumerModel.getSourceOfFunds().equals(ConsumerSourceOfFunds.OTHER) ?
//                                        createConsumerModel.getSourceOfFundsOther() : null))
//                        .body("acceptedTerms", equalTo(createConsumerModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createConsumerModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), createConsumerModel.getRootUser().getName());
//        assertEquals(responses.get(1).jsonPath().getString("rootUser.name"), patchConsumerModel.getName());
//    }
//
//    @Test
//    public void CreateConsumer_DifferentIdempotencyRefSamePayload_Conflict() {
//        final String firstIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final String secondIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final CreateConsumerModel createConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(firstIdempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(secondIdempotencyReference))
//                .then()
//                .statusCode(SC_CONFLICT)
//                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
//    }
//
//    @Test
//    public void CreateConsumer_LongIdempotencyRef_RequestTooLong() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);
//
//        final CreateConsumerModel createConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_REQUEST_TOO_LONG);
//    }
//
//    @Test
//    public void CreateConsumer_SameIdempotencyRefDifferentPayloadInitialCallFailed_BadRequest() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final CreateConsumerModel firstCreateConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
//                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
//                                .setOccupation(Occupation.UNKNOWN).build())
//                        .build();
//
//        ConsumersService.createConsumer(firstCreateConsumerModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//
//        final CreateConsumerModel secondCreateConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        ConsumersService.createConsumer(secondCreateConsumerModel, secretKey, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//        // TODO returns 200
//    }
//
//    @Test
//    public void CreateConsumer_SameIdempotencyRefSamePayloadReferenceExpired_Success() throws InterruptedException {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final CreateConsumerModel createConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
//
//        responses.add(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference)));
//        TimeUnit.SECONDS.sleep(18);
//        responses.add(ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id.type", equalTo("CONSUMER"))
//                        .body("id.id", notNullValue())
//                        .body("profileId", equalTo(consumerProfileId))
//                        .body("tag", equalTo(createConsumerModel.getTag()))
//                        .body("rootUser.id.type", equalTo("CONSUMER"))
//                        .body("rootUser.id.id", notNullValue())
//                        .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
//                        .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
//                        .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
//                        .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
//                        .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
//                        .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
//                        .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
//                        .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation() == null ? null : createConsumerModel.getRootUser().getOccupation().toString()))
//                        .body("rootUser.active", equalTo(true))
//                        .body("rootUser.emailVerified", equalTo(false))
//                        .body("rootUser.mobileNumberVerified", equalTo(false))
//                        .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
//                        .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
//                        .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
//                        .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
//                        .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
//                        .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
//                        .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
//                        .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()))
//                        .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds() == null ? null : createConsumerModel.getSourceOfFunds().toString()))
//                        .body("sourceOfFundsOther",
//                                equalTo(createConsumerModel.getSourceOfFunds().equals(ConsumerSourceOfFunds.OTHER) ?
//                                        createConsumerModel.getSourceOfFundsOther() : null))
//                        .body("acceptedTerms", equalTo(createConsumerModel.getAcceptedTerms()))
//                        .body("ipAddress", equalTo(createConsumerModel.getIpAddress()))
//                        .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
//                        .body("feeGroup", equalTo("DEFAULT"))
//                        .body("creationTimestamp", notNullValue()));
//
//        assertEquals(responses.get(0).jsonPath().getString("id.id"), responses.get(1).jsonPath().getString("id.id"));
//    }

    private void assertSuccessfulResponse(final Response response, final CreateConsumerModel createConsumerModel) {
        assertSuccessfulResponse(response, createConsumerModel, consumerProfileId);
    }

    private void assertSuccessfulResponse(final Response response,
                                          final CreateConsumerModel createConsumerModel,
                                          final String consumerProfileId) {

        response
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CONSUMER"))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(consumerProfileId))
                .body("tag", equalTo(createConsumerModel.getTag()))
                .body("rootUser.id.type", equalTo("CONSUMER"))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation() == null ? null : createConsumerModel.getRootUser().getOccupation().toString()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(false))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
                .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
                .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds() == null ? null : createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther",
                        equalTo(createConsumerModel.getSourceOfFunds().equals(ConsumerSourceOfFunds.OTHER) ?
                                createConsumerModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createConsumerModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createConsumerModel.getIpAddress()))
                .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
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
