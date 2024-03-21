package opc.junit.multi.webhooks;

import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.multi.users.UsersModel;
import opc.models.webhook.WebhookStepUpModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(MultiTags.IDENTITY_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public abstract class AbstractStepUpWebhooksTests extends BaseWebhooksSetup {

    @BeforeAll
    public static void setup() {

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.setProgrammeAuthyChallengeLimit(passcodeAppProgrammeId, resetCount, adminToken);
    }

    @Test
    public void RootUser_StepUpOtpVerified_Success() throws InterruptedException, SQLException {

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE,
                EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getIdentityToken());

        final String challengeId = AuthSessionsDatabaseHelper.getChallengeWithIdentityId(
                getIdentityId()).get(0).get("id");

        final WebhookStepUpModel event = getWebhookResponse(timestamp, getIdentityId());

        assertStepUpEvent(getIdentityId(), getIdentityId(), UserType.ROOT.name(),
                getIdentityType(), challengeId, "VERIFIED", "SMS_OTP", event, true);
    }

    @Test
    public void AuthenticatedUser_StepUpOtpVerified_Success() throws InterruptedException, SQLException {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, passcodeAppSecretKey,
                getIdentityToken());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE,
                EnrolmentChannel.SMS.name(), passcodeAppSecretKey, user.getRight());

        final String challengeId = AuthSessionsDatabaseHelper.getChallengeWithIdentityId(
                getIdentityId()).get(0).get("id");

        final WebhookStepUpModel event = getWebhookResponse(timestamp, user.getLeft());

        assertStepUpEvent(user.getLeft(), getIdentityId(), UserType.USER.name(),
                getIdentityType(), challengeId, "VERIFIED", "SMS_OTP", event, true);
    }

    @Test
    public void RootUser_AuthyStepUpVerified_Success() throws InterruptedException {

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getIdentityToken());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, getIdentityToken());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptAuthyStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, getIdentityId());

        assertStepUpEvent(getIdentityId(), getIdentityId(), UserType.ROOT.name(),
                getIdentityType(), challengeId, "VERIFIED", "AUTHY_PUSH", event, true);
    }

    @Test
    public void AuthenticatedUser_AuthyStepUpVerified_Success() throws InterruptedException {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                getIdentityToken());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), passcodeAppSecretKey, user.getRight());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, user.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptAuthyStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, user.getLeft());

        assertStepUpEvent(user.getLeft(), getIdentityId(), UserType.USER.name(),
                getIdentityType(), challengeId, "VERIFIED", "AUTHY_PUSH", event, true);
    }

    @Test
    public void RootUser_AuthyStepUpRejected_Declined() throws InterruptedException {

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getIdentityToken());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, getIdentityToken());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectAuthyStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, getIdentityId());

        assertStepUpEvent(getIdentityId(), getIdentityId(), UserType.ROOT.name(),
                getIdentityType(), challengeId, "DECLINED", "AUTHY_PUSH", event, false);
    }

    @Test
    public void AuthenticatedUser_AuthyStepUpRejected_Declined() throws InterruptedException {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                getIdentityToken());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), passcodeAppSecretKey, user.getRight());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, user.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectAuthyStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, user.getLeft());

        assertStepUpEvent(user.getLeft(), getIdentityId(), UserType.USER.name(),
                getIdentityType(), challengeId, "DECLINED", "AUTHY_PUSH", event, false);
    }

    @Test
    public void RootUser_BiometricStepUpVerified_Success() throws InterruptedException {

        SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getIdentityToken());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getIdentityToken());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptBiometricStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, getIdentityId());

        assertStepUpEvent(getIdentityId(), getIdentityId(), UserType.ROOT.name(),
                getIdentityType(), challengeId, "VERIFIED", "BIOMETRIC", event, true);
    }

    @Test
    public void AuthenticatedUser_BiometricStepUpVerified_Success() throws InterruptedException {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                getIdentityToken());

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, user.getRight());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, user.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptBiometricStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, user.getLeft());

        assertStepUpEvent(user.getLeft(), getIdentityId(), UserType.USER.name(),
                getIdentityType(), challengeId, "VERIFIED", "BIOMETRIC", event, true);
    }

    @Test
    public void RootUser_BiometricStepUpRejected_Declined() throws InterruptedException {

        SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getIdentityToken());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getIdentityToken());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectBiometricStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, getIdentityId());

        assertStepUpEvent(getIdentityId(), getIdentityId(), UserType.ROOT.name(),
                getIdentityType(), challengeId, "DECLINED", "BIOMETRIC", event, false);
    }

    @Test
    public void AuthenticatedUser_BiometricStepUpRejected_Declined() throws InterruptedException {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
                getIdentityToken());

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, user.getRight());

        final String challengeId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, user.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectBiometricStepUp(passcodeAppSecretKey, challengeId);

        final WebhookStepUpModel event = getWebhookResponse(timestamp, user.getLeft());

        assertStepUpEvent(user.getLeft(), getIdentityId(), UserType.USER.name(),
                getIdentityType(), challengeId, "DECLINED", "BIOMETRIC", event, false);
    }

    protected abstract String getIdentityId();

    protected abstract String getIdentityToken();

    protected abstract String getIdentityType();

    private WebhookStepUpModel getWebhookResponse(final long timestamp,
                                                  final String userId) {
        return (WebhookStepUpModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.STEP_UP,
                Pair.of("credential.id", userId),
                WebhookStepUpModel.class,
                ApiSchemaDefinition.StepupEvent);
    }

    private void assertStepUpEvent(final String userId,
                                   final String identityId,
                                   final String userType,
                                   final String identityType,
                                   final String challengeId,
                                   final String status,
                                   final String eventType,
                                   final WebhookStepUpModel stepUpEvent,
                                   final boolean authTokenExpected) {

        assertEquals(userId, stepUpEvent.getCredential().get("id"));
        assertEquals(userType, stepUpEvent.getCredential().get("type"));
        assertEquals(identityId, stepUpEvent.getIdentity().get("id"));
        assertEquals(identityType, stepUpEvent.getIdentity().get("type"));
        assertEquals(challengeId, stepUpEvent.getChallengeId());
        assertEquals(status, stepUpEvent.getStatus());
        assertEquals(eventType, stepUpEvent.getType());
        assertNotNull(stepUpEvent.getPublishedTimestamp());

        if (authTokenExpected) {
            if (!eventType.equals("BIOMETRIC")) {
                assertEquals("", stepUpEvent.getAuthToken());
            } else {
                assertNotNull(stepUpEvent.getAuthToken());
            }
        } else {
            assertNull(stepUpEvent.getAuthToken());
        }
    }
}
