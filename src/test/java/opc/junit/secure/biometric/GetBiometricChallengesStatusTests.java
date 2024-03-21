package opc.junit.secure.biometric;

import static commons.enums.State.COMPLETED;
import static commons.enums.State.DECLINED;
import static commons.enums.State.ISSUED;
import static commons.enums.State.PENDING;
import static commons.enums.State.VERIFIED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import commons.enums.State;
import opc.junit.database.OkayDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.secure.EnrolBiometricModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GetBiometricChallengesStatusTests extends BaseBiometricSetup {

  /**
   * This class is about getting biometric challenges with okay_session_id instead of external_request_id
   */

  private static String corporateAuthToken;
  private static String consumerAuthToken;
  private static String corporateDeviceId;
  private static String consumerDeviceId;
  private static String externalRequestId;


  @BeforeAll
  public static void setup() {
    corporateSetup();
    consumerSetup();
  }

  @Test
  public void Corporate_IssueLoginBiometricChallenge_Pending() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, corporateDeviceId);
    getBiometricChallengeInformation(challengeId);

    callGetChallengeApiWithExternalRequestId(corporateAuthToken, externalRequestId, challengeId, PENDING, ISSUED);
  }

  @Test
  public void Consumer_IssueLoginBiometricChallenge_Pending() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    getBiometricChallengeInformation(challengeId);
    
    callGetChallengeApiWithExternalRequestId(consumerAuthToken, externalRequestId, challengeId, PENDING, ISSUED);
  }

  @Test
  public void Corporate_AcceptLoginBiometricChallenge_Completed() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, corporateDeviceId);
    SimulatorHelper.acceptOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);
    
    callGetChallengeApiWithExternalRequestId(corporateAuthToken, externalRequestId, challengeId, COMPLETED, VERIFIED);
  }

  @Test
  public void Corporate_AcceptLoginBiometricChallengeNoToken_Completed() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, corporateDeviceId);
    SimulatorHelper.acceptOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);
    
    callGetChallengeApiWithExternalRequestId(corporateAuthToken, externalRequestId, challengeId, COMPLETED, VERIFIED);
  }

  @Test
  public void Consumer_AcceptLoginBiometricChallenge_Completed() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    SimulatorHelper.acceptOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);
    
    callGetChallengeApiWithExternalRequestId(consumerAuthToken, externalRequestId, challengeId, COMPLETED, VERIFIED);
  }

  @Test
  public void Corporate_RejectLoginBiometricChallenge_() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, corporateDeviceId);
    SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);
    
    callGetChallengeApiWithExternalRequestId(corporateAuthToken, externalRequestId, challengeId, DECLINED, DECLINED);
  }

  @Test
  public void Consumer_RejectLoginBiometricChallenge_() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);
    
    callGetChallengeApiWithExternalRequestId(consumerAuthToken, externalRequestId, challengeId, DECLINED, DECLINED);
  }

  @Test
  public void Biometric_GetChallengeStatusInvalidToken_Unauthorized() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);

    SecureService.getBiometricChallengesStatus(Optional.of(RandomStringUtils.randomAlphanumeric(20)), sharedKey, externalRequestId)
            .then()
            .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void Biometric_GetChallengeStatusCrossIdentity_NotFound() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);

    SecureService.getBiometricChallengesStatus(Optional.of(corporateAuthToken), sharedKey, externalRequestId)
            .then()
            .statusCode(SC_NOT_FOUND);
  }
  @Test
  public void Biometric_GetChallengeStatusDifferentSharedKey_NotFound() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);

    SecureService.getBiometricChallengesStatus(Optional.of(corporateAuthToken), applicationFour.getSharedKey(), externalRequestId)
            .then()
            .statusCode(SC_NOT_FOUND);
  }
  @Test
  public void Biometric_GetChallengeStatusDifferentSharedKeyNoToken_NotFound() throws SQLException {
    final String challengeId = SecureHelper.loginViaBiometric(sharedKey, consumerDeviceId);
    SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);
    getBiometricChallengeInformation(challengeId);

    SecureService.getBiometricChallengesStatus(Optional.empty(), applicationFour.getSharedKey(), externalRequestId)
            .then()
            .statusCode(SC_NOT_FOUND);
  }

  private static void consumerSetup() {

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
    final String consumerId = authenticatedConsumer.getLeft();
    consumerAuthToken = authenticatedConsumer.getRight();

    ConsumersHelper.verifyKyc(secretKey, consumerId);
    consumerDeviceId = enrolDeviceBiometric(consumerAuthToken, consumerId);
  }

  private static void corporateSetup() {

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

    final String corporateId = authenticatedCorporate.getLeft();
    corporateAuthToken = authenticatedCorporate.getRight();

    CorporatesHelper.verifyKyb(secretKey, corporateId);

    corporateDeviceId = enrolDeviceBiometric(corporateAuthToken, corporateId);
  }

  private static String enrolDeviceBiometric(final String authenticationToken,
                                             final String identityId) {

    final String random = SecureHelper.associate(authenticationToken, sharedKey);
    final String deviceId = RandomStringUtils.randomAlphanumeric(40);

    final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
            .random(random)
            .deviceId(deviceId)
            .build();

    final String linking_code = SecureService.enrolDeviceBiometric(authenticationToken, sharedKey,
                    enrolBiometricModel)
            .jsonPath().getString("linkingCode");

    SimulatorHelper.simulateEnrolmentLinking(secretKey, identityId, linking_code);

    return deviceId;
  }

  private static void callGetChallengeApiWithExternalRequestId(final String token, 
                                                               final String sessionId,
                                                               final String challengeId, 
                                                               final State challengeStatus, 
                                                               final State okayStatus){
    TestHelper.ensureAsExpected(15,
            () -> SecureService.getBiometricChallengesStatus(Optional.of(token), sharedKey, sessionId),
            x-> x.statusCode() == SC_OK && x.jsonPath().getString("challengeStatus").equals(challengeStatus.name()),
            Optional.of(String.format("Expecting %s status is not matched with the response, check logged payload", challengeStatus.name())))

            .then()
            .body("okayStatus", equalTo(okayStatus.name()))
            .body("challengeId", equalTo(challengeId));
  }

  private static void getBiometricChallengeInformation(final String challengeId) throws SQLException {
    final Map<String, String> biometricChallenge = OkayDatabaseHelper.getBiometricChallenge(challengeId).get(0);
    externalRequestId = biometricChallenge.get("ext_request_id");
  }

}