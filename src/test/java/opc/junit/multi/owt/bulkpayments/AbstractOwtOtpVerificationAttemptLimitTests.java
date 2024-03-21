package opc.junit.multi.owt.bulkpayments;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import opc.enums.opc.ResourceType;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.OwtModelHelper;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.services.multi.ChallengesService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
@Tag(MultiTags.BULK_OWT)
public abstract class AbstractOwtOtpVerificationAttemptLimitTests extends BaseOutgoingWireTransfersSetup {

    protected abstract String getToken();

    protected abstract String getCurrency();

    protected abstract String getManagedAccountProfileId();

    protected abstract String getDestinationIdentityName();

    protected abstract IdentityType getIdentityType();
    final private static String VERIFICATION_CODE = "123456";
    final private static String INVALID_CODE = "000000";

    @Test
    public void VerifyTransfer_WrongVerificationCodeAttemptLimitFive_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 5);

        final String scaChallengeId = issueOtpChallenges();

        IntStream.range(0, 3)
                .forEach(i -> verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        verifyOtpChallenges(scaChallengeId, VERIFICATION_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifyTransfer_WrongVerificationCodeValidCodeAtLastAttempt_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 5);

        final String scaChallengeId = issueOtpChallenges();

        IntStream.range(0, 3)
                .forEach(i -> verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        verifyOtpChallenges(scaChallengeId, VERIFICATION_CODE)
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyTransfer_WrongVerificationCodeAttemptLimitThree_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 3);

        final String scaChallengeId = issueOtpChallenges();

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        verifyOtpChallenges(scaChallengeId, VERIFICATION_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 6})
    public void VerifyTransfer_WrongVerificationCodeAttemptLimitOutOfDefault_Success(final int limitNumber) throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), limitNumber);
        final String scaChallengeId = issueOtpChallenges();

        IntStream.range(0, 3)
                .forEach(i -> verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        verifyOtpChallenges(scaChallengeId, INVALID_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        verifyOtpChallenges(scaChallengeId, VERIFICATION_CODE)
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    private String issueOtpChallenges(){
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), secondaryScaApp.getSecretKey()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
                OwtModelHelper
                        .createOwtBulkPayments(2, secondaryScaApp.getOwtProfileId(), sourceManagedAccountId,
                                getCurrency(), 100L, OwtType.SEPA), secondaryScaApp.getSecretKey(), getToken(), Optional.empty());
        response.then().statusCode(SC_OK);

        List<String> owts = response.jsonPath().getList("response.id");

        return ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
                        .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
                EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), getToken());
    }
    private ValidatableResponse verifyOtpChallenges(final String scaChallengeId, final String verificationCode){
        return ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, verificationCode),
                scaChallengeId, EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), getToken()).then();
    }
}
