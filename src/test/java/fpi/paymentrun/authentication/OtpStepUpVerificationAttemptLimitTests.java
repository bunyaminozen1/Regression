package fpi.paymentrun.authentication;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.services.AuthenticationService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.models.shared.VerificationModel;
import opc.tags.PluginsTags;
import org.junit.jupiter.api.Tag;
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
@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION)
public class OtpStepUpVerificationAttemptLimitTests extends BasePaymentRunSetup {
    final private static String VERIFICATION_CODE = "123456";
    final private static String INVALID_CODE = "000000";

    @Test
    public void VerifyStepUpTokenBuyer_AttemptLimitFive_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, null);

        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKeyScaMa).getRight();

        AuthenticationHelper.startStepup(secretKeyScaMa, buyerToken);

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyStepUpTokenBuyer_ValidCodeAtLastAttempt_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 5);

        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKeyScaMa).getRight();

        AuthenticationHelper.startStepup(secretKeyScaMa, buyerToken);

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpTokenBuyer_AttemptLimitThree_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 3);

        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKeyScaMa).getRight();

        AuthenticationHelper.startStepup(secretKeyScaMa, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    public void VerifyStepUpTokenBuyer_AttemptLimitOutOfDefault_ChallengeLimitExceeded(final int limitNumber) throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, limitNumber);

        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKeyScaMa).getRight();

        AuthenticationHelper.startStepup(secretKeyScaMa, buyerToken);

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyStepUpTokenBuyer_NoAttemptLimitForProgramme_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 0);

        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKeyScaMa).getRight();

        AuthenticationHelper.startStepup(secretKeyScaMa, buyerToken);

        IntStream.range(0, 7)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        //It is successfully stepped up when user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpTokenAuthUser_AttemptLimitFive_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, null);

        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyScaMa).getRight();
        final String userToken = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyScaMa, buyerToken).getRight();
        AuthenticationHelper.startStepup(secretKeyScaMa, userToken);

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyStepUpTokenAuthUser_ValidCodeAtLastAttempt_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 5);

        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyScaMa).getRight();
        final String userToken = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyScaMa, buyerToken).getRight();
        AuthenticationHelper.startStepup(secretKeyScaMa, userToken);

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpTokenAuthUser_AttemptLimitThree_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 3);

        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyScaMa).getRight();
        final String userToken = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyScaMa, buyerToken).getRight();
        AuthenticationHelper.startStepup(secretKeyScaMa, userToken);

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    public void VerifyStepUpTokenAuthUser_AttemptLimitOutOfDefault_ChallengeLimitExceeded(final int limitNumber) throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, limitNumber);

        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyScaMa).getRight();
        final String userToken = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyScaMa, buyerToken).getRight();
        AuthenticationHelper.startStepup(secretKeyScaMa, userToken);

        IntStream.range(0, 3)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyStepUpTokenAuthUser_NoAttemptLimitForProgramme_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 0);

        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyScaMa).getRight();
        final String userToken = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyScaMa, buyerToken).getRight();
        AuthenticationHelper.startStepup(secretKeyScaMa, userToken);

        IntStream.range(0, 7)
                .forEach(i -> AuthenticationService.verifyStepup(new VerificationModel(INVALID_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        //It is successfully stepped up when user enters the correct verification code
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyScaMa, userToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
