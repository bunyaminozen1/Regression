package opc.junit.multi.access;

import opc.enums.authy.AuthyMessage;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssuePushNotificationToStepUpTokenTests extends BaseAuthenticationSetup {

    @Test
    public void IssuePushNotification_CorporateRoot_Success() {
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void IssuePushNotification_ConsumerRoot_Success() {
        final Pair<String, String> consumer = ConsumersHelper.createStepupAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void IssuePushNotification_NotificationCheck_Success() throws SQLException {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(createCorporateModel, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK);

        final Map<String, String> notification = AuthySimulatorDatabaseHelper.getNotification(applicationOne.getProgrammeId()).get(0);
        assertEquals(String.format(AuthyMessage.PUSH_STEPUP.getMessage(), applicationOne.getProgrammeName()), notification.get("message"));
    }

    @Test
    public void IssuePushNotification_AlreadyIssuedNotVerified_Access() {
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void IssuePushNotification_OneSessionAndVerified_RequestAlreadyProcessed() {
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(corporateProfileId, secretKey);

        String sessionId = AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .extract()
            .jsonPath()
            .get("id");

        SimulatorHelper.acceptAuthyStepUp(secretKey, sessionId);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssuePushNotification_TwoSessionsAndVerified_Success() {
        final CreateCorporateModel createCorporateModel =
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(createCorporateModel, secretKey);

        String sessionId = AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .extract()
            .jsonPath()
            .get("id");

        SimulatorHelper.acceptAuthyStepUp(secretKey, sessionId);

        AuthenticationHelper.logout(corporate.getRight(), secretKey);
        String identityEmail = createCorporateModel.getRootUser().getEmail();
        String newToken = AuthenticationHelper.login(identityEmail, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, newToken)
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void IssuePushNotification_OneSessionAndRejected_RequestAlreadyProcessed() {
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(corporateProfileId, secretKey);

        String sessionId = AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .extract()
            .jsonPath()
            .get("id");

        SimulatorHelper.rejectAuthyStepUp(secretKey, sessionId);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssuePushNotification_TwoSessionsAndRejected_Success() {
        final CreateCorporateModel createCorporateModel =
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(createCorporateModel, secretKey);

        String sessionId = AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .extract()
            .jsonPath()
            .get("id");

        SimulatorHelper.rejectAuthyStepUp(secretKey, sessionId);

        AuthenticationHelper.logout(corporate.getRight(), secretKey);
        String identityEmail = createCorporateModel.getRootUser().getEmail();
        String newToken = AuthenticationHelper.login(identityEmail, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, newToken)
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void IssuePushNotification_IdentityNotEnrolled_ChannelNotRegistered() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, consumer.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void IssuePushNotification_AuthyNotSupported_ChannelNotSupported() {
        String corporateProfileIdAppThree = applicationThree.getCorporatesProfileId();
        String secretKeyAppThree = applicationThree.getSecretKey();

        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(corporateProfileIdAppThree, secretKeyAppThree);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyAppThree, corporate.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }

    @Test
    public void IssuePushNotification_InvalidChannel_BadRequest() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssuePushNotification_NoApiKey_BadRequest() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), "", consumer.getRight())
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssuePushNotification_InvalidApiKey_Unauthorised() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), "123", consumer.getRight())
            .then()
            .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssuePushNotification_InvalidToken_Unauthorised() {
        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), "123", RandomStringUtils.randomAlphanumeric(5))
            .then()
            .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssuePushNotification_RootUserLoggedOut_Unauthorised() {
        final Pair<String, String> consumer = ConsumersHelper.createStepupAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.logout(secretKey, consumer.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, consumer.getRight())
            .then()
            .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssuePushNotification_BackofficeImpersonator_Forbidden() {
        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey,
                getBackofficeImpersonateToken(corporate.getLeft(), IdentityType.CORPORATE))
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssuePushNotification_DifferentInnovatorApiKey_Forbidden() {
        final Pair<String, String> consumer = ConsumersHelper.createStepupAuthenticatedConsumer(consumerProfileId, secretKey);

        final Triple<String, String, String> innovator =
            TestHelper.registerLoggedInInnovatorWithProgramme();
        final String otherInnovatorSecretKey =
            InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), otherInnovatorSecretKey, consumer.getRight())
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssuePushNotification_OtherApplicationSecretKey_Forbidden() {
        final Pair<String, String> consumer = ConsumersHelper.createStepupAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), consumer.getRight())
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssuePushNotification_EmptyChannel_Forbidden() {
        final Pair<String, String> consumer = ConsumersHelper.createStepupAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.issuePushStepup("", applicationThree.getSecretKey(), consumer.getRight())
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssuePushNotification_Biometric_Success() {

        final Pair<String, String> consumer = ConsumersHelper.
            createBiometricStepupAuthenticatedConsumer(passcodeAppConsumerProfileId, passcodeAppSharedKey, passcodeAppSecretKey);

        final String challengeId = TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, consumer.getRight()),
                SC_OK)
            .jsonPath()
            .get("id");

        SimulatorHelper.successfullyAcceptOkayOwt(passcodeAppSecretKey, challengeId);
    }

    @Test
    public void IssuePushNotification_Biometric_Reject() {

        final Pair<String, String> consumer = ConsumersHelper.
            createBiometricStepupAuthenticatedConsumer(passcodeAppConsumerProfileId, passcodeAppSharedKey, passcodeAppSecretKey);

        final String challengeId = TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, consumer.getRight()),
                SC_OK)
            .jsonPath()
            .get("id");

        SimulatorHelper.successfullyRejectOkayOwt(passcodeAppSecretKey, challengeId);
    }

    @Test
    public void IssuePushNotification_Biometric_Pin_Success() {

        final Pair<String, String> consumer = ConsumersHelper.
            createBiometricStepupAuthenticatedConsumer(passcodeAppConsumerProfileId, passcodeAppSharedKey, passcodeAppSecretKey);

        final String challengeId = TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, consumer.getRight()),
                SC_OK)
            .jsonPath()
            .get("id");

        SimulatorHelper.okayOwtWithPin(passcodeAppSecretKey, challengeId, TestHelper.getDefaultPassword(passcodeAppSecretKey), "COMPLETED");
    }

    @Test
    public void IssuePushNotification_Biometric_Wrong_Pin() {

        final Pair<String, String> consumer = ConsumersHelper.
            createBiometricStepupAuthenticatedConsumer(passcodeAppConsumerProfileId, passcodeAppSharedKey, passcodeAppSecretKey);

        final String challengeId = TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, consumer.getRight()),
                SC_OK)
            .jsonPath()
            .get("id");

        SimulatorHelper.okayOwtWithPin(passcodeAppSecretKey, challengeId, "WRONG", "DECLINED");
    }
}
