package opc.junit.multi.consumers;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.ConsumerSourceOfFunds;
import opc.enums.opc.CountryCode;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.Occupation;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.AddressModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.mailhog.MailhogService;
import opc.services.multi.ConsumersService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PatchConsumersTests extends BaseConsumersSetup {

    private String authenticationToken;
    private CreateConsumerModel createConsumerModel;
    private String consumerId;
    private String consumerEmail;

    final private static int mobileChangeLimit = 3;
    final private static int emailChangeBlockingLimit = 10;
    final private static int mobileChangeBlockingLimit = 10;

    private final static String VERIFICATION_CODE = "123456";

    @BeforeEach
    public void Setup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerEmail = createConsumerModel.getRootUser().getEmail();
        authenticationToken = authenticatedConsumer.getRight();
    }

    @Test
    public void PatchConsumers_EmailChecksNewEmailNotValidated_Success() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        //if new email is not validated - old email remains active and returns in response
        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tag", equalTo(createConsumerModel.getTag()))
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
                .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
                .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createConsumerModel.getSourceOfFunds() == ConsumerSourceOfFunds.OTHER ? createConsumerModel.getSourceOfFundsOther() : null))
                .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()));
    }

    @ParameterizedTest
    @EnumSource(value = Occupation.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void PatchConsumers_OccupationChecks_Success(final Occupation occupation) {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .setOccupation(occupation)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tag", equalTo(createConsumerModel.getTag()))
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
                .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
                .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createConsumerModel.getSourceOfFunds() == ConsumerSourceOfFunds.OTHER ? createConsumerModel.getSourceOfFundsOther() : null))
                .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()));
    }

    @Test
    public void PatchConsumers_PatchAllEntries_Success() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.DefaultPatchConsumerModel()
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tag", equalTo(patchConsumerModel.getTag()))
                .body("rootUser.name", equalTo(patchConsumerModel.getName()))
                .body("rootUser.surname", equalTo(patchConsumerModel.getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(patchConsumerModel.getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(patchConsumerModel.getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(patchConsumerModel.getDateOfBirth().getDay()))
                .body("rootUser.address.addressLine1", equalTo(patchConsumerModel.getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(patchConsumerModel.getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(patchConsumerModel.getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(patchConsumerModel.getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(patchConsumerModel.getAddress().getState()))
                .body("rootUser.address.country", equalTo(patchConsumerModel.getAddress().getCountry()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createConsumerModel.getSourceOfFunds() == ConsumerSourceOfFunds.OTHER ? createConsumerModel.getSourceOfFundsOther() : null))
                .body("baseCurrency", equalTo(patchConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("rootUser.placeOfBirth", equalTo(patchConsumerModel.getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(patchConsumerModel.getNationality()));
    }

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, mode = EnumSource.Mode.EXCLUDE, names = {"AF"})
    public void PatchConsumers_CountryCodeChecks_Success(final CountryCode countryCode) {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(countryCode)
                                .build())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tag", equalTo(createConsumerModel.getTag()))
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
                .body("rootUser.address.addressLine1", equalTo(patchConsumerModel.getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(patchConsumerModel.getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(patchConsumerModel.getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(patchConsumerModel.getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(patchConsumerModel.getAddress().getState()))
                .body("rootUser.address.country", equalTo(patchConsumerModel.getAddress().getCountry()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createConsumerModel.getSourceOfFunds() == ConsumerSourceOfFunds.OTHER ? createConsumerModel.getSourceOfFundsOther() : null))
                .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()));
    }

    @Test
    public void PatchConsumers_SameEmail_Conflict() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(createConsumerModel.getRootUser().getEmail())
                        .build();

        ConsumersHelper.verifyEmail(patchConsumerModel.getEmail(), secretKey);

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CONSUMER.name()))
                .body("id.id", equalTo(consumerId))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.active", equalTo(true));
    }

    @Test
    public void PatchConsumers_DobNotRequired_Success() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.DefaultPatchConsumerModel()
                        .setDateOfBirth(null)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tag", equalTo(patchConsumerModel.getTag()))
                .body("rootUser.name", equalTo(patchConsumerModel.getName()))
                .body("rootUser.surname", equalTo(patchConsumerModel.getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
                .body("rootUser.address.addressLine1", equalTo(patchConsumerModel.getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(patchConsumerModel.getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(patchConsumerModel.getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(patchConsumerModel.getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(patchConsumerModel.getAddress().getState()))
                .body("rootUser.address.country", equalTo(patchConsumerModel.getAddress().getCountry()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createConsumerModel.getSourceOfFunds() == ConsumerSourceOfFunds.OTHER ? createConsumerModel.getSourceOfFundsOther() : null))
                .body("baseCurrency", equalTo(patchConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("rootUser.placeOfBirth", equalTo(patchConsumerModel.getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(patchConsumerModel.getNationality()));
    }

    @Test
    public void PatchConsumers_RequiredOnlyInAddressField_Success() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.DefaultPatchConsumerModel()
                        .setAddress(AddressModel.builder()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tag", equalTo(patchConsumerModel.getTag()))
                .body("rootUser.name", equalTo(patchConsumerModel.getName()))
                .body("rootUser.surname", equalTo(patchConsumerModel.getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(patchConsumerModel.getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(patchConsumerModel.getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(patchConsumerModel.getDateOfBirth().getDay()))
                .body("rootUser.address.addressLine1", nullValue())
                .body("rootUser.address.addressLine2", nullValue())
                .body("rootUser.address.city", nullValue())
                .body("rootUser.address.postCode", nullValue())
                .body("rootUser.address.state", nullValue())
                .body("rootUser.address.country", equalTo(patchConsumerModel.getAddress().getCountry()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().name()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther", equalTo(createConsumerModel.getSourceOfFunds() == ConsumerSourceOfFunds.OTHER ? createConsumerModel.getSourceOfFundsOther() : null))
                .body("baseCurrency", equalTo(patchConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("rootUser.placeOfBirth", equalTo(patchConsumerModel.getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(patchConsumerModel.getNationality()));
    }

    @Test
    public void PatchConsumers_MissingCountryInAddress_BadRequest() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.DefaultPatchConsumerModel()
                        .setAddress(AddressModel.builder().build())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.address.country: must not be blank"));
    }

    @Test
    public void PatchConsumers_OldEmailVerified_Success() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchConsumers_NewEmailVerified_Success() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.verifyEmail(new EmailVerificationModel(patchConsumerModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchConsumers_ChangeEmailTwiceWithinBlockingTime_Conflict() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_FREQUENT_EMAIL_CHANGES"));
    }

    @Test
    public void PatchConsumers_ChangeEmailTwiceAfterBlockingTime_Success() throws InterruptedException {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        //on QA blocking time should be 5s
        TimeUnit.SECONDS.sleep(emailChangeBlockingLimit);

        final PatchConsumerModel patchConsumerModelSecondUpdate =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModelSecondUpdate, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchConsumers_ChangeEmailTwiceAfterBlockingTimeFirstNewEmailVerified_Success() throws InterruptedException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .build())
                .build();
        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty());

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.verifyEmail(new EmailVerificationModel(patchConsumerModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        //on QA blocking time should be 5s
        TimeUnit.SECONDS.sleep(emailChangeBlockingLimit);

        final PatchConsumerModel patchConsumerModelSecondUpdate =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModelSecondUpdate, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(patchConsumerModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void PatchConsumers_ChangeEmailTwiceInBlockingTimeFirstNewEmailVerified_Conflict() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.verifyEmail(new EmailVerificationModel(patchConsumerModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final PatchConsumerModel patchConsumerModelSecondUpdate =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModelSecondUpdate, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_FREQUENT_EMAIL_CHANGES"));
    }

    @Test
    public void PatchConsumers_ChangeAnyFieldTwiceWithinBlockingTime_Success() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setName("Test")
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchConsumerModel.getName()));

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchConsumerModel.getName()));
    }

    @Test
    public void PatchConsumers_ChangeAnyFieldAfterEmailChangedWithinBlockingTime_Success() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        final PatchConsumerModel patchConsumerModelSecond =
                PatchConsumerModel.newBuilder()
                        .setName("Test")
                        .build();

        ConsumersService.patchConsumer(patchConsumerModelSecond, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchConsumerModelSecond.getName()));
    }

    @Test
    public void PatchConsumers_UpdateEmailKycVerified_ConsumerVerified() {

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSUMER_VERIFIED"));
    }

    @Test
    public void PatchConsumers_UpdateNameKycVerified_ConsumerVerified() {

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setName(RandomStringUtils.randomAlphabetic(5))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSUMER_VERIFIED"));
    }

    @Test
    public void PatchConsumers_UpdateSurnameKycVerified_ConsumerVerified() {

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setSurname(RandomStringUtils.randomAlphabetic(5))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSUMER_VERIFIED"));
    }

    @Test
    public void PatchConsumers_UpdateMobileCountryCodeKycVerified_ConsumerVerified() {

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.randomUK())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSUMER_VERIFIED"));
    }

    @Test
    public void PatchConsumers_UpdateMobileNumberKycVerified_ConsumerVerified() {

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSUMER_VERIFIED"));
    }

    @Test
    public void PatchConsumers_UpdateDateOfBirthKycVerified_ConsumerVerified() {

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setDateOfBirth(new DateOfBirthModel(1977, 2, 2))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSUMER_VERIFIED"));
    }

    @Test
    public void PatchConsumers_EmailAlreadyExists_EmailNotUnique() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(createConsumerModel.getRootUser().getEmail())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void PatchConsumers_UnknownOccupation_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setOccupation(Occupation.UNKNOWN)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_MobileWithoutCountryCode_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel(null, RandomStringUtils.randomNumeric(8)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_CountryCodeWithoutMobile_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", null))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_InvalidEmail_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(RandomStringUtils.randomAlphanumeric(6))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_InvalidEmailFormat_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("PatchConsumers_UnknownCurrency_CurrencyUnsupported - DEV-2807 opened to return 409")
    public void PatchConsumers_UnknownCurrency_CurrencyUnsupported() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setBaseCurrency("ABC")
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR);// To be updated to 409 - CURRENCY_UNSUPPORTED following fix
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void PatchConsumers_InvalidMobileNumber_BadRequest(final String mobileNumber) {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", mobileNumber))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_NoMobileNumberCountryCode_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel("", "123456"))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_AddressWithoutRootUserCountry_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(null)
                                .build()    )
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.address.country: must not be blank"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+"})
    public void PatchConsumers_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_PatchInactiveUser_Unauthorized() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(false, "TEMPORARY"),
                consumerId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchConsumers_InvalidApiKey_Unauthorised() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, "abc", authenticationToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchConsumers_NoApiKey_BadRequest() {
        System.out.println(PhoneNumberUtil.getInstance().getExampleNumber("GB").getNationalNumber());
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, "", authenticationToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchConsumers_RootUserLoggedOut_Unauthorised() {

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchConsumers_PatchUsingNonRootUserAuthentication_Forbidden() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, authenticationToken);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchConsumers_BackofficeImpersonator_Forbidden() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchConsumers_CountryCodeNotSupported_CountryUnsupported() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.AF)
                                .build())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "", "A"})
    public void PatchConsumers_InvalidNationality_BadRequest(final String nationality) {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setNationality(nationality)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumers_MobileNotUnique_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(createConsumerModel.getRootUser().getMobile())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_InvalidDateOfBirthDay_BadRequest() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setDateOfBirth(new DateOfBirthModel(2101, 12, 3))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchConsumer_SourceOfFundsOtherMissing_Success() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setSourceOfFunds(ConsumerSourceOfFunds.OTHER)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds().toString()));
    }

    @Test
    public void PatchConsumer_PatchMobileWithUniqueNumber_Success() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_PatchMobileWithNonUniqueNumberSameIdentityType_Success() {

        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(MobileNumberModel.random())
                        .build())
                .build();

        ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(consumerModel.getRootUser().getMobile())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_PatchMobileWithNonUniqueNumberCrossIdentityType_Success() {

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(MobileNumberModel.random())
                        .build())
                .build();

        CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(corporateModel.getRootUser().getMobile())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_MobileChangeLimitUpdateMobileFieldNull_Success() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchConsumerModel patchConsumerModel =
                            PatchConsumerModel.newBuilder()
                                    .setMobile(null)
                                    .build();

                    ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
                });

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(null)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));

        final PatchConsumerModel patchConsumerModel2 =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel2, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel2.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel2.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_MobileChangeLimitByUpdateNewNumber_LimitExceeded() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchConsumerModel patchConsumerModel =
                            PatchConsumerModel.newBuilder()
                                    .setMobile(MobileNumberModel.random())
                                    .build();

                    ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
                });

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    @Test
    public void PatchConsumer_UpdateMobileMultipleTimesWithItsOwnNumber_Success() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchConsumerModel patchConsumerModel =
                            PatchConsumerModel.newBuilder()
                                    .setMobile(createConsumerModel.getRootUser().getMobile())
                                    .build();

                    ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
                });

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_UpdateAnotherFieldAfterMobileChangeLimitExceeded_Success() {
        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final PatchConsumerModel patchConsumerModel =
                            PatchConsumerModel.newBuilder()
                                    .setMobile(MobileNumberModel.random())
                                    .build();

                    ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
                });

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));

        final PatchConsumerModel patchConsumerModel2 =
                PatchConsumerModel.newBuilder()
                        .setName(RandomStringUtils.randomAlphabetic(5))
                        .setSurname(RandomStringUtils.randomAlphabetic(5))
                        .setMobile(null)
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel2, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(patchConsumerModel2.getName()))
                .body("rootUser.surname", equalTo(patchConsumerModel2.getSurname()));
    }

    @Test
    public void PatchConsumer_MobileChangeLimitByUpdateNonUniqueNumber_LimitExceeded() {

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                            .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                    .setMobile(MobileNumberModel.random())
                                    .build())
                            .build();

                    ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey);

                    final PatchConsumerModel patchConsumerModel =
                            PatchConsumerModel.newBuilder()
                                    .setMobile(consumerModel.getRootUser().getMobile())
                                    .build();

                    ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                            .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
                });


        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    /**
     * Cases for the ticket <a href="https://weavr-payments.atlassian.net/browse/DEV-4974">...</a>
     * 1. Create 2 users with the same mobile number
     * 2. Create third user without mobile number or different mobile number
     * 3. Patch third user with the same mobile number and country code as previous ones.
     */
    @Test
    public void PatchConsumer_SeveralUsersSameNumberPatchMobileNotUniqueNumber_Success() {
        final String mobileNumber = MobileNumberModel.random().getNumber();

        final CreateConsumerModel firstConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+356", mobileNumber))
                        .build())
                .build();
        ConsumersHelper.createAuthenticatedConsumer(firstConsumerModel, secretKey);

        final CreateConsumerModel secondConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+356", mobileNumber))
                        .build())
                .build();
        ConsumersHelper.createAuthenticatedConsumer(secondConsumerModel, secretKey);

        final CreateConsumerModel thirdConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setMobile(MobileNumberModel.random())
                        .build())
                .build();
        final Pair<String, String> thirdConsumer = ConsumersHelper.createAuthenticatedConsumer(thirdConsumerModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", mobileNumber))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, thirdConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumer_CheckMobileChangeSmsOldNumberVerified_Success() {
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                authenticationToken);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                authenticationToken);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals(String.format(MailHogSms.SCA_CHANGE_SMS.getSmsText(),
                        StringUtils.right(createConsumerModel.getRootUser().getMobile().getNumber(), 4),
                        StringUtils.right(patchConsumerModel.getMobile().getNumber(), 4)),
                sms.getBody());

    }

    @Test
    public void PatchConsumer_CheckMobileChangeSmsOldNumberNotVerified_NoSms() {

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey,
                authenticationToken);

        MailhogService.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber())
                .then()
                .statusCode(200)
                .body("items[0]", nullValue());
    }

    @Test
    public void PatchConsumers_NewEmailVerifiedMobileNumberOtpNotEnrolledSecurityRule24H_Success() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        final PatchConsumerModel patchConsumerEmailModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.verifyEmail(new EmailVerificationModel(patchConsumerEmailModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        //No blocking time to check the new functionality, user can update the mobile number once otp was not enrolled

        final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(new MobileNumberModel("+356", String.format("21%s", RandomStringUtils.randomNumeric(6))))
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_NewEmailNotVerifiedAndMobileNumberOtpNotEnrolledSecurityRule24H_Success() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        final PatchConsumerModel patchConsumerEmailModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));


        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        //No blocking time to check the response, should be 200 once new email was not verified and otp not enrolled

        final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_NewEmailVerifiedAndMobileNumberOtpEnrolledSecurityRule24H_Conflict() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        final PatchConsumerModel patchConsumerEmailModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.verifyEmail(new EmailVerificationModel(patchConsumerEmailModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time less than mobileChangeBlockingLimit, once new email was verified and otp enrolled user has to get error 409

        final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_CHANGE_NOT_ALLOWED"));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_NewEmailVerifiedAndMobileNumberOtpEnrolledSecurityRule24H_Success() throws InterruptedException {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        final PatchConsumerModel patchConsumerEmailModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerEmailModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(consumerEmail))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        ConsumersService.verifyEmail(new EmailVerificationModel(patchConsumerEmailModel.getEmail(), TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time is mobileChangeBlockingLimit, once new email was verified and otp enrolled user can update mobile number after blocking time
        TimeUnit.SECONDS.sleep(mobileChangeBlockingLimit);

          final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(patchConsumerEmailModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_ResumeLostPasswordMobileNumberOtpNotEnrolledSecurityRule24H_Success() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        //No blocking time to check the new functionality, user can update the mobile number once otp was not enrolled

        final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_ResumeLostPasswordAndMobileNumberOtpEnrolledSecurityRule24H_Conflict() {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time less than mobileChangeBlockingLimit, once new password was resumed and otp enrolled user has to get error 409
        final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_CHANGE_NOT_ALLOWED"));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_ResumeLostPasswordAndMobileNumberOtpEnrolledSecurityRule24H_Success() throws InterruptedException {

        ConsumersHelper.verifyEmail(consumerEmail, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, authenticationToken);

        //Blocking time is mobileChangeBlockingLimit, once new email was verified and otp enrolled user can update mobile number after blocking time
        TimeUnit.SECONDS.sleep(mobileChangeBlockingLimit);

        final PatchConsumerModel patchConsumerMobileModel =
                PatchConsumerModel.newBuilder()
                        .setMobile(MobileNumberModel.random())
                        .build();

        ConsumersService.patchConsumer(patchConsumerMobileModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobile.countryCode", equalTo(patchConsumerMobileModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchConsumerMobileModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_ChangeEmailWithNotAllowedDomain_Conflict() {
        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@gav0.com", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void PatchConsumers_ChangeEmailHavingApostropheOrSingleQuotes_Success(final String email) {

        final PatchConsumerModel patchConsumerModel = PatchConsumerModel.newBuilder().setEmail(email).build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchConsumers_PatchMobileNumberCountryCodeCharacterLimitOne_Success() {

        final PatchConsumerModel patchCorporateModel = PatchConsumerModel.newBuilder()
                .setMobile(new MobileNumberModel("+1", String.format("82923%s", RandomStringUtils.randomNumeric(5))))
                .build();

        ConsumersService.patchConsumer(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_PatchMobileNumberCountryCodeCharacterLimitTwo_Success() {

        final PatchConsumerModel patchCorporateModel = PatchConsumerModel.newBuilder()
                .setMobile(new MobileNumberModel("+49",
                        String.format("30%s", RandomStringUtils.randomNumeric(6))))
                .build();

        ConsumersService.patchConsumer(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @Test
    public void PatchConsumers_PatchMobileNumberCountryCodeCharacterLimitFourToSix_Success() {

        final PatchConsumerModel patchCorporateModel = PatchConsumerModel.newBuilder()
                .setMobile(new MobileNumberModel("+1829",
                        String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        ConsumersService.patchConsumer(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1-1829", "001-1829"})
    public void PatchConsumers_PatchMobileNumberCountryCodeCharacterLimitMoreThanSix_BadRequest(final String countryCode) {

        final PatchConsumerModel patchCorporateModel = PatchConsumerModel.newBuilder()
                .setMobile(new MobileNumberModel(countryCode, String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        ConsumersService.patchConsumer(patchCorporateModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.mobile.countryCode: must match \"^\\+[0-9]+$\""))
                .body("_embedded.errors[1].message", equalTo("request.mobile.countryCode: size must be between 1 and 6"));
    }

    // TODO Idempotency tests to be updated when this operation becomes idempotent

//    @Test
//    public void PatchConsumers_SameIdempotencyRefDifferentPayload_BadRequest() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final PatchConsumerModel firstPatchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        ConsumersService.patchConsumer(firstPatchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        final PatchConsumerModel secondPatchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        ConsumersService.patchConsumer(secondPatchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//        // TODO returns 200
//    }
//
//    @Test
//    public void PatchConsumers_SameIdempotencyRefSamePayload_Success() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchConsumerModel patchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//        responses.add(ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("tag", equalTo(createConsumerModel.getTag()))
//                        .body("rootUser.name", equalTo(patchConsumerModel.getName()))
//                        .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(consumerEmail)));
//
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }
//
//    @Test
//    public void PatchConsumers_DifferentIdempotencyRefSamePayload_Conflict() {
//        final String firstIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final String secondIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchConsumerModel patchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(firstIdempotencyReference)));
//        responses.add(ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(secondIdempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("tag", equalTo(createConsumerModel.getTag()))
//                        .body("rootUser.name", equalTo(patchConsumerModel.getName()))
//                        .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(consumerEmail)));
//
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }
//
//    @Test
//    public void PatchConsumers_LongIdempotencyRef_RequestTooLong() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);
//
//        final PatchConsumerModel patchConsumerModel =
//                PatchConsumerModel.DefaultPatchConsumerModel()
//                        .build();
//
//        ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_REQUEST_TOO_LONG);
//    }
//
//    @Test
//    public void PatchConsumers_SameIdempotencyRefDifferentPayloadInitialCallFailed_BadRequest() {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final PatchConsumerModel firstPatchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setAddress(AddressModel.builder().build())
//                        .build();
//
//        ConsumersService.patchConsumer(firstPatchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//
//        final PatchConsumerModel secondPatchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setAddress(AddressModel.RandomAddressModel())
//                        .build();
//
//        ConsumersService.patchConsumer(secondPatchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//        // TODO returns 200
//    }
//
//    @Test
//    public void PatchConsumers_SameIdempotencyRefSamePayloadReferenceExpired_Success() throws InterruptedException {
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final PatchConsumerModel patchConsumerModel =
//                PatchConsumerModel.newBuilder()
//                        .setName(RandomStringUtils.randomAlphabetic(5))
//                        .build();
//
//        responses.add(ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//        TimeUnit.SECONDS.sleep(18);
//        responses.add(ConsumersService.patchConsumer(patchConsumerModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("tag", equalTo(createConsumerModel.getTag()))
//                        .body("rootUser.name", equalTo(patchConsumerModel.getName()))
//                        .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
//                        .body("rootUser.email", equalTo(consumerEmail)));
//
//        assertEquals(responses.get(0).jsonPath().getString("rootUser.name"), responses.get(1).jsonPath().getString("rootUser.name"));
//    }

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
