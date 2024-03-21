package opc.junit.admin.passwords;

import static opc.enums.opc.PasswordConstraint.PASSCODE;
import static opc.enums.opc.PasswordConstraint.PASSWORD;
import static opc.enums.opc.PasswordConstraint.UNKNOWN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.enums.opc.PasswordConstraint;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.PasswordValidationModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.multi.users.ConsumerUserInviteModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

public class PasswordConstraintsTests extends BasePasswordsSetup {


  @Test
  public void UpdateProfile_CorporateProfileMultipleApplications_Success() {
    updatePasswordConstraint(PASSCODE, programmeId);
    updatePasswordConstraint(PASSWORD, programmeId2);

    final String corporateId1 =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(),
            secretKey);

    final String corporateId2 =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId2).build(),
            secretKey2);

    createPassword(RandomStringUtils.randomAlphabetic(3), corporateId1, secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", CoreMatchers.equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), corporateId1, secretKey)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId1))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());

    createPassword(RandomStringUtils.randomAlphabetic(7), corporateId2, secretKey2)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", CoreMatchers.equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), corporateId2, secretKey2)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId2))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_CorporateProfileToPasscode_Success() {

    updatePasswordConstraint(PASSCODE);

    final String corporateId1 =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(),
            secretKey);

    createPassword(RandomStringUtils.randomAlphabetic(3), corporateId1)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), corporateId1)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId1))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_CorporateProfileToPassword_Success() {

    updatePasswordConstraint(PASSCODE);
    updatePasswordConstraint(PASSWORD);

    final String corporateId1 =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(),
            secretKey);

    createPassword(RandomStringUtils.randomAlphabetic(7), corporateId1)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), corporateId1)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId1))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ConsumerMultipleApplications_Success() {
    updatePasswordConstraint(PASSWORD, programmeId);
    updatePasswordConstraint(PASSCODE, programmeId2);

    final String consumerId1 =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);

    final String consumerId2 =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId2).build(), secretKey2);

    createPassword(RandomStringUtils.randomAlphabetic(7), consumerId1, secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), consumerId1, secretKey)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerId1))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());

    createPassword(RandomStringUtils.randomAlphabetic(3), consumerId2, secretKey2)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), consumerId2, secretKey2)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerId2))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ConsumerProfileToPasscode_Success() {

    updatePasswordConstraint(PASSCODE);

    final String consumerId1 =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);

    createPassword(RandomStringUtils.randomAlphabetic(3), consumerId1)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), consumerId1)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerId1))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ConsumerProfileToPassword_Success() {

    updatePasswordConstraint(PASSCODE);
    updatePasswordConstraint(PASSWORD);

    final String consumerId1 =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);

    createPassword(RandomStringUtils.randomAlphabetic(7), consumerId1)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), consumerId1)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerId1))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ValidatePassword_Success() {
    updatePasswordConstraint(PASSCODE);
    updatePasswordConstraint(PASSWORD);

    PasswordValidationModel passwordValidationModel =
        new PasswordValidationModel(new PasswordModel(RandomStringUtils.randomAlphabetic(7)));

    PasswordsService.validatePassword(passwordValidationModel, secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    passwordValidationModel = new PasswordValidationModel(
        new PasswordModel(RandomStringUtils.randomAlphabetic(8)));
    PasswordsService.validatePassword(passwordValidationModel, secretKey)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void UpdateProfile_ValidatePasscode_Success() {
    updatePasswordConstraint(PASSCODE);

    PasswordValidationModel passwordValidationModel =
        new PasswordValidationModel(new PasswordModel(RandomStringUtils.randomAlphabetic(3)));

    PasswordsService.validatePassword(passwordValidationModel, secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    passwordValidationModel = new PasswordValidationModel(
        new PasswordModel(RandomStringUtils.randomAlphabetic(4)));
    PasswordsService.validatePassword(passwordValidationModel, secretKey)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void UpdateProfile_RootFromPasscodeToPassword_ExistingUserNotAffectedSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(4);
    updatePasswordConstraint(PASSCODE);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

    createPassword(password, corporateId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSWORD);

    assertSuccessfulLogin(createCorporateModel.getRootUser().getEmail(), password);
  }

  @Test
  public void UpdateProfile_RootFromPasswordToPasscode_ExistingUserNotAffectedSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(8);
    updatePasswordConstraint(PASSWORD);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

    createPassword(password, corporateId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSCODE);

    assertSuccessfulLogin(createCorporateModel.getRootUser().getEmail(), password);
  }

  @Test
  public void UpdateProfile_RootFromPasscodeToPassword_NewUserUsingPasswordSuccess() {

    updatePasswordConstraint(PASSCODE);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final Pair<String, String> corporate = CorporatesHelper
        .createAuthenticatedCorporate(createCorporateModel, secretKey,
            RandomStringUtils.randomAlphanumeric(4));

    updatePasswordConstraint(PASSWORD);

    final UsersModel model = UsersModel.DefaultUsersModel().build();
    final String userId = UsersHelper.createUser(model, secretKey, corporate.getRight());

    UsersService.inviteUser(secretKey, userId, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(7))), secretKey, userId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(8))), secretKey, userId)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasswordToPasscode_NewUserUsingPasscodeSuccess() {

    updatePasswordConstraint(PASSWORD);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final Pair<String, String> corporate = CorporatesHelper
        .createAuthenticatedCorporate(createCorporateModel, secretKey,
            RandomStringUtils.randomAlphanumeric(8));

    updatePasswordConstraint(PASSCODE);

    final UsersModel model = UsersModel.DefaultUsersModel().build();
    final String userId = UsersHelper.createUser(model, secretKey, corporate.getRight());

    UsersService.inviteUser(secretKey, userId, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(3))), secretKey, userId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(4))), secretKey, userId)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasscodeToPassword_UpdatePasswordSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(4);
    updatePasswordConstraint(PASSCODE);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

    createPassword(password, corporateId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSWORD);

    final String token = assertSuccessfulLogin(createCorporateModel.getRootUser().getEmail(),
        password);

    UpdatePasswordModel updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(7)));

    PasswordsService.updatePassword(updatePasswordModel, secretKey, token)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", CoreMatchers.equalTo("PASSWORD_TOO_SHORT"));

    updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(8)));

    PasswordsService.updatePassword(updatePasswordModel, secretKey, token)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasswordToPasscode_UpdatePasswordSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(8);
    updatePasswordConstraint(PASSWORD);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

    createPassword(password, corporateId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSCODE);

    final String token = assertSuccessfulLogin(createCorporateModel.getRootUser().getEmail(),
        password);

    UpdatePasswordModel updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(3)));

    PasswordsService.updatePassword(updatePasswordModel, secretKey, token)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", CoreMatchers.equalTo("PASSWORD_TOO_SHORT"));

    updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(4)));

    PasswordsService.updatePassword(updatePasswordModel, secretKey, token)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasscodeToPassword_ResumeLostPasswordSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(4);
    updatePasswordConstraint(PASSCODE);

    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

    final String consumerId =
        ConsumersHelper.createConsumer(createConsumerModel, secretKey);

    createPassword(password, consumerId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSWORD);

    PasswordsService.startLostPassword(
            new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKey)
        .then()
        .statusCode(SC_NO_CONTENT);

    LostPasswordResumeModel lostPasswordResumeModel =
        LostPasswordResumeModel
            .newBuilder()
            .setEmail(createConsumerModel.getRootUser().getEmail())
            .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphabetic(7)))
            .setNonce(TestHelper.VERIFICATION_CODE)
            .build();

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    lostPasswordResumeModel =
        LostPasswordResumeModel
            .newBuilder()
            .setEmail(createConsumerModel.getRootUser().getEmail())
            .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphabetic(8)))
            .setNonce(TestHelper.VERIFICATION_CODE)
            .build();

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasswordToPasscode_ResumeLostPasswordSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(8);
    updatePasswordConstraint(PASSWORD);

    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

    final String consumerId =
        ConsumersHelper.createConsumer(createConsumerModel, secretKey);

    createPassword(password, consumerId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSCODE);

    PasswordsService.startLostPassword(
            new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKey)
        .then()
        .statusCode(SC_NO_CONTENT);

    LostPasswordResumeModel lostPasswordResumeModel =
        LostPasswordResumeModel
            .newBuilder()
            .setEmail(createConsumerModel.getRootUser().getEmail())
            .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphabetic(3)))
            .setNonce(TestHelper.VERIFICATION_CODE)
            .build();

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    lostPasswordResumeModel =
        LostPasswordResumeModel
            .newBuilder()
            .setEmail(createConsumerModel.getRootUser().getEmail())
            .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphabetic(4)))
            .setNonce(TestHelper.VERIFICATION_CODE)
            .build();

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_NoConstraint_BadRequest() {
    AdminService.updateProfileConstraint(
            new PasswordConstraintsModel(UNKNOWN), programmeId, adminToken)
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", equalTo( "JSON request violated validation rules"));
  }

  @Test
  public void UpdateProfile_UnknownProgrammeId_ProgrammeNotFound() {
    AdminService.updateProfileConstraint(new PasswordConstraintsModel(PASSCODE),
            RandomStringUtils.randomNumeric(18), adminToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void UpdateProfile_InvalidToken_Unauthorised() {

    AdminService.updateProfileConstraint(new PasswordConstraintsModel(PASSCODE), programmeId,
            RandomStringUtils.randomAlphanumeric(5))
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }


  @Test
  public void UpdateProfile_GetConstraint_Success() {
    updatePasswordConstraint(PASSCODE);

    AdminService.getProfileConstraint(programmeId, adminToken)
        .then()
        .statusCode(SC_OK)
        .body("constraint", equalTo(PASSCODE.name()));

    updatePasswordConstraint(PASSWORD);

    AdminService.getProfileConstraint(programmeId, adminToken)
        .then()
        .statusCode(SC_OK)
        .body("constraint", equalTo(PASSWORD.name()));
  }

  @Test
  public void GetConstraint_UnknownProgrammeId_ProgrammeNotFound() {
    AdminService.getProfileConstraint(RandomStringUtils.randomNumeric(18), adminToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
  }


  @Test
  public void UpdateProfile_PasscodeConstraint_BadRequest() {
    AdminService.updateProfileConstraint(new PasswordConstraintsModel(PASSCODE), programmeId,
            adminToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void UpdateProfile_PasswordConstraint_BadRequest() {
    AdminService.updateProfileConstraint(new PasswordConstraintsModel(PASSWORD), programmeId,
            adminToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void GetConstraint_InvalidToken_Unauthorised() {

    AdminService.getProfileConstraint(programmeId, RandomStringUtils.randomAlphanumeric(5))
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  private String assertSuccessfulLogin(final String email, final String password) {
    return AuthenticationService.loginWithPassword(
            new LoginModel(email, new PasswordModel(password)), secretKey)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue())
        .extract()
        .jsonPath()
        .get("token");
  }

  private void updatePasswordConstraint(final PasswordConstraint passwordConstraint) {
    updatePasswordConstraint(passwordConstraint, programmeId);
  }

  private void updatePasswordConstraint(final PasswordConstraint passwordConstraint,
      final String programmeId) {
    AdminService.updateProfileConstraint(new PasswordConstraintsModel(passwordConstraint),
            programmeId, adminToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  private Response createPassword(final String password, final String identityId) {
    return createPassword(password, identityId, secretKey);
  }

  private Response createPassword(final String password, final String identityId,
      final String secretKey) {
    final CreatePasswordModel createPasswordModel = CreatePasswordModel
        .newBuilder()
        .setPassword(new PasswordModel(password)).build();

    return PasswordsService.createPassword(createPasswordModel, identityId, secretKey);
  }
}
