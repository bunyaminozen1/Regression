package fpi.paymentrun.sca;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.services.uicomponents.PaymentRunConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.helpers.ModelHelper;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.models.shared.VerificationModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
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
@Tag(PluginsTags.PAYMENT_RUN_SCA)
public class PaymentRunVerificationOtpAttemptLimitTests extends BasePaymentRunSetup {
    final private static String VERIFICATION_CODE = "123456";
    final private static String INVALID_CODE = "000000";
    final private static String CHANNEL = EnrolmentChannel.SMS.name();

    private static String buyerToken;
    private static String userToken;

    @BeforeAll
    public static void IdentitySetup() {
        buyerToken = createBuyer();
        userToken = createAuthUser();
    }

    @Test
    public void VerifyIssueScaPaymentRunBuyer_AttemptLimitFive_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, null);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 3)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyIssueScaPaymentRunBuyer_ValidCodeAtLastAttempt_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 5);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 3)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyIssueScaPaymentRunBuyer_AttemptLimitThree_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 3);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyScaMa, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    public void VerifyIssueScaPaymentRunBuyer_AttemptLimitOutOfDefault_ChallengeLimitExceeded(final int limitNumber) throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, limitNumber);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 3)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyIssueScaPaymentRunBuyer_NoAttemptLimitForProgramme_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 0);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 7)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        //It is successfully stepped up when user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyIssueScaPaymentRunAuthUser_AttemptLimitFive_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, null);

        final String paymentRunId = createPaymentRun(userToken);

        AuthenticationHelper.startIssueScaPaymentRun(userToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 3)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyIssueScaPaymentRunAuthUser_ValidCodeAtLastAttempt_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 5);

        final String paymentRunId = createPaymentRun(userToken);

        AuthenticationHelper.startIssueScaPaymentRun(userToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 3)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyIssueScaPaymentRunAuthUser_AttemptLimitThree_ChallengeLimitExceeded() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, 3);

        final String paymentRunId = createPaymentRun(userToken);

        AuthenticationHelper.startIssueScaPaymentRun(userToken, sharedKeyScaMa, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    public void VerifyIssueScaPaymentRunAuthUser_AttemptLimitOutOfDefault_ChallengeLimitExceeded(final int limitNumber) throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(programmeIdScaMa, limitNumber);

        final String paymentRunId = createPaymentRun(userToken);

        AuthenticationHelper.startIssueScaPaymentRun(userToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 3)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyIssueScaPaymentRunAuthUser_NoAttemptLimitForProgramme_Success() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(programmeIdScaMa, 0);

        final String paymentRunId = createPaymentRun(userToken);

        AuthenticationHelper.startIssueScaPaymentRun(userToken, sharedKeyScaMa, paymentRunId);

        IntStream.range(0, 7)
                .forEach(i -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(INVALID_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        //It is successfully stepped up when user enters the correct verification code
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), userToken, sharedKeyScaMa, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String createPaymentRun(final String buyerToken) {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        return PaymentRunsHelper.createConfirmedPaymentRun(createPaymentRunModel, secretKeyScaMa, buyerToken);
    }

    private static String createBuyer() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyScaMa);
        BuyersHelper.assignAllRoles(secretKeyScaMa, authenticatedBuyer.getRight());
        return authenticatedBuyer.getRight();
    }

    private static String createAuthUser() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyScaMa, buyerToken);
        BuyerAuthorisedUserHelper.assignAllRoles(authUser.getLeft(), secretKeyScaMa, buyerToken);
        return authUser.getRight();
    }
}
