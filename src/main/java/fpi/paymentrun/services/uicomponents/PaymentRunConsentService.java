package fpi.paymentrun.services.uicomponents;

import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;
import opc.enums.opc.UrlType;
import opc.models.shared.VerificationModel;

public class PaymentRunConsentService extends BaseService {

    public static Response getPaymentRunConsentInfo(final String paymentRunId,
                                                    final String authenticationToken,
                                                    final String sharedKey){
         return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .pathParam("payment_run_id", paymentRunId)
                .when()
                .get(String.format("%s/ui_components/v1/payment_run_sca/{payment_run_id}/consent_info", getPaymentRunEnvironmentPrefix()));
    }

    public static Response issueScaChallengeRequest(final String authenticationToken,
                                                    final String sharedKey,
                                                    final String paymentRunId,
                                                    final String channel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .pathParam("payment_run_id", paymentRunId)
                .pathParam("channel", channel)
                .when()
                .post(String.format("%s/ui_components/v1/payment_run_sca/{payment_run_id}/challenges/otp/{channel}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response verifyScaChallengeRequest(final VerificationModel verificationCode,
                                                    final String authenticationToken,
                                                     final String sharedKey,
                                                     final String paymentRunId,
                                                     final String channel) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .pathParam("payment_run_id", paymentRunId)
                .pathParam("channel", channel)
                .body(verificationCode)
                .when()
                .post(String.format("%s/ui_components/v1/payment_run_sca/{payment_run_id}/challenges/otp/{channel}/verify", getPaymentRunEnvironmentPrefix()));
    }
}
