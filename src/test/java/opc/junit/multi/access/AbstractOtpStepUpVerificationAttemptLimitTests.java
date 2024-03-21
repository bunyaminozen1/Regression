package opc.junit.multi.access;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.shared.VerificationModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.multi.AuthenticationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractOtpStepUpVerificationAttemptLimitTests extends BaseAuthenticationSetup{

    final private static String VERIFICATION_CODE = "123456";
    final private static String INVALID_CODE = "000000";
    protected abstract IdentityDetails getEnrolledIdentity(final ProgrammeDetailsModel programme);

    @Test
    public void VerifyStepUpToken_AttemptLimitFive_ChallengeLimitExceeded() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), null);

        final IdentityDetails identity = getEnrolledIdentity(secondaryScaApp);
        startSuccessfulStepup(identity.getToken());

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyStepUpToken_ValidCodeAtLastAttempt_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 5);

        final IdentityDetails identity = getEnrolledIdentity(secondaryScaApp);
        startSuccessfulStepup(identity.getToken());

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpToken_AttemptLimitThree_ChallengeLimitExceeded() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 3);
        final IdentityDetails identity = getEnrolledIdentity(secondaryScaApp);

        startSuccessfulStepup(identity.getToken());

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 6})
    public void VerifyStepUpToken_AttemptLimitOutOfDefault_ChallengeLimitExceeded(final int limitNumber) throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), limitNumber);
        final IdentityDetails identity = getEnrolledIdentity(secondaryScaApp);

        startSuccessfulStepup(identity.getToken());

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyStepUpToken_NoAttemptLimitForProgramme_Success() {

        final IdentityDetails identity = getEnrolledIdentity(passcodeApp);

        AuthenticationHelper.startStepup(EnrolmentChannel.SMS.name(), passcodeApp.getSecretKey(), identity.getToken());

        IntStream.range(0, 7)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), passcodeApp.getSecretKey(), identity.getToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        //It is successfully stepped up when user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), passcodeApp.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void startSuccessfulStepup(String token) {
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @AfterAll
    public static void resetConfiguration() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 0);
    }
}
