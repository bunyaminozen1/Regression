package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.transfers.CancelScheduledModel;
import opc.models.multi.transfers.TransferFundsModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class TransfersService extends BaseService {

    public static Response transferFunds(final TransferFundsModel transferFundsModel,
                                         final String secretKey,
                                         final String token,
                                         final Optional<String> idempotencyRef) {
        return getBodyApiKeyAuthenticationRequest(transferFundsModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/transfers");
    }

    public static Response getTransfers(final String secretKey,
                                        final Optional<Map<String, Object>> filters,
                                        final String token) {
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/transfers");
    }

    public static Response getTransfer(final String secretKey,
                                       final String transactionId,
                                       final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", transactionId)
                .when()
                .get("/multi/transfers/{id}");
    }

    public static Response cancelScheduledTransferTransactions(final CancelScheduledModel cancelScheduledModel,
                                                               final String secretKey,
                                                               final String token) {
        return getBodyApiKeyAuthenticationRequest(cancelScheduledModel, secretKey, token)
                .when()
                .post("/multi/transfers/bulk/cancel");
    }

}
