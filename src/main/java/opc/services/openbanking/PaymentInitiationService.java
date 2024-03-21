package opc.services.openbanking;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.models.openbanking.CreateOutgoingWireTransferModel;
import commons.services.BaseService;

import java.util.Map;

public class PaymentInitiationService extends BaseService {

    public static RequestSpecification getOpenBankingRequest(final String sharedKey) {
        return getRequest()
                .header("programme-key", sharedKey);
    }

    public static Response createOutgoingWireTransfer(final String sharedKey,
                                                      final Map<String, String> headers,
                                                      final CreateOutgoingWireTransferModel createOutgoingWireTransferModel) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .body(createOutgoingWireTransferModel)
                .when()
                .post("/openbanking/payment_initiation/outgoing_wire_transfers");
    }

    public static Response createOutgoingWireTransfer(final String sharedKey,
                                                      final Map<String, String> headers,
                                                      final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                                      final String idempotencyReference) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .header("idempotency-ref", idempotencyReference)
                .body(createOutgoingWireTransferModel)
                .when()
                .post("/openbanking/payment_initiation/outgoing_wire_transfers");
    }

    public static Response getOutgoingWireTransfer(final String sharedKey,
                                                   final Map<String, String> headers,
                                                   final String paymentConsentId) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .pathParam("id", paymentConsentId)
                .when()
                .get("/openbanking/payment_initiation/outgoing_wire_transfers/{id}");
    }

    public static Response revokeOutgoingWireTransferInitiation(final String sharedKey,
                                                                final Map<String, String> headers,
                                                                final String paymentConsentId) {

        return getOpenBankingRequest(sharedKey)
                .header("Date", headers.get("date"))
                .header("Digest", headers.get("digest"))
                .header("TPP-Signature", headers.get("TPP-Signature"))
                .pathParam("id", paymentConsentId)
                .when()
                .post("/openbanking/payment_initiation/outgoing_wire_transfers/{id}/revoke");
    }
}
