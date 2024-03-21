package opc.junit.innovator.passwords;

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
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static opc.enums.opc.PasswordConstraint.PASSCODE;
import static opc.enums.opc.PasswordConstraint.PASSWORD;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PasswordConstraintsTests extends BasePasswordsSetup{
  @Test
  public void GetProfileConstraints_Success() {
    updatePasswordConstraint(PASSCODE, programmeIdAppOne);

    InnovatorService.getProfileConstraint(programmeIdAppOne, innovatorToken)
            .then()
            .statusCode(SC_OK)
            .body("constraint", equalTo(PASSCODE.name()))
            .body("length", notNullValue())
            .body("complexity", notNullValue());
  }

  @Test
  public void UpdateProfile_CorporateProfileMultipleApplications_Success() {
    updatePasswordConstraint(PASSCODE, programmeIdAppOne);
    updatePasswordConstraint(PASSWORD, programmeIdAppTwo);

    final String corporateIdOne =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build(),
                secretKeyAppOne);

    final String corporateIdTwo =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppTwo).build(),
                secretKeyAppTwo);

    createPassword(RandomStringUtils.randomAlphabetic(3), corporateIdOne, secretKeyAppOne)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), corporateIdOne, secretKeyAppOne)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateIdOne))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());

    createPassword(RandomStringUtils.randomAlphabetic(7), corporateIdTwo, secretKeyAppTwo)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), corporateIdTwo, secretKeyAppTwo)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateIdTwo))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_CorporateProfileToPasscode_Success() {

    updatePasswordConstraint(PASSCODE);

    final String corporateId =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build(),
                secretKeyAppOne);

    createPassword(RandomStringUtils.randomAlphabetic(3), corporateId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), corporateId)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_CorporateProfileToPassword_Success() {

    updatePasswordConstraint(PASSWORD);

    final String corporateId =
        CorporatesHelper.createCorporate(
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build(),
                secretKeyAppOne);

    createPassword(RandomStringUtils.randomAlphabetic(7), corporateId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), corporateId)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
        .body("passwordInfo.identityId.id", equalTo(corporateId))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ConsumerMultipleApplications_Success() {
    updatePasswordConstraint(PASSWORD, programmeIdAppOne);
    updatePasswordConstraint(PASSCODE, programmeIdAppTwo);

    final String consumerIdOne =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppOne).build(), secretKeyAppOne);

    final String consumerIdTwo =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppTwo).build(), secretKeyAppTwo);

    createPassword(RandomStringUtils.randomAlphabetic(7), consumerIdOne, secretKeyAppOne)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), consumerIdOne, secretKeyAppOne)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerIdOne))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());

    createPassword(RandomStringUtils.randomAlphabetic(3), consumerIdTwo, secretKeyAppTwo)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), consumerIdTwo, secretKeyAppTwo)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerIdTwo))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ConsumerProfileToPasscode_Success() {

    updatePasswordConstraint(PASSCODE);

    final String consumerId =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppOne).build(), secretKeyAppOne);

    createPassword(RandomStringUtils.randomAlphabetic(3), consumerId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(4), consumerId)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerId))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ConsumerProfileToPassword_Success() {

    updatePasswordConstraint(PASSWORD);

    final String consumerId =
        ConsumersHelper.createConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppOne).build(), secretKeyAppOne);

    createPassword(RandomStringUtils.randomAlphabetic(7), consumerId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    createPassword(RandomStringUtils.randomAlphabetic(8), consumerId)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
        .body("passwordInfo.identityId.id", equalTo(consumerId))
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_ValidatePassword_Success() {
    updatePasswordConstraint(PASSCODE);
    updatePasswordConstraint(PASSWORD);

    PasswordValidationModel passwordValidationModel =
        new PasswordValidationModel(new PasswordModel(RandomStringUtils.randomAlphabetic(7)));

    PasswordsService.validatePassword(passwordValidationModel, secretKeyAppOne)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    passwordValidationModel = new PasswordValidationModel(
        new PasswordModel(RandomStringUtils.randomAlphabetic(8)));
    PasswordsService.validatePassword(passwordValidationModel, secretKeyAppOne)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void UpdateProfile_ValidatePasscode_Success() {
    updatePasswordConstraint(PASSCODE);

    PasswordValidationModel passwordValidationModel =
        new PasswordValidationModel(new PasswordModel(RandomStringUtils.randomAlphabetic(3)));

    PasswordsService.validatePassword(passwordValidationModel, secretKeyAppOne)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    passwordValidationModel = new PasswordValidationModel(
        new PasswordModel(RandomStringUtils.randomAlphabetic(4)));
    PasswordsService.validatePassword(passwordValidationModel, secretKeyAppOne)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void UpdateProfile_RootFromPasscodeToPassword_ExistingUserNotAffectedSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(4);
    updatePasswordConstraint(PASSCODE);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKeyAppOne);

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
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKeyAppOne);

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
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build();

    final Pair<String, String> corporate = CorporatesHelper
        .createAuthenticatedCorporate(createCorporateModel, secretKeyAppOne,
            RandomStringUtils.randomAlphanumeric(4));

    updatePasswordConstraint(PASSWORD);

    final UsersModel model = UsersModel.DefaultUsersModel().build();
    final String userId = UsersHelper.createUser(model, secretKeyAppOne, corporate.getRight());

    UsersService.inviteUser(secretKeyAppOne, userId, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(7))), secretKeyAppOne, userId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(8))), secretKeyAppOne, userId)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasswordToPasscode_NewUserUsingPasscodeSuccess() {

    updatePasswordConstraint(PASSWORD);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build();

    final Pair<String, String> corporate = CorporatesHelper
        .createAuthenticatedCorporate(createCorporateModel, secretKeyAppOne,
            RandomStringUtils.randomAlphanumeric(8));

    updatePasswordConstraint(PASSCODE);

    final UsersModel model = UsersModel.DefaultUsersModel().build();
    final String userId = UsersHelper.createUser(model, secretKeyAppOne, corporate.getRight());

    UsersService.inviteUser(secretKeyAppOne, userId, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(3))), secretKeyAppOne, userId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE,
            new PasswordModel(RandomStringUtils.randomAlphabetic(4))), secretKeyAppOne, userId)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasscodeToPassword_UpdatePasswordSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(4);
    updatePasswordConstraint(PASSCODE);

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKeyAppOne);

    createPassword(password, corporateId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSWORD);

    final String token = assertSuccessfulLogin(createCorporateModel.getRootUser().getEmail(),
        password);

    UpdatePasswordModel updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(7)));

    PasswordsService.updatePassword(updatePasswordModel, secretKeyAppOne, token)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(8)));

    PasswordsService.updatePassword(updatePasswordModel, secretKeyAppOne, token)
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
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppOne).build();

    final String corporateId =
        CorporatesHelper.createCorporate(createCorporateModel, secretKeyAppOne);

    createPassword(password, corporateId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSCODE);

    final String token = assertSuccessfulLogin(createCorporateModel.getRootUser().getEmail(),
        password);

    UpdatePasswordModel updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(3)));

    PasswordsService.updatePassword(updatePasswordModel, secretKeyAppOne, token)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

    updatePasswordModel =
        new UpdatePasswordModel(new PasswordModel(password),
            new PasswordModel(RandomStringUtils.randomAlphanumeric(4)));

    PasswordsService.updatePassword(updatePasswordModel, secretKeyAppOne, token)
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
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppOne).build();

    final String consumerId =
        ConsumersHelper.createConsumer(createConsumerModel, secretKeyAppOne);

    createPassword(password, consumerId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSWORD);

    PasswordsService.startLostPassword(
            new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKeyAppOne)
        .then()
        .statusCode(SC_NO_CONTENT);

    LostPasswordResumeModel lostPasswordResumeModel =
        LostPasswordResumeModel
            .newBuilder()
            .setEmail(createConsumerModel.getRootUser().getEmail())
            .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphabetic(7)))
            .setNonce(TestHelper.VERIFICATION_CODE)
            .build();

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKeyAppOne)
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

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKeyAppOne)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_RootFromPasswordToPasscode_ResumeLostPasswordSuccess() {
    final String password = RandomStringUtils.randomAlphanumeric(8);
    updatePasswordConstraint(PASSWORD);

    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppOne).build();

    final String consumerId =
        ConsumersHelper.createConsumer(createConsumerModel, secretKeyAppOne);

    createPassword(password, consumerId)
        .then()
        .statusCode(SC_OK);

    updatePasswordConstraint(PASSCODE);

    PasswordsService.startLostPassword(
            new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKeyAppOne)
        .then()
        .statusCode(SC_NO_CONTENT);

    LostPasswordResumeModel lostPasswordResumeModel =
        LostPasswordResumeModel
            .newBuilder()
            .setEmail(createConsumerModel.getRootUser().getEmail())
            .setNewPassword(new PasswordModel(RandomStringUtils.randomAlphabetic(3)))
            .setNonce(TestHelper.VERIFICATION_CODE)
            .build();

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKeyAppOne)
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

    PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKeyAppOne)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue());
  }

  @Test
  public void UpdateProfile_NoConstraint_BadRequest() {
    InnovatorService.updateProfileConstraint(
            new PasswordConstraintsModel(PasswordConstraint.UNKNOWN), programmeIdAppOne, innovatorToken)
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void UpdateProfile_UnknownProgrammeId_ProgrammeNotFound() {
    InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PASSCODE),
            RandomStringUtils.randomNumeric(18), innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void UpdateProfile_InvalidToken_Unauthorised() {
    InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PASSCODE), programmeIdAppOne,
            RandomStringUtils.randomAlphanumeric(5))
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void UpdateProfile_GetConstraint_Success() {
    updatePasswordConstraint(PASSCODE);

    InnovatorService.getProfileConstraint(programmeIdAppOne, innovatorToken)
        .then()
        .statusCode(SC_OK)
        .body("constraint", equalTo(PASSCODE.name()));

    updatePasswordConstraint(PASSWORD);

    InnovatorService.getProfileConstraint(programmeIdAppOne, innovatorToken)
        .then()
        .statusCode(SC_OK)
        .body("constraint", equalTo(PASSWORD.name()));
  }

  @Test
  public void GetConstraint_UnknownProgrammeId_ProgrammeNotFound() {
    InnovatorService.getProfileConstraint(RandomStringUtils.randomNumeric(18), innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void UpdateProfile_PasscodeConstraint_BadRequest() {
    InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PASSCODE), programmeIdAppOne,
            innovatorToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void UpdateProfile_PasswordConstraint_BadRequest() {
    InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PASSWORD), programmeIdAppOne,
            innovatorToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void GetConstraint_InvalidToken_Unauthorised() {

    InnovatorService.getProfileConstraint(programmeIdAppOne, RandomStringUtils.randomAlphanumeric(5))
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  private String assertSuccessfulLogin(final String email, final String password) {
    return AuthenticationService.loginWithPassword(
            new LoginModel(email, new PasswordModel(password)), secretKeyAppOne)
        .then()
        .statusCode(SC_OK)
        .body("token", notNullValue())
        .extract()
        .jsonPath()
        .get("token");
  }

  private void updatePasswordConstraint(final PasswordConstraint passwordConstraint) {
    updatePasswordConstraint(passwordConstraint, programmeIdAppOne);
  }

  private void updatePasswordConstraint(final PasswordConstraint passwordConstraint,
      final String programmeId) {
    InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(passwordConstraint),
            programmeId, innovatorToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  private Response createPassword(final String password, final String identityId) {
    return createPassword(password, identityId, secretKeyAppOne);
  }

  private Response createPassword(final String password, final String identityId,
      final String secretKey) {
    final CreatePasswordModel createPasswordModel = CreatePasswordModel
        .newBuilder()
        .setPassword(new PasswordModel(password)).build();

    return PasswordsService.createPassword(createPasswordModel, identityId, secretKey);
  }
}
