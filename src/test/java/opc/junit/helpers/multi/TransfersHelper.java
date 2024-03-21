package opc.junit.helpers.multi;

import io.restassured.response.Response;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.models.multi.transfers.CancelScheduledModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CancellationModel;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;

public class TransfersHelper {
    public static Response getTransfers(final String secretKey,
                                        final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> TransfersService.getTransfers(secretKey, Optional.empty(), authenticationToken),
                SC_OK);
    }

    public static Response getTransfersForbidden(final String secretKey,
                                                 final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> TransfersService.getTransfers(secretKey, Optional.empty(), authenticationToken),
                SC_FORBIDDEN);
    }

    public static Response getTransfer(final String secretKey,
                                       final String transactionId,
                                       final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> TransfersService.getTransfer(secretKey, transactionId, authenticationToken),
                SC_OK);
    }

    public static Response getTransferForbidden(final String secretKey,
                                                final String transactionId,
                                                final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> TransfersService.getTransfer(secretKey, transactionId, authenticationToken),
                SC_FORBIDDEN);
    }

    public static String sendScheduledTransfer(final String timestamp,
                                               final String transfersProfileId,
                                               final String currency,
                                               final Long amount,
                                               final String secretKey,
                                               final String authenticationToken,
                                               final Pair<String, ManagedInstrumentType> sourceInstrument,
                                               final Pair<String, ManagedInstrumentType> destinationInstrument) {

        final TransferFundsModel transferFundsModel = TransferFundsModel
                .DefaultTransfersModel(transfersProfileId, currency, amount, sourceInstrument, destinationInstrument)
                .setScheduledTimestamp(timestamp)
                .build();


        return TestHelper.ensureAsExpected(15,
                        () -> TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                .jsonPath()
                .get("id");
    }

    public static String sendTransfer(final String transfersProfileId,
                                      final String currency,
                                      final Long amount,
                                      final String secretKey,
                                      final String authenticationToken,
                                      final Pair<String, ManagedInstrumentType> sourceInstrument,
                                      final Pair<String, ManagedInstrumentType> destinationInstrument) {

        final TransferFundsModel transferFundsModel = TransferFundsModel
                .DefaultTransfersModel(transfersProfileId, currency, amount, sourceInstrument, destinationInstrument)
                .build();


        return TestHelper.ensureAsExpected(15,
                        () -> TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                .jsonPath()
                .get("id");
    }

    public static void ensureTransferState(final String secretKey,
                                           final String transferId,
                                           final String token,
                                           final String state) {
        TestHelper.ensureAsExpected(120,
                () -> TransfersService.getTransfer(secretKey, transferId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state),
                Optional.of(String.format("Expecting 200 with a transfer %s in state %s, check logged payload", transferId, state)));
    }

    public static void cancelScheduledTransfer(final String secretKey,
                                               final String transferId,
                                               final String token) {
        TestHelper.ensureAsExpected(10,
                () -> TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(transferId).cancellationReason("Cancellation reason").build()))
                        .build(), secretKey, token),
                SC_OK);
    }
}
