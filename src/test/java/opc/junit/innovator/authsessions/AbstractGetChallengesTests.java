package opc.junit.innovator.authsessions;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.admin.GetUserChallengesModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.PagingModel;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

public abstract class AbstractGetChallengesTests extends BaseGetChallengesSetup {

    public abstract String getIdentityToken();
    public abstract String getIdentityId();
    public abstract String getIdentityManagedAccountId();
    public abstract String getIdentityManagedCardId();
    public abstract String getIdentityCurrency();

    private static final String CHANNEL_SMS = EnrolmentChannel.SMS.name();
    private static final String CHANNEL_AUTHY = EnrolmentChannel.AUTHY.name();
    private static final String VERIFICATION_CODE = "123456";

    @Test
    public void GetChallenges_OwtViaOtp_Success() {

        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .transaction_id(owtId).build();

        assertOtpChallenges(InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK), owtId, State.PENDING.name(), "outgoing_wire_transfers");

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                new VerificationModel(VERIFICATION_CODE), owtId, CHANNEL_SMS, secretKey, getIdentityToken());

        assertOtpChallenges(InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK), owtId, State.COMPLETED.name(), "outgoing_wire_transfers");
    }

    @Test
    public void GetChallenges_OwtViaAuthy_Success() {

        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationPushAuthy(owtId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .transaction_id(owtId).build();

        assertAuthyChallenges(
                InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                        .then()
                        .statusCode(SC_OK), owtId, State.PENDING.name(), "outgoing_wire_transfers");

        SimulatorService.acceptAuthyOwt(secretKey, owtId);

        assertAuthyChallenges(
                InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                        .then()
                        .statusCode(SC_OK), owtId, State.COMPLETED.name(), "outgoing_wire_transfers");
    }

    @Test
    public void GetChallenges_SendViaOtp_Success() {
        final String sendId = identityDepositAndSendMaToMc(getIdentityToken(), getIdentityManagedAccountId(),
                getIdentityCurrency(), getIdentityManagedCardId()).get(0).getLeft();
        startVerificationSendViaOtpSms(sendId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .transaction_id(sendId).build();

        assertOtpChallenges(InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK), sendId, State.PENDING.name(), "send");

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId, CHANNEL_SMS,
                secretKey, getIdentityToken());

        assertOtpChallenges(InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK), sendId, State.COMPLETED.name(), "send");

    }

    @Test
    public void GetChallenges_SendViaAuthy_Success() {
        final String sendId = identityDepositAndSendMaToMc(getIdentityToken(), getIdentityManagedAccountId(),
                getIdentityCurrency(), getIdentityManagedCardId()).get(0).getLeft();
        startVerificationSendViaPushAuthy(sendId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .transaction_id(sendId).build();

        assertAuthyChallenges(
                InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                        .then()
                        .statusCode(SC_OK), sendId, State.PENDING.name(), "send");

        SimulatorService.acceptAuthySend(secretKey, sendId);

        assertAuthyChallenges(
                InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                        .then()
                        .statusCode(SC_OK), sendId, State.COMPLETED.name(), "send");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SMS_OTP", "AUTHY_PUSH"})
    public void GetChallenges_FilterResultsWithAuthMethod_Success(String authMethod) {
        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId, getIdentityToken());

        final String sendId = identityDepositAndSendMaToMc(getIdentityToken(), getIdentityManagedAccountId(),
                getIdentityCurrency(), getIdentityManagedCardId()).get(0).getLeft();
        startVerificationSendViaOtpSms(sendId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .auth_method(authMethod).build();

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0].factorType", equalTo(authMethod))
                .body("challenges[-1].factorType", equalTo(authMethod));
    }

    @ParameterizedTest
    @ValueSource(strings = {"send", "outgoing_wire_transfers", "enrolment"})
    public void GetChallenges_FilterResultsWithActivity_Success(String activity) {
        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId, getIdentityToken());

        final String sendId = identityDepositAndSendMaToMc(getIdentityToken(), getIdentityManagedAccountId(),
                getIdentityCurrency(), getIdentityManagedCardId()).get(0).getLeft();
        startVerificationSendViaOtpSms(sendId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .activity(activity).build();

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0].transactionType", equalTo(activity))
                .body("challenges[-1].transactionType", equalTo(activity));
    }

    @Test
    public void GetChallenges_FilterResultsWithChallengeStatus_Success() {
        final String challengeStatus = State.COMPLETED.name();
        final String owtId2 = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId2, getIdentityToken());

        final String sendId2 = identityDepositAndSendMaToMc(getIdentityToken(), getIdentityManagedAccountId(),
                getIdentityCurrency(), getIdentityManagedCardId()).get(0).getLeft();
        startVerificationSendViaPushAuthy(sendId2, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .challenge_status(challengeStatus).build();

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0].status", equalTo(challengeStatus));
    }

    @Test
    public void GetChallenges_FilterResultWithTransactionId_Success() {
        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .transaction_id(owtId).build();

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0].challengeId", equalTo(owtId))
                .body("challenges[0].factorType", equalTo("SMS_OTP"))
                .body("challenges[0].status", equalTo("PENDING"))
                .body("challenges[0].transactionType", equalTo("outgoing_wire_transfers"))
                .body("challenges[0].transactionId", equalTo(owtId));
    }

    @Test
    public void GetChallenges_CheckResultComingPerUser_Success() {
        Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKey,
                getIdentityToken());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, CHANNEL_SMS, secretKey,
                authenticatedUser.getRight());

        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(),
                authenticatedUser.getRight());
        startVerificationOtp_Sms(owtId, authenticatedUser.getRight());
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                new VerificationModel(VERIFICATION_CODE), owtId, CHANNEL_SMS, secretKey,
                authenticatedUser.getRight());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10)).build();

        assertOtpChallenges(
                InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, authenticatedUser.getLeft())
                        .then()
                        .statusCode(SC_OK), owtId, State.COMPLETED.name(), "outgoing_wire_transfers");

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0].challengeId", not(equalTo(owtId)))
                .body("challenges[0].transactionId", not(equalTo(owtId)));
    }

    @Test
    public void GetChallenges_InvalidChallengeStatusEmptyResponse_Success() {
        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId, getIdentityToken());
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                new VerificationModel(VERIFICATION_CODE), owtId, CHANNEL_SMS, secretKey, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10))
                .challenge_status("ACCEPTED").build();   //needs to be VERIFIED

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0]", nullValue());
    }

    @Test
    public void GetChallenges_FilterWithPaging_Success() {
        final String owtId = sendOutgoingWireTransfer(getIdentityManagedAccountId(), getIdentityToken());
        startVerificationOtp_Sms(owtId, getIdentityToken());
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                new VerificationModel(VERIFICATION_CODE), owtId, CHANNEL_SMS, secretKey, getIdentityToken());

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(1, 2)).build();

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_OK)
                .body("challenges[0]", not(nullValue()));
    }

    @Test
    public void GetChallenges_WithoutToken_Unauthorized() {
        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10)).build();
        InnovatorService.getUserChallenges(userChallengesModel, "", getIdentityId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetChallenges_WithInvalidToken_Unauthorized() {
        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10)).build();
        InnovatorService.getUserChallenges(userChallengesModel, getIdentityToken(), getIdentityId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetChallenges_WithAnotherInnovatorToken_NotFound() {
        final String innovatorToken = InnovatorHelper.loginInnovator(nonFpsInnovatorEmail, nonFpsInnovatorPassword);

        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10)).build();

        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetChallenges_WithoutUserId_MethodNotAllowed() {
        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10)).build();
        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, "")
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void GetChallenges_WithInvalidUserId_NotFound() {
        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder()
                .paging(new PagingModel(0, 10)).build();
        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken,
                        RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetChallenges_WithInvalidRequestBody_BadRequest() {
        final GetUserChallengesModel userChallengesModel = GetUserChallengesModel.builder().build();
        InnovatorService.getUserChallenges(userChallengesModel, innovatorToken, getIdentityId())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("JSON request violated validation rules"));
    }

    private String sendOutgoingWireTransfer(final String managedAccountId,
                                            final String token) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();

        return OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private void startVerificationOtp_Sms(final String id,
                                          final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL_SMS,
                        secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void startVerificationPushAuthy(final String id,
                                            final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL_AUTHY,
                        secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void startVerificationSendViaOtpSms(final String id,
                                                final String token) {

        SendsService.startSendOtpVerification(id, CHANNEL_SMS, secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void startVerificationSendViaPushAuthy(final String id,
                                                   final String token) {

        SendsService.startSendPushVerification(id, CHANNEL_AUTHY, secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void assertOtpChallenges(final ValidatableResponse response,
                                     final String id,
                                     final String status,
                                     final String transactionType) {
        response.body("challenges[0].challengeId", equalTo(id))
                .body("challenges[0].factorType", equalTo("SMS_OTP"))
                .body("challenges[0].status", equalTo(status))
                .body("challenges[0].transactionType", equalTo(transactionType))
                .body("challenges[0].transactionId", equalTo(id));
    }

    private void assertAuthyChallenges(final ValidatableResponse response,
                                       final String id,
                                       final String status,
                                       final String transactionType) {
        response.body("challenges[0].challengeId", equalTo(id))
                .body("challenges[0].factorType", equalTo("AUTHY_PUSH"))
                .body("challenges[0].status", equalTo(status))
                .body("challenges[0].transactionType", equalTo(transactionType))
                .body("challenges[0].transactionId", equalTo(id));
    }

    private static List<Pair<String, SendFundsModel>> identityDepositAndSendMaToMc(final String token,
                                                                                   final String identityManagedAccountId,
                                                                                   final String identityCurrency,
                                                                                   final String identityManagedCardId) {
        final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

        fundManagedAccount(identityManagedAccountId, identityCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(identityCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(identityManagedAccountId, MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(identityManagedCardId, MANAGED_CARDS))
                            .build();

            final String id =
                    SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            identitySendFunds.add(Pair.of(id, sendFundsModel));
        });
        return identitySendFunds;
    }
}
