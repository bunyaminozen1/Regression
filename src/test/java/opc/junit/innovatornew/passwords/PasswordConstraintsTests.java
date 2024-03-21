package opc.junit.innovatornew.passwords;

import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.enums.opc.PasswordConstraint;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.innovatornew.InnovatorService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static opc.enums.opc.PasswordConstraint.PASSCODE;
import static opc.enums.opc.PasswordConstraint.PASSWORD;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PasswordConstraintsTests extends BasePasswordsSetup {
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
