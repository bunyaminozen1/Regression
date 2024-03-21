package opc.junit.multi.passwords;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.KycLevel;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ExpiredPasswordTests extends BasePasswordSetup {

  public static final String DEFAULT_PASSWORD = "Pass1234";

  @Test
  public void CorporateLoginWithExpiredPasswordRootUserForbidden() {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        corporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    final String userEmail = createCorporateModel.getRootUser().getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, corporate.getLeft());

    AuthenticationService.loginWithPassword(
            new LoginModel(userEmail, new PasswordModel(DEFAULT_PASSWORD)), secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("token", notNullValue());
  }

  @Test
  public void ConsumerLoginWithExpiredPasswordRootUserForbidden() {
    final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(
        consumerProfileId).build();
    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, secretKey);
    final String userEmail = createConsumerModel.getRootUser().getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, consumer.getLeft());

    AuthenticationService.loginWithPassword(
            new LoginModel(userEmail, new PasswordModel(DEFAULT_PASSWORD)), secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("token", notNullValue());
  }

  @Test
  public void CorporateLoginWithExpiredPasswordAuthorisedUserForbidden() {
    Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        corporateProfileId, secretKey);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(usersModel,
        secretKey, corporate.getRight());

    final String userEmail = usersModel.getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, authenticatedUser.getLeft());

    AuthenticationService.loginWithPassword(
            new LoginModel(userEmail, new PasswordModel(DEFAULT_PASSWORD)), secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("token", notNullValue());
  }

  @Test
  public void ConsumerLoginWithExpiredPasswordAuthorisedUserForbidden() {
    Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId,
        secretKey);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(usersModel,
        secretKey, consumer.getRight());

    final String userEmail = usersModel.getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, authenticatedUser.getLeft());

    AuthenticationService.loginWithPassword(
            new LoginModel(userEmail, new PasswordModel(DEFAULT_PASSWORD)), secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .body("token", notNullValue());
  }

  @Test
  public void CorporateCallMultiEndpointsWithExpiredPasswordForbidden() {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        corporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    final String userEmail = createCorporateModel.getRootUser().getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, corporate.getLeft());
    final String userToken = getExpiredPasswordToken(userEmail);

    CorporatesService.startCorporateKyb(secretKey, userToken)
        .then()
        .statusCode(SC_FORBIDDEN);

    CorporatesService.getCorporates(secretKey, userToken)
        .then()
        .statusCode(SC_FORBIDDEN);

    AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, userToken)
        .then()
        .statusCode(SC_FORBIDDEN);
  }

  @Test
  public void ConsumerCallMultiEndpointsWithExpiredPasswordForbidden() {
    final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(
        consumerProfileId).build();
    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, secretKey);
    final String userEmail = createConsumerModel.getRootUser().getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, consumer.getLeft());
    final String userToken = getExpiredPasswordToken(userEmail);

    final StartKycModel startKycModel = StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2);

    ConsumersService.startConsumerKyc(startKycModel, secretKey, userToken)
        .then()
        .statusCode(SC_FORBIDDEN);

    ConsumersService.getConsumers(secretKey, userToken)
        .then()
        .statusCode(SC_FORBIDDEN);

    AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, userToken)
        .then()
        .statusCode(SC_FORBIDDEN);
  }

  @Test
  public void CreateNewPasswordWithExpiredPasswordToken() {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        corporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    final String userEmail = createCorporateModel.getRootUser().getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, corporate.getLeft());
    final String userToken = getExpiredPasswordToken(userEmail);

    final String newPassword = DEFAULT_PASSWORD;

    final UpdatePasswordModel updatePasswordModel = new UpdatePasswordModel(
        new PasswordModel(DEFAULT_PASSWORD), new PasswordModel(newPassword));

    PasswordsService.updatePassword(updatePasswordModel, secretKey, userToken)
        .then()
        .statusCode(SC_OK)
        .body("passwordInfo.expiryDate", equalTo(0))
        .body("passwordInfo.identityId.id", equalTo(corporate.getLeft()))
        .body("passwordInfo.identityId.type", equalTo("CORPORATE"))
        .body("token", notNullValue());
  }

  @Test
  public void CallMultiEndpointsWithNewPassword() {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        corporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    final String userEmail = createCorporateModel.getRootUser().getEmail();

    SimulatorHelper.simulateSecretExpiry(secretKey, corporate.getLeft());
    final String userToken = getExpiredPasswordToken(userEmail);

    final String newPassword = DEFAULT_PASSWORD;

    final UpdatePasswordModel updatePasswordModel = new UpdatePasswordModel(
        new PasswordModel(DEFAULT_PASSWORD), new PasswordModel(newPassword));

    final String newToken = PasswordsService.updatePassword(updatePasswordModel, secretKey,
        userToken).jsonPath().getString("token");

    CorporatesService.startCorporateKyb(secretKey, newToken)
        .then()
        .statusCode(SC_OK);

    CorporatesService.getCorporates(secretKey, newToken)
        .then()
        .statusCode(SC_OK);
  }

  public String getExpiredPasswordToken(final String userEmail) {
    return AuthenticationService.loginWithPassword(
            new LoginModel(userEmail, new PasswordModel(DEFAULT_PASSWORD)), secretKey)
        .then()
        .statusCode(SC_CONFLICT)
        .extract()
        .jsonPath()
        .getString("token");
  }
}
