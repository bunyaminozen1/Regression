package opc.junit.smoke;

import io.cucumber.messages.internal.com.google.gson.Gson;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import commons.enums.State;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.database.OkayDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateOkayBrandingModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.secure.EnrolBiometricModel;
import opc.models.secure.LoginBiometricModel;
import opc.models.secure.LoginWithPasswordModel;
import opc.models.shared.PasswordModel;
import opc.models.webhook.WebhookBiometricLoginEventModel;
import opc.models.webhook.WebhookDataResponse;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.PasswordsService;
import opc.services.secure.SecureService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SecureBiometricTests extends BaseSmokeSetup {
    private static String existingConsumerAuthenticationToken;
    private static String existingCorporateAuthenticationToken;
    private static String existingCorporateId;
    final static String CHANNEL = EnrolmentChannel.BIOMETRIC.name();
    private static UpdateOkayBrandingModel updateBiometricBrandingModel;

    @BeforeAll
    public static void Setup() {

        existingCorporateAuthenticationToken = getExistingCorporateDetailsPasscodeApp().getLeft();
        existingCorporateId = getExistingCorporateDetailsPasscodeApp().getRight();

        existingConsumerAuthenticationToken = getExistingConsumerDetailsPasscodeApp().getLeft();

    }

    @Test
    public void Biometric_ConsumerEnrolRootUserPendingVerification_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, passcodeAppSecretKey);

        final String random = getRandom(consumer.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        issueEnrolChallenge(consumer.getRight(), enrolBiometricModel);

        assertAuthFactorsState(consumer.getRight(), State.PENDING_VERIFICATION.name());
        verifyDeviceId(consumer.getLeft(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_CorporateEnrolAuthenticatedUserPendingVerification_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                corporate.getRight());

        final String random = getRandom(user.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        assertAuthFactorsState(user.getRight(), State.PENDING_VERIFICATION.name());
        verifyDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_ExistingConsumerEnrolAuthenticatedUserPendingVerification_Success() {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                existingConsumerAuthenticationToken);

        final String random = getRandom(user.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        assertAuthFactorsState(user.getRight(), State.PENDING_VERIFICATION.name());
        verifyDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void LoginBiometric_CorporateRootUserGetChallengeId_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(corporate.getRight(),
                corporate.getLeft());

        SecureService.loginViaBiometric(passcodeAppSharedKey, new LoginBiometricModel(enrolBiometricModel.getRight()))
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginBiometric_CorporateAuthenticatedUserGetChallengeId_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                corporate.getRight());

        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(user.getRight(),
                user.getLeft());

        SecureService.loginViaBiometric(passcodeAppSharedKey, new LoginBiometricModel(enrolBiometricModel.getRight()))
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginBiometric_AuthenticatedUserFromExistingCorporateGetChallengeId_Success() {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                existingCorporateAuthenticationToken);

        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(user.getRight(),
                user.getLeft());

        SecureService.loginViaBiometric(passcodeAppSharedKey, new LoginBiometricModel(enrolBiometricModel.getRight()))
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginBiometric_ConsumerRootUserAcceptChallenge_Verified() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, passcodeAppSecretKey);

        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(consumer.getRight(),
                consumer.getLeft());

        final String challengeId = SecureService.loginViaBiometric(passcodeAppSharedKey, new LoginBiometricModel(enrolBiometricModel.getRight()))
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("challengeId");

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorService.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final WebhookBiometricLoginEventModel response = getWebhookResponse(timestamp);

        assertVerifiedChallenge(response, challengeId, consumer.getLeft());
    }

    @Test
    public void LoginBiometric_ExistingConsumerAuthenticatedUserAcceptChallenge_Verified() {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                existingConsumerAuthenticationToken);

        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(user.getRight(),
                user.getLeft());

        final String challengeId = SecureService.loginViaBiometric(passcodeAppSharedKey, new LoginBiometricModel(enrolBiometricModel.getRight()))
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("challengeId");

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorService.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final WebhookBiometricLoginEventModel response = getWebhookResponse(timestamp);

        assertVerifiedChallenge(response, challengeId, user.getLeft());
    }


    @Test
    public void LoginWithPassword_ConsumerRootUser_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, passcodeAppSecretKey);
        final String consumerEmail = createConsumerModel.getRootUser().getEmail();

        final Pair<EnrolBiometricModel, String> deviceId = enrolDeviceBiometric(consumer.getRight(), consumer.getLeft());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                .setEmail(consumerEmail)
                .setDeviceId(deviceId.getRight())
                .build();

        SecureService.loginWithPassword(passcodeAppSharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(passcodeApp.getProgrammeId()))
                .body("credential.type", equalTo(UserType.ROOT.name()))
                .body("credential.id", equalTo(consumer.getLeft()))
                .body("identity.type", equalTo(IdentityType.CONSUMER.getValue()))
                .body("identity.id", equalTo(consumer.getLeft()))
                .body("tokenType", equalTo("ACCESS"));
    }

    @Test
    public void LoginWithPassword_CorporateAuthUser_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, passcodeAppSecretKey, corporate.getRight());

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();
        PasswordsService.createPassword(createPasswordModel, user.getLeft(), passcodeAppSecretKey);

        final String userPassword = createPasswordModel.getPassword().getValue();

        final Pair<EnrolBiometricModel, String> deviceId = enrolDeviceBiometric(user.getRight(), user.getLeft());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(userPassword))
                .setEmail(usersModel.getEmail())
                .setDeviceId(deviceId.getRight())
                .build();

        SecureService.loginWithPassword(passcodeAppSharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(passcodeApp.getProgrammeId()))
                .body("credential.type", equalTo(UserType.USER.name()))
                .body("credential.id", equalTo(user.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(corporate.getLeft()))
                .body("tokenType", equalTo("ACCESS"));
    }

    @Test
    public void LoginWithPassword_ExistingCorporateAuthUser_Success() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, passcodeAppSecretKey, existingCorporateAuthenticationToken);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();
        PasswordsService.createPassword(createPasswordModel, user.getLeft(), passcodeAppSecretKey);

        final String userPassword = createPasswordModel.getPassword().getValue();

        final Pair<EnrolBiometricModel, String> deviceId = enrolDeviceBiometric(user.getRight(), user.getLeft());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(userPassword))
                .setEmail(usersModel.getEmail())
                .setDeviceId(deviceId.getRight())
                .build();

        SecureService.loginWithPassword(passcodeAppSharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(passcodeApp.getProgrammeId()))
                .body("credential.type", equalTo(UserType.USER.name()))
                .body("credential.id", equalTo(user.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(existingCorporateId))
                .body("tokenType", equalTo("ACCESS"));
    }

    @Test
    public void Biometric_ConsumerEnrolRootUserVerified_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, passcodeAppSecretKey);

        final String random = getRandom(consumer.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(consumer.getRight(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(passcodeAppSecretKey, consumer.getLeft(), linkingCode);

        assertAuthFactorsState(consumer.getRight(), State.ACTIVE.name());
        verifyDeviceId(consumer.getLeft(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_CorporateEnrolRootUserRejected_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final String random = getRandom(corporate.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        issueEnrolChallenge(corporate.getRight(), enrolBiometricModel);

        SimulatorHelper.rejectOkayIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(),
                State.INACTIVE);

        assertAuthFactorsState(corporate.getRight(), State.INACTIVE.name());
        verifyDeviceId(corporate.getLeft(), null);
    }

    @Test
    public void Biometric_CorporateEnrolAuthenticatedUserVerified_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                corporate.getRight());

        final String random = getRandom(user.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(passcodeAppSecretKey, user.getLeft(), linkingCode);

        assertAuthFactorsState(user.getRight(), State.ACTIVE.name());
        verifyDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_ExistingConsumerEnrolAuthenticatedUserVerified_Success() {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                existingConsumerAuthenticationToken);

        final String random = getRandom(user.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(passcodeAppSecretKey, user.getLeft(), linkingCode);

        assertAuthFactorsState(user.getRight(), State.ACTIVE.name());
        verifyDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId());
    }

    /**
     * After biometric enrolment moved to Secure Gateway, get branding Api was exposed
     * to retrieve the branding for the consent screen. This Api retrieve data via innovator Api
     * and consent screen is displayed according to changes that is made with innovator portal
     */

    @Test
    public void Consumer_GetBiometricBranding_Success() {

        updateBranding();

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, passcodeAppSecretKey);

        final String random = SecureHelper.associate(consumer.getRight(), passcodeAppSharedKey);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        SecureService.getBiometricBranding(consumer.getRight(), passcodeAppSharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .body("fontFaceFamily", equalTo(updateBiometricBrandingModel.getFontFaceFamily()))
                .body("textColor", equalTo(updateBiometricBrandingModel.getTextColor()))
                .body("confirmButtonColor", equalTo(updateBiometricBrandingModel.getConfirmButtonColor()))
                .body("confirmTextColor", equalTo(updateBiometricBrandingModel.getConfirmTextColor()))
                .body("declineButtonColor", equalTo(updateBiometricBrandingModel.getDeclineButtonColor()))
                .body("declineTextColor", equalTo(updateBiometricBrandingModel.getDeclineTextColor()))
                .body("backgroundColor", equalTo(updateBiometricBrandingModel.getBackgroundColor()));
    }

    @Test
    public void Corporate_GetBiometricBranding_Success() {

        updateBranding();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);

        final String random = SecureHelper.associate(corporate.getRight(), passcodeAppSharedKey);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        SecureService.getBiometricBranding(corporate.getRight(), passcodeAppSharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .body("fontFaceFamily", equalTo(updateBiometricBrandingModel.getFontFaceFamily()))
                .body("textColor", equalTo(updateBiometricBrandingModel.getTextColor()))
                .body("confirmButtonColor", equalTo(updateBiometricBrandingModel.getConfirmButtonColor()))
                .body("confirmTextColor", equalTo(updateBiometricBrandingModel.getConfirmTextColor()))
                .body("declineButtonColor", equalTo(updateBiometricBrandingModel.getDeclineButtonColor()))
                .body("declineTextColor", equalTo(updateBiometricBrandingModel.getDeclineTextColor()))
                .body("backgroundColor", equalTo(updateBiometricBrandingModel.getBackgroundColor()));
    }

    /**
     * These tests are about getting biometric challenges with okay_session_id instead of external_request_id
     */

    @Test
    public void Corporate_GetIssueLoginBiometricChallenge_Pending() throws SQLException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(corporate.getRight(),
                corporate.getLeft());

        final String challengeId = SecureHelper.loginViaBiometric(passcodeAppSharedKey, enrolBiometricModel.getRight());

        final Map<String, String> biometricChallenge = OkayDatabaseHelper.getBiometricChallenge(
                challengeId).get(0);

        final String externalRequestId = biometricChallenge.get("ext_request_id");
        final String sessionId = biometricChallenge.get("okay_session_id");

        SecureService.getBiometricChallengesStatus(Optional.of(corporate.getRight()), passcodeAppSharedKey,
                        externalRequestId)
                .then()
                .statusCode(SC_NOT_FOUND);

        SecureService.getBiometricChallengesStatus(Optional.of(corporate.getRight()), passcodeAppSharedKey, sessionId)
                .then()
                .statusCode(SC_OK)
                .body("challengeStatus", equalTo(State.PENDING.name()))
                .body("okayStatus", equalTo(State.ISSUED.name()))
                .body("challengeId", equalTo(challengeId));
    }

    @Test
    public void Corporate_GetAcceptLoginBiometricChallenge_Completed() throws SQLException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final Pair<EnrolBiometricModel, String> enrolBiometricModel = enrolDeviceBiometric(corporate.getRight(),
                corporate.getLeft());

        final String challengeId = SecureService.loginViaBiometric(passcodeAppSharedKey, new LoginBiometricModel(enrolBiometricModel.getRight()))
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("challengeId");

        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        final Map<String, String> biometricChallenge = OkayDatabaseHelper.getBiometricChallenge(
                challengeId).get(0);

        final String externalRequestId = biometricChallenge.get("ext_request_id");
        final String sessionId = biometricChallenge.get("okay_session_id");

        SecureService.getBiometricChallengesStatus(Optional.of(corporate.getRight()), passcodeAppSharedKey,
                        externalRequestId)
                .then()
                .statusCode(SC_NOT_FOUND);

        SecureService.getBiometricChallengesStatus(Optional.of(corporate.getRight()), passcodeAppSharedKey, sessionId)
                .then()
                .statusCode(SC_OK)
                .body("challengeStatus", equalTo(State.COMPLETED.name()))
                .body("okayStatus", equalTo(State.VERIFIED.name()))
                .body("challengeId", equalTo(challengeId));
    }

    private Pair<EnrolBiometricModel, String> enrolDeviceBiometric(final String authenticationToken,
                                                                   final String identityId) {

        final String random = SecureHelper.associate(authenticationToken, passcodeAppSharedKey);
        final String deviceId = RandomStringUtils.randomAlphanumeric(40);

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(deviceId)
                .build();

        final String linking_code = SecureService.enrolDeviceBiometric(authenticationToken, passcodeAppSharedKey,
                        enrolBiometricModel)
                .jsonPath().getString("linkingCode");

        SimulatorHelper.simulateEnrolmentLinking(passcodeAppSecretKey, identityId, linking_code);

        return Pair.of(enrolBiometricModel, deviceId);
    }

    private String issueEnrolChallenge(final String token,
                                       final EnrolBiometricModel enrolBiometricModel) {
        return SecureService.enrolDeviceBiometric(token, sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");
    }

    private String getRandom(final String token) {

        return SecureService.associate(passcodeAppSharedKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("random");
    }

    private void assertAuthFactorsState(final String token,
                                        final String expectedStatus) {

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), token),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("factors[0].status").equals(expectedStatus),
                Optional.of(String.format("Expecting 200 with an authentication factor in state %s, check logged payload", expectedStatus)));
    }

    private void assertVerifiedChallenge(final WebhookBiometricLoginEventModel response,
                                         final String challengeId, final String identityId) {

        assertNotNull(response.getAuthToken());
        assertEquals(challengeId, response.getChallengeId());
        assertEquals(identityId, response.getCredential().get("id"));
        assertEquals(State.VERIFIED.name(), response.getStatus());
        assertEquals(CHANNEL, response.getType());
    }

    public static void verifyDeviceId(final String identityId, final String enrolledDeviceId) {

        TestHelper.ensureDatabaseResultAsExpected(10,
                () -> AuthSessionsDatabaseHelper.getCredentialFactors(identityId),
                x -> Objects.equals(x.get(1).get("device_id"), enrolledDeviceId),
                Optional.of(String.format("Retrieved device id does not match device with id %s as expected", enrolledDeviceId)));
    }

    private WebhookBiometricLoginEventModel getWebhookResponse(final long timestamp) {

        final WebhookDataResponse webhookResponse = WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.STEP_UP);

        return new Gson().fromJson(webhookResponse.getContent(),
                WebhookBiometricLoginEventModel.class);
    }

    public static void updateBranding() {
        updateBiometricBrandingModel = UpdateOkayBrandingModel
                .builder()
                .fontFaceFamily("Arial")
                .textColor("#252D27")
                .confirmButtonColor("#33FF6E")
                .confirmTextColor("#12345D")
                .declineButtonColor("#D31347")
                .declineTextColor("#1245DE")
                .backgroundColor("#DAFF33").build();

        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        InnovatorService.updateOkayBranding(updateBiometricBrandingModel, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .body("fontFaceFamily", equalTo(updateBiometricBrandingModel.getFontFaceFamily()))
                .body("textColor", equalTo(updateBiometricBrandingModel.getTextColor()))
                .body("confirmButtonColor", equalTo(updateBiometricBrandingModel.getConfirmButtonColor()))
                .body("confirmTextColor", equalTo(updateBiometricBrandingModel.getConfirmTextColor()))
                .body("declineButtonColor", equalTo(updateBiometricBrandingModel.getDeclineButtonColor()))
                .body("declineTextColor", equalTo(updateBiometricBrandingModel.getDeclineTextColor()))
                .body("backgroundColor", equalTo(updateBiometricBrandingModel.getBackgroundColor()));
    }

}