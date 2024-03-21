package opc.junit.multi.passwords;

import io.vavr.Tuple3;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static opc.enums.mailhog.MailHogEmail.PASSWORD_CHANGED_ALERT;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ResumeLostPasswordTests extends BasePasswordSetup {

    @Test
    public void ResumeLostPassword_Corporate_Success() throws InterruptedException {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(corporateDetails.getRootUser().getEmail(),
                corporateDetails.getRootUser().getName(), corporateDetails.getRootUser().getSurname());
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void ResumeLostPassword_CorporateEmailHavingApostropheOrSingleQuote_Success(final String email) {

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(email).build())
                        .build();

        CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void ResumeLostPassword_ConsumerEmailHavingApostropheOrSingleQuote_Success(final String email) {

        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(email).build())
                        .build();

        ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void ResumeLostPassword_Consumer_Success() throws InterruptedException {

        final Tuple3<String, String, CreateConsumerModel> consumer = consumerSetup();
        final CreateConsumerModel consumerDetails = consumer._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(consumerDetails.getRootUser().getEmail(),
                consumerDetails.getRootUser().getName(), consumerDetails.getRootUser().getSurname());
    }

    @Test
    public void ResumeLostPassword_CorporateUser_Success() throws InterruptedException {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate._2());

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(usersModel.getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(usersModel.getEmail(), usersModel.getName(), usersModel.getSurname());
    }

    @Test
    public void ResumeLostPassword_ConsumerUser_Success() throws InterruptedException {

        final Tuple3<String, String, CreateConsumerModel> consumer = consumerSetup();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer._2());

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(usersModel.getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(usersModel.getEmail(), usersModel.getName(), usersModel.getSurname());
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void ResumeLostPassword_AuthorizedUserEmailHavingApostropheOrSingleQuote_Success(final String email) {

        final Tuple3<String, String, CreateConsumerModel> consumer = consumerSetup();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().setEmail(email).build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer._2());

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(usersModel.getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void ResumeLostPassword_CorporateInactive_Forbidden() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String corporateId = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey).getLeft();

        PasswordsService.startLostPassword(new LostPasswordStartModel(createCorporateModel.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"),
                corporateId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ResumeLostPassword_ConsumerInactive_Forbidden() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String corporateId = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey).getLeft();

        PasswordsService.startLostPassword(new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"),
                corporateId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createConsumerModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ResumeLostPassword_CorporateUserInactive_Forbidden() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final String userId = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight()).getLeft();

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        InnovatorHelper.deactivateCorporateUser(corporate.getLeft(), userId,
                InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(usersModel.getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ResumeLostPassword_ConsumerUserInactive_Forbidden() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final String userId = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight()).getLeft();

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        InnovatorHelper.deactivateConsumerUser(consumer.getLeft(), userId,
                InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(usersModel.getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ResumeLostPassword_InvalidApiKey_Unauthorised(){

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(String.format("%s@weavrapitest.io", RandomStringUtils.randomAlphanumeric(8)))
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ResumeLostPassword_NoApiKey_BadRequest(){

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(String.format("%s@weavrapitest.io", RandomStringUtils.randomAlphanumeric(8)))
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, "")
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResumeLostPassword_DifferentInnovatorApiKey_InvalidNonceOrEmail(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(String.format("%s@weavrapitest.io", RandomStringUtils.randomAlphanumeric(8)))
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then().statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_NONCE_OR_EMAIL"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc"})
    public void ResumeLostPassword_InvalidEmail_BadRequest(final String email) {

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(email)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResumeLostPassword_InvalidEmailFormat_BadRequest() {

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)))
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResumeLostPassword_UnknownEmail_InvalidNonceOrEmail() {

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(String.format("%s@weavrunknown.io", RandomStringUtils.randomAlphanumeric(8)))
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_NONCE_OR_EMAIL"));
    }

    @Test
    public void ResumeLostPassword_PasswordNotStarted_InvalidNonceOrEmail() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_NONCE_OR_EMAIL"));
    }

    @Test
    public void ResumeLostPassword_OtherRootUser_InvalidNonceOrEmail() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Tuple3<String, String, CreateCorporateModel> newCorporate = corporateSetup();

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(newCorporate._3().getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_NONCE_OR_EMAIL"));
    }

    @Test
    public void ResumeLostPassword_NonceMismatch_InvalidNonceOrEmail() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce("222222")
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_NONCE_OR_EMAIL"));
    }

    @Test
    public void ResumeLostPassword_ShortPassword_PasswordTooShort() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(7)))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));
    }

    @Test
    public void ResumeLostPassword_LongPassword_PasswordTooLong() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(51)))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void ResumeLostPassword_NoPassword_BadRequest(final String password) {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel(password))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResumeLostPassword_NoPasswordModel_BadRequest() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(null)
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResumeLostPassword_NoNonce_BadRequest() {

        final Tuple3<String, String, CreateCorporateModel> corporate = corporateSetup();
        final CreateCorporateModel corporateDetails = corporate._3();

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce("")
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResumeLostPassword_CorporatePasswordNotCreated_PasswordNotSet() {

        final CreateCorporateModel corporateDetails = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        CorporatesHelper.createCorporate(corporateDetails, secretKey);
        CorporatesHelper.verifyEmail(corporateDetails.getRootUser().getEmail(), secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateDetails.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_NOT_SET"));
    }

    @Test
    public void ResumeLostPassword_ConsumerPasswordNotCreated_PasswordNotSet() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        ConsumersHelper.createConsumer(createConsumerModel, secretKey);
        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), secretKey);

        PasswordsService.startLostPassword(new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createConsumerModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_NOT_SET"));
    }

    private static Tuple3<String, String, CreateConsumerModel> consumerSetup() {
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        final String consumerUserId = authenticatedConsumer.getLeft();
        final String consumerId = authenticatedConsumer.getLeft();
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerUserId);
        return new Tuple3<>(consumerId, consumerAuthenticationToken, consumerDetails);
    }

    private static Tuple3<String, String, CreateCorporateModel> corporateSetup() {
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateUserId);
        return new Tuple3<>(corporateId, corporateAuthenticationToken, corporateDetails);
    }

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }

    private void assertPasswordUpdatedAlertEmail(final String receiverEmail,
                                                 final String receiverName,
                                                 final String receiverSurname) throws InterruptedException {

        TimeUnit.SECONDS.sleep(5);
        final MailHogMessageResponse actualMail = MailhogHelper.getMailHogEmail(receiverEmail);

        assertEquals(PASSWORD_CHANGED_ALERT.getFrom(), actualMail.getFrom());
        assertEquals(receiverEmail, actualMail.getTo());
        assertEquals(PASSWORD_CHANGED_ALERT.getSubject(), actualMail.getSubject());
        assertEquals(String.format(PASSWORD_CHANGED_ALERT.getEmailText(), receiverName, receiverSurname), actualMail.getBody());
    }
}
