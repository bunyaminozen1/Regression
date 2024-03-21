package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.OutgoingWireTransfersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractOwtVerificationOtpAttemptLimitTests extends BaseOutgoingWireTransfersSetup{

    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";
    private static final String INVALID_CODE = "000000";
    protected abstract String getIdentityToken();
    protected abstract String getManagedAccountId();
    protected abstract Pair<String, String> getNonLimitIdentity();

    @Test
    public void VerifyTransfer_CorporateAttemptLimitFive_ChallengeLimitExceeded() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), null);

        final String id = sendOutgoingWireTransfer(getManagedAccountId(), getIdentityToken());
        startVerification(id, getIdentityToken());

        IntStream.range(0, 3)
                .forEach(i -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())                            .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }
    @Test
    public void VerifyTransfer_CorporateAttemptLimitThree_ChallengeLimitExceeded() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 3);

        final String id = sendOutgoingWireTransfer(getManagedAccountId(), getIdentityToken());
        startVerification(id, getIdentityToken());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 6})
    public void VerifyTransfer_CorporateAttemptLimitOutOfDefault_ChallengeLimitExceeded(final int limitNumber) throws SQLException {

        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), limitNumber);

        final String id = sendOutgoingWireTransfer(getManagedAccountId(), getIdentityToken());
        startVerification(id, getIdentityToken());

        IntStream.range(0, 3)
                .forEach(i -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), id, CHANNEL, secondaryScaApp.getSecretKey(), getIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }
    @Test
    public void VerifyTransfer_CorporateNoAttemptLimitForProgramme_Success() {

        final Pair<String, String> identity = getNonLimitIdentity();

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(identity.getLeft(), CHANNEL, passcodeAppSecretKey, identity.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        IntStream.range(0, 7)
                .forEach(i -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(INVALID_CODE), identity.getLeft(), CHANNEL, passcodeAppSecretKey, identity.getRight())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        //It is successfully stepped up when user enters the correct verification code
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), identity.getLeft(), CHANNEL, passcodeAppSecretKey, identity.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String sendOutgoingWireTransfer(final String managedAccountId,
                                            final String token){

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(secondaryScaApp.getOwtProfileId(),
                        managedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();

        return OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, secondaryScaApp.getSecretKey(), token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private void startVerification(final String id,
                                   final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secondaryScaApp.getSecretKey(), token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
    @AfterAll
    public static void resetConfiguration() throws SQLException {
        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 0);
    }
}
