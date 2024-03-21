package opc.junit.secure.biometric;

import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.EnrolmentChannel;
import commons.enums.State;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.secure.BiometricPinModel;
import opc.models.secure.LoginBiometricModel;
import opc.models.shared.Identity;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.models.webhook.WebhookBiometricLoginEventModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Execution(ExecutionMode.CONCURRENT)
public abstract class AbstractLoginBiometricTests extends BaseBiometricSetup {

    final static String CHANNEL = EnrolmentChannel.BIOMETRIC.name();

    @BeforeAll
    public static void setup() {
        InnovatorHelper.enableWebhook(
                UpdateProgrammeModel.WebHookUrlSetup(scaMcApp.getProgrammeId(), false, webhookServiceDetails.getRight()),
                scaMcApp.getProgrammeId(), innovatorToken);
    }

    @Test
    public void LoginBiometric_RootUserGetChallengeId_Success() {
        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        SecureService.loginViaBiometric(sharedKey, new LoginBiometricModel(deviceId))
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginBiometric_AuthenticatedUserGetChallengeId_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, identity.getToken());

        final String deviceId = SecureHelper.enrolAndGetDeviceId(user.getRight(), user.getLeft(), passcodeApp);

        SecureService.loginViaBiometric(sharedKey, new LoginBiometricModel(deviceId))
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginBiometric_RootUserAcceptChallenge_Verified() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        final String challengeId = getChallengeId(deviceId, passcodeApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(secretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertVerifiedChallenge(event, challengeId, identity.getId());
    }

    @Test
    public void LoginBiometric_AuthenticatedUserAcceptChallenge_Verified() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, identity.getToken());

        final String deviceId = SecureHelper.enrolAndGetDeviceId(user.getRight(), user.getLeft(), passcodeApp);

        final String challengeId = getChallengeId(deviceId, passcodeApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(secretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, user.getLeft());

        assertVerifiedChallenge(event, challengeId, user.getLeft());
    }

    @Test
    public void LoginBiometric_RootUserRejectChallenge_Declined() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        final String challengeId = getChallengeId(deviceId, passcodeApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertDeclinedChallenge(event, challengeId, identity.getId());
    }

    @Test
    public void LoginBiometric_AuthenticatedUserRejectChallenge_Declined() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, identity.getToken());

        final String deviceId = SecureHelper.enrolAndGetDeviceId(user.getRight(), user.getLeft(), passcodeApp);

        final String challengeId = getChallengeId(deviceId, passcodeApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectOkayLoginChallenge(secretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, user.getLeft());

        assertDeclinedChallenge(event, challengeId, user.getLeft());
    }

    @Test
    public void LoginBiometric_UserEntersValidPin_Verified() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        final String challengeId = getChallengeId(deviceId, passcodeApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(secretKey, challengeId, new BiometricPinModel(TestHelper.getDefaultPassword(secretKey)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertVerifiedChallenge(event, challengeId, identity.getId());
    }

    @Test
    public void LoginBiometric_UserEntersInvalidPin_Declined() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        final String challengeId = getChallengeId(deviceId, passcodeApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(secretKey, challengeId, new BiometricPinModel(RandomStringUtils.randomNumeric(4)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertDeclinedChallenge(event, challengeId, identity.getId());
    }

    @Test
    public void LoginBiometric_AcceptChallengeGenerateStepUpAccessToken_Success() {

        final IdentityDetails identity = getIdentity(scaMcApp);

        ManagedCardsService.getManagedCards(scaMcApp.getSecretKey(), Optional.empty(), identity.getToken())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), scaMcApp);

        final String challengeId = getChallengeId(deviceId, scaMcApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(scaMcApp.getSecretKey(), challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        // Biometric login should return auth token instead of access
        ManagedCardsService.getManagedCards(scaMcApp.getSecretKey(), Optional.empty(), event.getAuthToken())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("ACCESS_TOKEN_REQUIRED"));

        final Identity accessRequest = new Identity(new IdentityModel(identity.getId(), identity.getIdentityType()));

        final String accessToken = generateAccessToken(accessRequest, event.getAuthToken(), identity, scaMcApp);

        ManagedCardsService.getManagedCards(scaMcApp.getSecretKey(), Optional.empty(), accessToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void LoginBiometric_PinValidationGenerateStepUpAccessToken_Verified() {

        final IdentityDetails identity = getIdentity(scaMcApp);

        ManagedCardsService.getManagedCards(scaMcApp.getSecretKey(), Optional.empty(), identity.getToken())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), scaMcApp);

        final String challengeId = getChallengeId(deviceId, scaMcApp);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(scaMcApp.getSecretKey(), challengeId, new BiometricPinModel(TestHelper.getDefaultPassword(secretKey)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        // Pin fallback should return auth token instead of access
        ManagedCardsService.getManagedCards(scaMcApp.getSecretKey(), Optional.empty(), event.getAuthToken())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("ACCESS_TOKEN_REQUIRED"));

        final Identity accessRequest = new Identity(new IdentityModel(identity.getId(), identity.getIdentityType()));

        final String accessToken = generateAccessToken(accessRequest, event.getAuthToken(), identity, scaMcApp);

        ManagedCardsService.getManagedCards(scaMcApp.getSecretKey(), Optional.empty(), accessToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    // TODO It will be activated after fixing bug related secure api
//  @Test
//  public void LoginBiometric_InvalidSharedKey_Unauthorized() {
//
//    final EnrolBiometricModel enrolBiometricModel = enrolDeviceBiometric(getIdentityToken(),
//        getIdentityId());
//
//    SecureService.loginViaBiometric(RandomStringUtils.randomAlphanumeric(10), enrolBiometricModel)
//        .then()
//        .statusCode(SC_UNAUTHORIZED);
//  }

    @Test
    public void LoginBiometric_WithoutSharedKey_BadRequest() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        SecureService.loginViaBiometric("", new LoginBiometricModel(deviceId))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("programme-key"));
    }

    @Test
    public void LoginBiometric_DifferentSharedKey_Unauthorized() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final String deviceId = SecureHelper.enrolAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        SecureService.loginViaBiometric(scaMcApp.getSharedKey(), new LoginBiometricModel(deviceId))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void LoginBiometric_WithoutDeviceId_DeviceIdRequired() {

        SecureService.loginViaBiometric(sharedKey, new LoginBiometricModel(null))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("deviceId"))
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"));
    }

    @Test
    public void LoginBiometric_InvalidDeviceId_DeviceNotEnrolled() {

        SecureService.loginViaBiometric(sharedKey, new LoginBiometricModel(RandomStringUtils.randomAlphanumeric(40)))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DEVICE_NOT_ENROLLED"));
    }

    @Test
    public void LoginBiometric_RootUserBiometricEnrolledWithOtpGetChallengeId_Success() {
        final IdentityDetails identity = getIdentity(passcodeApp);

        final String deviceId = SecureHelper.enrolBiometricWithOtpAndGetDeviceId(identity.getToken(), identity.getId(), passcodeApp);

        SecureService.loginViaBiometric(sharedKey, new LoginBiometricModel(deviceId))
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    protected String getChallengeId(final String deviceId, final ProgrammeDetailsModel programme) {

        return SecureService.loginViaBiometric(programme.getSharedKey(), new LoginBiometricModel(deviceId))
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("challengeId");
    }

    protected WebhookBiometricLoginEventModel getWebhookResponse(final long timestamp, final String identityId) {

        return (WebhookBiometricLoginEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.STEP_UP,
                Pair.of("credential.id", identityId),
                WebhookBiometricLoginEventModel.class,
                ApiSchemaDefinition.StepupEvent);
    }

    private String generateAccessToken(final Identity accessRequest,
                                       final String authToken,
                                       final IdentityDetails identity,
                                       final ProgrammeDetailsModel programme) {

        return AuthenticationService.accessToken(accessRequest, programme.getSecretKey(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(identity.getId()))
                .body("identity.id", equalTo(identity.getId()))
                .body("identity.type", equalTo(identity.getIdentityType().name()))
                .body("status", equalTo("STEPPED_UP"))
                .body("token", notNullValue())
                .extract()
                .jsonPath()
                .getString("token");
    }

    protected void assertVerifiedChallenge(final WebhookBiometricLoginEventModel event,
                                         final String challengeId,
                                         final String identityId) {

        assertNotNull(event.getAuthToken());
        assertEquals(challengeId, event.getChallengeId());
        assertEquals(identityId, event.getCredential().get("id"));
        assertEquals(State.VERIFIED.name(), event.getStatus());
        assertEquals(CHANNEL, event.getType());
    }

    private void assertDeclinedChallenge(final WebhookBiometricLoginEventModel event,
                                         final String challengeId,
                                         final String identityId) {

        assertNull(event.getAuthToken());
        assertEquals(challengeId, event.getChallengeId());
        assertEquals(identityId, event.getCredential().get("id"));
        assertEquals(State.DECLINED.name(), event.getStatus());
        assertEquals(CHANNEL, event.getType());
    }

    protected abstract IdentityDetails getIdentity(final ProgrammeDetailsModel programme);
}