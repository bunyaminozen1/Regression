package opc.services.openbanking;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.models.openbanking.LoginModel;
import opc.models.openbanking.VerificationModel;
import commons.services.BaseService;

public class OpenBankingSecureService extends BaseService {

    public static RequestSpecification getSecureOpenBankingRequest(final String sharedKey,
                                                                   final String authenticationToken,
                                                                   final String tppId) {
        return getRequest()
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("tpp-id", tppId);
    }

    public static Response login(final String sharedKey,
                                 final LoginModel loginModel,
                                 final String tppId) {

        return getRequest()
                .header("programme-key", sharedKey)
                .header("tpp-id", tppId)
                .body(loginModel)
                .when()
                .post("/secure/openbanking/login_with_password");
    }

    public static Response authoriseConsent(final String sharedKey,
                                            final String authenticationToken,
                                            final String tppId,
                                            final String consentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("consent_id", consentId)
                .when()
                .post("/secure/openbanking/account_information/consents/{consent_id}/authorise");
    }

    public static Response authorisePayment(final String sharedKey,
                                            final String authenticationToken,
                                            final String tppId,
                                            final String paymentConsentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("id", paymentConsentId)
                .when()
                .post("/secure/openbanking/payment_initiation/outgoing_wire_transfers/{id}/authorise");
    }

    public static Response rejectPayment(final String sharedKey,
                                         final String authenticationToken,
                                         final String tppId,
                                         final String paymentConsentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("id", paymentConsentId)
                .when()
                .post("/secure/openbanking/payment_initiation/outgoing_wire_transfers/{id}/reject");
    }

    public static Response initiatePaymentChallenge(final String sharedKey,
                                                    final String authenticationToken,
                                                    final String tppId,
                                                    final String paymentConsentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("id", paymentConsentId)
                .when()
                .post("/secure/openbanking/payment_initiation/outgoing_wire_transfers/{id}/challenges/otp");
    }

    public static Response verifyPaymentChallenge(final String sharedKey,
                                                  final String authenticationToken,
                                                  final String tppId,
                                                  final String paymentConsentId,
                                                  final VerificationModel verificationModel) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("id", paymentConsentId)
                .body(verificationModel)
                .when()
                .post("/secure/openbanking/payment_initiation/outgoing_wire_transfers/{id}/verify");
    }

    public static Response getOutgoingWireTransfer(final String sharedKey,
                                                   final String authenticationToken,
                                                   final String tppId,
                                                   final String paymentConsentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("id", paymentConsentId)
                .when()
                .get("/secure/openbanking/payment_initiation/outgoing_wire_transfers/{id}");
    }

    public static Response rejectConsent(final String sharedKey,
                                         final String authenticationToken,
                                         final String tppId,
                                         final String consentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("consent_id", consentId)
                .when()
                .post("/secure/openbanking/account_information/consents/{consent_id}/reject");
    }

    public static Response verifyConsent(final String sharedKey,
                                         final String authenticationToken,
                                         final String tppId,
                                         final String consentId,
                                         final VerificationModel verificationModel) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("consent_id", consentId)
                .body(verificationModel)
                .when()
                .post("/secure/openbanking/account_information/consents/{consent_id}/verify");
    }

    public static Response getConsent(final String sharedKey,
                                      final String authenticationToken,
                                      final String tppId,
                                      final String consentId) {

        return getSecureOpenBankingRequest(sharedKey, authenticationToken, tppId)
                .pathParam("consent_id", consentId)
                .when()
                .get("/secure/openbanking/account_information/consents/{consent_id}");
    }

    public static Response logout(final String sharedKey,
                                  final String token,
                                  final String tppId) {

        return getRequest()
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", token))
                .header("tpp-id", tppId)
                .when()
                .post("/secure/openbanking/logout");
    }
}
