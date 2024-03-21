package opc.junit.secure.biometric;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.UserId;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.models.webhook.WebhookBiometricLoginEventModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.SemiService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class CorporateLoginBiometricTests extends AbstractLoginBiometricTests{

  @Override
  protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
            programme.getCorporatesProfileId(), programme.getSecretKey());

    return IdentityDetails.generateDetails(null, corporate.getLeft(),
            corporate.getRight(), IdentityType.CORPORATE, null,null);
  }

  @Test
  public void LoginBiometric_AuthTokenForSemiLinkedIdentities_Success(){
    final CreateCorporateModel targetCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
    final Pair<String, String> targetCorporate = CorporatesHelper.createBiometricEnrolledVerifiedCorporate(targetCorporateModel, secretKey, sharedKey);

    final CreateCorporateModel linkedCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
            .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                    .setName(targetCorporateModel.getRootUser().getName())
                    .setSurname(targetCorporateModel.getRootUser().getSurname())
                    .build())
            .build();

    final Pair<String, String> linkedCorporateId = CorporatesHelper.createAuthenticatedVerifiedCorporate(linkedCorporateModel, secretKey);

    AdminService.linkUseridToCorporateSemi(new UserId(linkedCorporateId.getLeft()), targetCorporate.getLeft(), adminImpersonatedTenantToken)
            .then()
            .statusCode(SC_NO_CONTENT);

    final String deviceId = SecureHelper.enrolAndGetDeviceId(targetCorporate.getRight(), targetCorporate.getLeft(), passcodeApp);

    final String challengeId = getChallengeId(deviceId, passcodeApp);

    final long timestamp = Instant.now().toEpochMilli();

    SimulatorHelper.acceptOkayLoginChallenge(secretKey, challengeId);

    final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, targetCorporate.getLeft());

    // We should get successful response for get identities with auth token from biometric login
    SemiService.getLinkedIdentities(secretKey, Optional.empty(), event.getAuthToken())
            .then()
            .statusCode(SC_OK);

    // We should get Forbidden for get auth factors with auth token from biometric login
    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), event.getAuthToken())
            .then()
            .statusCode(SC_FORBIDDEN)
            .body("errorCode", equalTo("ACCESS_TOKEN_REQUIRED"));
  }
}
