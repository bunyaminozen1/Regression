package opc.junit.innovator.authenticatorfactors;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.PasswordConstraint;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.innovator.UpdateOkayModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

public class PushBiometricExceptionsTests extends BaseAuthenticationFactorsSetup{
  private static  String programmeId;
  private static String innovatorToken;


  @BeforeAll
  public static void EnrolUser_SetPasswordConstraintToPasscode() {
    programmeId = applicationOne.getProgrammeId();
    innovatorToken = InnovatorHelper.loginInnovator(applicationOne.getInnovatorEmail(), applicationOne.getInnovatorPassword());

    InnovatorService.updateProfileConstraint(
            new PasswordConstraintsModel(PasswordConstraint.PASSCODE), programmeId, innovatorToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }
  @Test
  public void EnableOkayInvalidProgrammeIdConflict(){
    InnovatorService.enableOkay(RandomStringUtils.randomNumeric(18),innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void EnableOkayDifferentProgrammeIdConflict(){
    final Triple<String, String, String> newInnovator = TestHelper.registerLoggedInInnovatorWithProgramme();

    InnovatorService.enableOkay(newInnovator.getLeft(),innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void EnableOkayInvalidSecretKeyUnauthorized(){

    InnovatorService.enableOkay(programmeId,"123")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void DisableOkayInvalidProgrammeId(){
    InnovatorService.disableOkay(RandomStringUtils.randomNumeric(18),innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void DisableOkayDifferentProgrammeId(){
    final Triple<String, String, String> newInnovator = TestHelper.registerLoggedInInnovatorWithProgramme();

    InnovatorService.disableOkay(newInnovator.getLeft(),innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void DisableOkayInvalidSecretKeyUnauthorized(){

    InnovatorService.disableOkay(programmeId,"123")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void UpdateOkayInvalidProgrammeId(){
    final UpdateOkayModel updateOkayModel=UpdateOkayModel.builder().firebaseKey("real-firebase-key").build();
    InnovatorService.updateOkay(updateOkayModel,RandomStringUtils.randomNumeric(18),innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void UpdateOkayDifferentProgrammeId(){
    final Triple<String, String, String> newInnovator = TestHelper.registerLoggedInInnovatorWithProgramme();

    final UpdateOkayModel updateOkayModel=UpdateOkayModel.builder().firebaseKey("real-firebase-key").build();
    InnovatorService.updateOkay(updateOkayModel,newInnovator.getLeft(),innovatorToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void UpdateOkayInvalidSecretKeyUnauthorized(){

    InnovatorService.disableOkay(programmeId,"123")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void LinkUserInvalidLinkingCodeConflict(){
    InnovatorHelper.enableOkay(programmeId,innovatorToken);
    Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

    AuthenticationFactorsService.enrolPush(EnrolmentChannel.BIOMETRIC.name(), secretKey,corporate.getRight());

    SimulatorService.simulateEnrolmentLinking(secretKey,corporate.getLeft(),"1235647586")
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("USER_NOT_FOUND"));
  }

  @Test //simulate accept okay API is already deprecated
  public void AcceptUserLinkingInvalidIdentityIdConflict(){
    InnovatorHelper.enableOkay(programmeId,innovatorToken);

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

    final String linkingCode= SecureHelper.enrolBiometricUser(corporate.getRight(), sharedKey);

    SimulatorService.simulateEnrolmentLinking(secretKey,corporate.getLeft(),linkingCode)
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptOkayIdentity(secretKey,RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("USER_NOT_FOUND"));
  }

  @Test //simulate accept okay API is already deprecated
  public void AcceptUserLinkingWithoutLinkingIdConflict(){
    InnovatorHelper.enableOkay(programmeId,innovatorToken);

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

    final String linkingCode= SecureHelper.enrolBiometricUser(corporate.getRight(), sharedKey);

    SimulatorService.simulateEnrolmentLinking(secretKey,corporate.getLeft(),linkingCode)
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptOkayIdentity(secretKey,corporate.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode",equalTo("REQUEST_NOT_FOUND"));
  }
}
