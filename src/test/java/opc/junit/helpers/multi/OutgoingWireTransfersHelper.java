package opc.junit.helpers.multi;

import commons.enums.State;
import io.restassured.response.Response;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.helpers.OwtModelHelper;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.outgoingwiretransfers.CancelScheduledModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransferResponseModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersResponseModel;
import opc.models.shared.CancellationModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.VerificationModel;
import opc.services.multi.OutgoingWireTransfersService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class OutgoingWireTransfersHelper {

    public static Pair<String, OutgoingWireTransfersModel> sendOwt(final String outgoingWireTransfersProfileId,
                                                                   final String managedAccountId,
                                                                   final CurrencyAmount owtAmount,
                                                                   final String secretKey,
                                                                   final String authenticationToken) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        owtAmount.getCurrency(), owtAmount.getAmount(), OwtType.SEPA).build();

        return sendOwt(outgoingWireTransfersModel, secretKey, authenticationToken);
    }

    public static Pair<String, OutgoingWireTransfersModel> sendOwt(final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                                   final String secretKey,
                                                                   final String authenticationToken) {

        final String owtId =
                TestHelper.ensureAsExpected(15,
                                () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        return Pair.of(owtId, outgoingWireTransfersModel);
    }

    public static String sendScheduledOwt(final String timestamp,
                                          final String outgoingWireTransfersProfileId,
                                          final String managedAccountId,
                                          final String currency,
                                          final Long amount,
                                          final String secretKey,
                                          final String authenticationToken) {


        return TestHelper.ensureAsExpected(15,
                    () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(OwtModelHelper.createOwtScheduledPayment(timestamp,
                        outgoingWireTransfersProfileId, managedAccountId, currency, amount, OwtType.SEPA), secretKey, authenticationToken, Optional.empty()),
                    SC_OK)
                .jsonPath()
                .get("id");
    }

    public static Pair<String, OutgoingWireTransfersModel> sendOwtFasterPayments(final String outgoingWireTransfersProfileId,
                                                                   final String managedAccountId,
                                                                   final CurrencyAmount owtAmount,
                                                                   final String secretKey,
                                                                   final String authenticationToken) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        owtAmount.getCurrency(), owtAmount.getAmount(), OwtType.FASTER_PAYMENTS).build();

        final String owtId =
                TestHelper.ensureAsExpected(15,
                                () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        return Pair.of(owtId, outgoingWireTransfersModel);
    }

    public static Pair<String, OutgoingWireTransfersModel> sendSuccessfulOwtOtp(final String outgoingWireTransfersProfileId,
                                                                                final String managedAccountId,
                                                                                final CurrencyAmount owtAmount,
                                                                                final String secretKey,
                                                                                final String authenticationToken) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        owtAmount.getCurrency(), owtAmount.getAmount(), OwtType.SEPA).build();

        return sendSuccessfulOwtOtp(outgoingWireTransfersModel, secretKey, authenticationToken);
    }

    public static Pair<String, OutgoingWireTransfersModel> sendSuccessfulOwtOtp(final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                                                final String secretKey,
                                                                                final String authenticationToken) {

        final String owtId =
                TestHelper.ensureAsExpected(15,
                                () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        verifyOwtOtp(owtId, secretKey, authenticationToken);

        checkOwtStateById(owtId, "COMPLETED");

        return Pair.of(owtId, outgoingWireTransfersModel);
    }

    public static Pair<String, OutgoingWireTransfersModel> sendSuccessfulOwtPush(final EnrolmentChannel enrolmentChannel,
                                                                                 final String outgoingWireTransfersProfileId,
                                                                                 final String managedAccountId,
                                                                                 final CurrencyAmount owtAmount,
                                                                                 final String secretKey,
                                                                                 final String authenticationToken) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        owtAmount.getCurrency(), owtAmount.getAmount(), OwtType.SEPA).build();

        final String owtId =
                TestHelper.ensureAsExpected(15,
                                () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        verifyOwtPush(owtId, enrolmentChannel, secretKey, authenticationToken);

        checkOwtStateById(owtId, "COMPLETED");

        return Pair.of(owtId, outgoingWireTransfersModel);
    }

    public static Pair<String, OutgoingWireTransfersModel> sendRejectedOwtPush(final EnrolmentChannel enrolmentChannel,
                                                                               final String outgoingWireTransfersProfileId,
                                                                               final String managedAccountId,
                                                                               final CurrencyAmount owtAmount,
                                                                               final String secretKey,
                                                                               final String authenticationToken) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        owtAmount.getCurrency(), owtAmount.getAmount(), OwtType.SEPA).build();

        final String owtId =
                TestHelper.ensureAsExpected(15,
                        () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                        .jsonPath()
                        .get("id");

        rejectOwtPush(owtId, enrolmentChannel, secretKey, authenticationToken);

        checkOwtStateById(owtId, "DECLINED_SCA");

        return Pair.of(owtId, outgoingWireTransfersModel);
    }

    public static Pair<String, OutgoingWireTransfersModel> sendOwtAndStartOtpChallenge(final String outgoingWireTransfersProfileId,
                                                                                       final String managedAccountId,
                                                                                       final CurrencyAmount owtAmount,
                                                                                       final String secretKey,
                                                                                       final String authenticationToken) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        owtAmount.getCurrency(), owtAmount.getAmount(), OwtType.SEPA).build();

        final String owtId =
                TestHelper.ensureAsExpected(15,
                                () -> OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, EnrolmentChannel.SMS.name(), secretKey, authenticationToken),
                SC_NO_CONTENT);

        return Pair.of(owtId, outgoingWireTransfersModel);
    }

    public static void verifyOwtOtp(final String outgoingWireTransferId,
                                    final String secretKey,
                                    final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(outgoingWireTransferId, EnrolmentChannel.SMS.name(), secretKey, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(TestHelper.OTP_VERIFICATION_CODE), outgoingWireTransferId,
                        EnrolmentChannel.SMS.name(), secretKey, token),
                SC_NO_CONTENT);
    }

    public static void verifyOwtPush(final String outgoingWireTransferId,
                                     final EnrolmentChannel enrolmentChannel,
                                     final String secretKey,
                                     final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(outgoingWireTransferId, enrolmentChannel.name(),
                        secretKey, token),
                SC_NO_CONTENT);

        if (enrolmentChannel.equals(EnrolmentChannel.AUTHY)) {
            SimulatorHelper.acceptAuthyOwt(secretKey, outgoingWireTransferId);
        } else {
            SimulatorHelper.acceptOkayOwt(secretKey, outgoingWireTransferId);
        }
    }

    public static void rejectOwtPush(final String outgoingWireTransferId,
                                     final EnrolmentChannel enrolmentChannel,
                                     final String secretKey,
                                     final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(outgoingWireTransferId, enrolmentChannel.name(),
                        secretKey, token),
                SC_NO_CONTENT);

        if (enrolmentChannel.equals(EnrolmentChannel.AUTHY)) {
            SimulatorHelper.rejectAuthyOwt(secretKey, outgoingWireTransferId);
        } else {
            SimulatorHelper.rejectOkayOwt(secretKey, outgoingWireTransferId);
        }
    }

    public static Response getOutgoingWireTransfers(final String secretKey,
                                                    final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), authenticationToken),
                SC_OK);
    }

    public static void ensureOwtState(final String secretKey,
                                      final String owtId,
                                      final String token,
                                      final State state) {
        TestHelper.ensureAsExpected(120,
            () -> OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, owtId, token),
            x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
            Optional.of(String.format("Expecting 200 with a owt in state %s, check logged payload", state)));
    }

    public static OutgoingWireTransferResponseModel getOwtByAccountAndTag(final String secretKey,
                                                                          final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                                          final String token) {
        return OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), token)
                .as(OutgoingWireTransfersResponseModel.class).getTransfer().stream().filter(x -> x.getSourceInstrument().getId().equals(outgoingWireTransfersModel.getSourceInstrument().getId()) && x.getTag().equals(outgoingWireTransfersModel.getTag())).findFirst().orElseThrow();
    }

    public static void getOutgoingWireTransfersForbidden(final String secretKey,
                                                             final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), authenticationToken),
                SC_FORBIDDEN);
    }

    public static Response getOutgoingWireTransfer(final String secretKey,
                                                   final String outgoingWireTransferId,
                                                   final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, outgoingWireTransferId, authenticationToken),
                SC_OK);
    }

    public static void getOutgoingWireTransferForbidden(final String secretKey,
                                                   final String outgoingWireTransferId,
                                                   final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, outgoingWireTransferId, authenticationToken),
                SC_FORBIDDEN);
    }

    public static void checkOwtStateById(final String owtId, final String state) {
        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> OwtDatabaseHelper.getOwtById(owtId),
                x -> x.size() > 0 && x.get(0).get("state").equals(state),
                Optional.of(String.format("OWT with id %s not in state %s as expected", owtId, state)));
    }

    public static void checkOwtStateByAccountId(final String accountId, final String state) {
        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> OwtDatabaseHelper.getOwt(accountId),
                x -> x.size() > 0 && x.get(0).get("state").equals(state),
                Optional.of(String.format("OWT with id %s not in state %s as expected", accountId, state)));
    }

    public static void checkOwtValidationFailureById(final String owtId, final String validationFailure) {
        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> OwtDatabaseHelper.getOwtById(owtId),
                x -> x.size() > 0 && x.get(0).get("validation_failure").equals(validationFailure),
                Optional.of(String.format("OWT with id %s not in state %s as expected", owtId, validationFailure)));
    }

    public static void cancelScheduledTransfer(final String secretKey,
                                               final String owtId,
                                               final String token) {
        TestHelper.ensureAsExpected(10,
                () -> OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owtId).cancellationReason("Cancellation reason").build()))
                        .build(), secretKey, token),
                SC_OK);
    }
}
