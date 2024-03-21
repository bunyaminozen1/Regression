package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.CancelScheduledModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.VerificationModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class OutgoingWireTransfersService extends BaseService {

    public static Response sendOutgoingWireTransfer(final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                    final String secretKey,
                                                    final String token,
                                                    final Optional<String> idempotencyRef) {
        return getBodyApiKeyAuthenticationRequest(outgoingWireTransfersModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/outgoing_wire_transfers");
    }

    public static Response sendBulkOutgoingWireTransfers(final BulkOutgoingWireTransfersModel bulkOutgoingWireTransfersModel,
                                                         final String secretKey,
                                                         final String token,
                                                         final Optional<String> idempotencyRef) {
        return getBodyApiKeyAuthenticationRequest(bulkOutgoingWireTransfersModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/outgoing_wire_transfers/bulk/create");
    }

    public static Response cancelScheduledOutgoingWireTransfer(final CancelScheduledModel cancelScheduledModel,
                                                               final String secretKey,
                                                               final String token) {
        return getBodyApiKeyAuthenticationRequest(cancelScheduledModel, secretKey, token)
                .when()
                .post("/multi/outgoing_wire_transfers/bulk/cancel");
    }


    public static Response getOutgoingWireTransfers(final String secretKey,
                                                    final Optional<Map<String, Object>> filters,
                                                    final String token) {
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/outgoing_wire_transfers");
    }

    public static Response getOutgoingWireTransfer(final String secretKey,
                                                   final String outgoingWireTransferId,
                                                   final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", outgoingWireTransferId)
                .when()
                .get("/multi/outgoing_wire_transfers/{id}");
    }

    public static Response startOutgoingWireTransferOtpVerification(final String outgoingWireTransferId,
                                                                    final String channel,
                                                                    final String secretKey,
                                                                    final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", outgoingWireTransferId)
                .pathParam("channel", channel)
                .when()
                .post("/multi/outgoing_wire_transfers/{id}/challenges/otp/{channel}");
    }

    public static Response startOutgoingWireTransferPushVerification(final String outgoingWireTransferId,
                                                                     final String channel,
                                                                     final String secretKey,
                                                                     final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", outgoingWireTransferId)
                .pathParam("channel", channel)
                .when()
                .post("/multi/outgoing_wire_transfers/{id}/challenges/push/{channel}");
    }

    public static Response verifyOutgoingWireTransfer(final VerificationModel verificationModel,
                                                      final String outgoingWireTransferId,
                                                      final String channel,
                                                      final String secretKey,
                                                      final String token) {
        return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
                .pathParam("id", outgoingWireTransferId)
                .pathParam("channel", channel)
                .when()
                .post("/multi/outgoing_wire_transfers/{id}/challenges/otp/{channel}/verify");
    }
}