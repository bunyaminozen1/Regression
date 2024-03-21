package opc.junit.multi.authenticationfactors;

import opc.enums.opc.EnrolmentChannel;
import commons.enums.State;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class VerifyAuthyPushUserEnrolmentTests extends BaseAuthenticationFactorsSetup {

    @Test
    public void VerifyEnrolment_AcceptCorporate_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.ACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_RejectCorporate_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.INACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AcceptConsumer_Success() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(passcodeAppConsumerProfileId, passcodeAppSecretKey);
        enrolUser(consumer.getRight());

        SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, consumer.getLeft(), consumer.getRight(), State.ACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_RejectConsumer_Success() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(passcodeAppConsumerProfileId, passcodeAppSecretKey);
        enrolUser(consumer.getRight());

        SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, consumer.getLeft(), consumer.getRight(), State.INACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AcceptAuthenticatedUser_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey, corporate.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, user.getLeft(), user.getRight(), State.ACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_RejectAuthenticatedUser_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey, corporate.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, user.getLeft(), user.getRight(), State.INACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AcceptEnrolmentInitiatedBySms_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), passcodeAppSecretKey, corporate.getRight());

        SimulatorService.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void VerifyEnrolment_RejectEnrolmentInitiatedBySms_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), passcodeAppSecretKey, corporate.getRight());

        SimulatorService.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void VerifyEnrolment_AcceptEnrolmentInitiatedByOkay_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        SecureHelper.enrolBiometricUser(corporate.getRight(), passcodeAppSharedKey);

        SimulatorService.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void VerifyEnrolment_RejectEnrolmentInitiatedByOkay_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        SecureHelper.enrolBiometricUser(corporate.getRight(), passcodeAppSharedKey);

        SimulatorService.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void VerifyEnrolment_UserNotEnrolled_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);

        SimulatorService.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .log().all()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void VerifyEnrolment_AcceptAlreadyAccepted_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.ACTIVE);

        SimulatorService.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AcceptAlreadyRejected_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.INACTIVE);

        SimulatorService.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void VerifyEnrolment_RejectAlreadyAccepted_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.ACTIVE);

        SimulatorService.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_RejectAlreadyRejected_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.INACTIVE);

        SimulatorService.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void VerifyEnrolment_DifferentInnovatorApiKey_NotFound() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        SimulatorService.acceptAuthyIdentity(otherInnovatorSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));
    }

    @Test
    public void VerifyEnrolment_InvalidApiKey_NotFound() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorService.acceptAuthyIdentity("abc", corporate.getLeft())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifyEnrolment_NoApiKey_BadRequest() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        enrolUser(corporate.getRight());

        SimulatorService.acceptAuthyIdentity("", corporate.getLeft())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEnrolment_ExpiredChallenge() throws SQLException, InterruptedException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);

        enrolUser(corporate.getRight());

        AuthySimulatorDatabaseHelper.expireIdentityEnrolmentRequest(corporate.getLeft());
        TimeUnit.SECONDS.sleep(2);

        SimulatorService.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EXPIRED"));

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    private void enrolUser(final String token){
        AuthenticationFactorsHelper.enrolAuthyPushUser(passcodeAppSecretKey, token);
    }
}