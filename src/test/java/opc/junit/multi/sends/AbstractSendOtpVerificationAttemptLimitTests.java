package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.VerificationModel;
import opc.services.multi.SendsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractSendOtpVerificationAttemptLimitTests extends BaseSendsSetup{
    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String CURRENCY = Currency.EUR.name();
    private static final String VERIFICATION_CODE = "123456";
    final private static String INVALID_CODE = "000000";

    protected abstract String getSourceIdentityToken();
    protected abstract String getSourceManagedAccount();
    protected abstract String getDestinationManagedCard();


    @Test
    public void VerifySend_WrongVerificationCodeAttemptLimitFive_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 5);

        final String sendId = identityDepositAndSendMaToMc(getSourceIdentityToken(), getSourceManagedAccount(),
                getDestinationManagedCard()).get(0).getLeft();

        startVerification(sendId, getSourceIdentityToken());

        IntStream.range(0, 3)
                .forEach(i -> SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifySend_WrongVerificationCodeValidCodeAtLastAttempt_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 5);

        final String sendId = identityDepositAndSendMaToMc(getSourceIdentityToken(), getSourceManagedAccount(),
                getDestinationManagedCard()).get(0).getLeft();

        startVerification(sendId, getSourceIdentityToken());

        IntStream.range(0, 3)
                .forEach(i -> SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifySend_WrongVerificationCodeAttemptLimitThree_Success() throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), 3);

        final String sendId = identityDepositAndSendMaToMc(getSourceIdentityToken(), getSourceManagedAccount(),
                getDestinationManagedCard()).get(0).getLeft();
        startVerification(sendId, getSourceIdentityToken());

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 6})
    public void VerifySend_WrongVerificationCodeAttemptLimitOutOfDefault_Success(final int limitNumber) throws SQLException {

        ProgrammeDatabaseHelper.switchOtpVerifyLimitFunction(secondaryScaApp.getProgrammeId(), 1);
        ProgrammeDatabaseHelper.updateOtpVerifyLimitNumber(secondaryScaApp.getProgrammeId(), limitNumber);

        final String sendId = identityDepositAndSendMaToMc(getSourceIdentityToken(), getSourceManagedAccount(),
                getDestinationManagedCard()).get(0).getLeft();
        startVerification(sendId, getSourceIdentityToken());

        IntStream.range(0, 3)
                .forEach(i -> SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                        .then()
                        .statusCode(SC_CONFLICT)
                        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID")));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ONE_CHALLENGE_LIMIT_REMAINING"));

        SendsService.verifySendOtp(new VerificationModel(INVALID_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        //It returns STATE_INVALID even if user enters the correct verification code
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId, CHANNEL, secondaryScaApp.getSecretKey(), getSourceIdentityToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    private static List<Pair<String, SendFundsModel>> identityDepositAndSendMaToMc(final String token,
                                                                                   final String identityManagedAccountId,
                                                                                   final String identityManagedCardId) {
        final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

        fundManagedAccount(identityManagedAccountId, CURRENCY, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(secondaryScaApp.getSendProfileId())
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(CURRENCY, 100L))
                            .setSource(new ManagedInstrumentTypeId(identityManagedAccountId, MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(identityManagedCardId, MANAGED_CARDS))
                            .build();

            final String id =
                    SendsService.sendFunds(sendFundsModel, secondaryScaApp.getSecretKey(), token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            identitySendFunds.add(Pair.of(id, sendFundsModel));
        });
        return identitySendFunds;
    }

    private void startVerification(final String id,
                                   final String token) {

        SendsService.startSendOtpVerification(id, CHANNEL, secondaryScaApp.getSecretKey(), token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
