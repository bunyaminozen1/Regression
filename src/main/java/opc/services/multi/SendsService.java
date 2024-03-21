package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.sends.BulkSendFundsModel;
import opc.models.multi.sends.CancelScheduledModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.VerificationModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class SendsService extends BaseService {

    public static Response sendFunds(final SendFundsModel sendFundsModel,
                                     final String secretKey,
                                     final String token,
                                     final Optional<String> idempotencyRef) {
        return getBodyApiKeyAuthenticationRequest(sendFundsModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/sends");
    }

    public static Response bulkSendFunds(final BulkSendFundsModel bulkSendFundsModel,
        final String secretKey,
        final String token,
        final Optional<String> idempotencyRef) {
        return getBodyApiKeyAuthenticationRequest(bulkSendFundsModel, secretKey, token, idempotencyRef)
            .when()
            .post("/multi/sends/bulk/create");
    }

    public static Response cancelScheduledSend(final CancelScheduledModel cancelScheduledModel,
                                               final String secretKey,
                                               final String token) {
        return getBodyApiKeyAuthenticationRequest(cancelScheduledModel, secretKey, token)
            .when()
            .post("/multi/sends/bulk/cancel");
    }

    public static Response getSends(final String secretKey,
                                    final Optional<Map<String, Object>> filters,
                                    final String token) {
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/sends");
    }

    public static Response getSend(final String secretKey,
                                   final String transactionId,
                                   final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", transactionId)
                .when()
                .get("/multi/sends/{id}");
    }

    public static Response startSendOtpVerification(final String sendId,
                                                    final String channel,
                                                    final String secretKey,
                                                    final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", sendId)
                .pathParam("channel", channel)
                .when()
                .post("/multi/sends/{id}/challenges/otp/{channel}");
    }

    public static Response verifySendOtp(final VerificationModel verificationModel,
                                         final String sendId,
                                         final String channel,
                                         final String secretKey,
                                         final String token) {
        return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
                .pathParam("id", sendId)
                .pathParam("channel", channel)
                .when()
                .post("/multi/sends/{id}/challenges/otp/{channel}/verify");
    }

    public static Response startSendPushVerification(final String sendId,
                                                    final String channel,
                                                    final String secretKey,
                                                    final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", sendId)
                .pathParam("channel", channel)
                .when()
                .post("/multi/sends/{id}/challenges/push/{channel}");
    }
}