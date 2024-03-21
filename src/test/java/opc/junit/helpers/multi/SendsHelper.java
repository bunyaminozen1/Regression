package opc.junit.helpers.multi;

import com.github.javafaker.Faker;
import commons.enums.State;
import io.restassured.response.Response;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.ManagedInstrumentType;
import opc.helpers.SendModelHelper;
import opc.junit.database.SendsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.sends.CancelScheduledModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CancellationModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.VerificationModel;
import opc.services.multi.SendsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class SendsHelper {

    public static Pair<String, SendFundsModel> sendFundsSuccessfulOtpVerified(final String sendsProfileId,
                                                                              final CurrencyAmount sendAmount,
                                                                              final String sourceManagedAccountId,
                                                                              final String destinationManagedAccountId,
                                                                              final String secretKey,
                                                                              final String authenticationToken) {
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(sendAmount.getCurrency(), sendAmount.getAmount()))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .build();

        String sendId =
                TestHelper.ensureAsExpected(15,
                                () -> SendsService.sendFunds(sendFundsModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        verifySendOtp(sendId, secretKey, authenticationToken);

        TestHelper.ensureAsExpected(120,
                () -> SendsService.getSend(secretKey, sendId, authenticationToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals("COMPLETED"),
                Optional.of("Expecting 200 with a send in state COMPLETED, check logged payload"));

        return Pair.of(sendId, sendFundsModel);
    }

    public static Pair<String, SendFundsModel> sendFundsAndStartOtpChallenge(final String sendsProfileId,
                                                                             final CurrencyAmount sendAmount,
                                                                             final String sourceManagedAccountId,
                                                                             final String destinationManagedAccountId,
                                                                             final String secretKey,
                                                                             final String authenticationToken) {
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(sendAmount.getCurrency(), sendAmount.getAmount()))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .build();

        String sendId =
                TestHelper.ensureAsExpected(15,
                                () -> SendsService.sendFunds(sendFundsModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        TestHelper.ensureAsExpected(15,
                () -> SendsService.startSendOtpVerification(sendId, EnrolmentChannel.SMS.name(), secretKey, authenticationToken),
                SC_NO_CONTENT);

        return Pair.of(sendId, sendFundsModel);
    }

    public static void verifySendOtp(final String sendId,
                                     final String secretKey,
                                     final String token) {
        TestHelper.ensureAsExpected(15,
                () -> SendsService.startSendOtpVerification(sendId, EnrolmentChannel.SMS.name(), secretKey, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> SendsService.verifySendOtp(new VerificationModel(TestHelper.OTP_VERIFICATION_CODE), sendId,
                        EnrolmentChannel.SMS.name(), secretKey, token),
                SC_NO_CONTENT);
    }

    public static Response getSend(final String secretKey,
                                   final String transactionId,
                                   final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SendsService.getSend(secretKey, transactionId, authenticationToken),
                SC_OK);
    }

    public static Response getSendForbidden(final String secretKey,
                                            final String transactionId,
                                            final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SendsService.getSend(secretKey, transactionId, authenticationToken),
                SC_FORBIDDEN);
    }

    public static Response getSends(final String secretKey,
                                    final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SendsService.getSends(secretKey, Optional.empty(), authenticationToken),
                SC_OK);
    }

    public static Response getSendsForbidden(final String secretKey,
                                             final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SendsService.getSends(secretKey, Optional.empty(), authenticationToken),
                SC_FORBIDDEN);
    }

    public static void ensureSendState(final String secretKey,
                                       final String sendId,
                                       final String token,
                                       final State state) {
        TestHelper.ensureAsExpected(120,
            () -> SendsService.getSend(secretKey, sendId, token),
            x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
            Optional.of(String.format("Expecting 200 with a send in state %s, check logged payload", state)));
    }

    public static void startSendPushVerification(final String sendId,
                                                 final String channel,
                                                 final String secretKey,
                                                 final String token) {
        TestHelper.ensureAsExpected(15,
                () -> SendsService.startSendPushVerification(sendId, channel, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void checkSendStateById(final String sendId, final String state) {
        TestHelper.ensureDatabaseResultAsExpected(120,
            () -> SendsDatabaseHelper.getSendById(sendId),
            x -> x.size() > 0 && x.get(0).get("state").equals(state),
            Optional.of(String.format("Send with id %s not in state %s as expected", sendId, state)));
    }

    public static String sendScheduledSend(final String timestamp,
                                           final String profileId,
                                           final ManagedInstrumentType sourceInstrumentType,
                                           final String sourceInstrumentId,
                                           final ManagedInstrumentType destinationInstrumentType,
                                           final String destinationInstrumentId,
                                           final String currency,
                                           final Long amount,
                                           final String secretKey,
                                           final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> SendsService.sendFunds(SendModelHelper.createSendScheduledPayment(timestamp,
                    profileId, sourceInstrumentType, sourceInstrumentId, destinationInstrumentType,
                    destinationInstrumentId, currency, amount), secretKey, token, Optional.empty()),
                SC_OK)
            .jsonPath()
            .get("id");
    }

    public static void issueAndVerifyPushChallenge(final String sendId,
                                                   final EnrolmentChannel enrolmentChannel,
                                                   final String secretKey,
                                                   final String token) {

        startSendPushVerification(sendId, enrolmentChannel.name(), secretKey, token);

        if (enrolmentChannel.equals(EnrolmentChannel.AUTHY)) {
            SimulatorHelper.acceptAuthySend(secretKey, sendId);
        } else {
            SimulatorHelper.acceptOkaySend(secretKey, sendId);
        }
    }

    public static Pair<String, SendFundsModel> sendFundsToCardSuccessfulOtpVerified(final String sendsProfileId,
                                                                                    final CurrencyAmount sendAmount,
                                                                                    final String sourceManagedAccountId,
                                                                                    final String destinationManagedCardId,
                                                                                    final String secretKey,
                                                                                    final String authenticationToken) {

        final Faker faker = new Faker();

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setDescription(faker.dog().name())
                        .setTag(faker.dog().name())
                        .setDestinationAmount(new CurrencyAmount(sendAmount.getCurrency(), sendAmount.getAmount()))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedCardId, MANAGED_CARDS))
                        .build();

        String sendId =
                TestHelper.ensureAsExpected(15,
                                () -> SendsService.sendFunds(sendFundsModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        verifySendOtp(sendId, secretKey, authenticationToken);

        TestHelper.ensureAsExpected(120,
                () -> SendsService.getSend(secretKey, sendId, authenticationToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals("COMPLETED"),
                Optional.of("Expecting 200 with a send in state COMPLETED, check logged payload"));

        return Pair.of(sendId, sendFundsModel);
    }

    public static void cancelScheduledTransfer(final String secretKey,
                                               final String sendId,
                                               final String token) {
        TestHelper.ensureAsExpected(10,
                () -> SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(sendId).cancellationReason("Cancellation reason").build()))
                        .build(), secretKey, token),
                SC_OK);
    }
}
