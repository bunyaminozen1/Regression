package fpi.paymentrun.services;

import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.UpdateBuyerModel;
import io.restassured.response.Response;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;

import java.util.Optional;

public class BuyersService extends BaseService {

    public static Response createBuyer(final CreateBuyerModel createBuyerModel,
                                       final String apiKey) {
        return getBodyApiKeyRequest(createBuyerModel, apiKey)
                .when()
                .post(String.format("%s/v1/buyers", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response createBuyerNoApiKey(final CreateBuyerModel createBuyerModel) {
        return getBodyRequest(createBuyerModel)
                .when()
                .post(String.format("%s/v1/buyers", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getBuyer(final String apiKey,
                                    final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .when()
                .get(String.format("%s/v1/buyers", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response getBuyerNoApiKey(final String authenticationToken) {
        return getAuthenticatedRequest(authenticationToken)
                .when()
                .get(String.format("%s/v1/buyers", getPaymentRunEnvironmentPrefix()));
    }

    public static Response updateBuyer(final UpdateBuyerModel updateBuyerModel,
                                       final String apiKey,
                                       final String authenticationToken) {
        return getBodyApiKeyAuthenticationRequest(updateBuyerModel, apiKey, authenticationToken)
                .when()
                .patch(String.format("%s/v1/buyers", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response updateBuyerNoApiKey(final UpdateBuyerModel updateBuyerModel,
                                               final String authenticationToken) {
        return getBodyAuthenticatedRequest(updateBuyerModel, authenticationToken)
                .when()
                .patch(String.format("%s/v1/buyers", getPaymentRunEnvironmentPrefix()));
    }

    public static Response sendEmailVerification(final SendEmailVerificationModel sendEmailVerificationModel,
                                                 final String secretKey) {
        return getBodyApiKeyRequest(sendEmailVerificationModel, secretKey)
                .when()
                .post(String.format("%s/v1/buyers/verification/email/send", getPaymentRunEnvironmentPrefix()));
    }

    public static Response verifyEmail(final EmailVerificationModel emailVerificationModel,
                                       final String secretKey) {
        return getBodyApiKeyRequest(emailVerificationModel, secretKey)
                .when()
                .post(String.format("%s/v1/buyers/verification/email/verify", getPaymentRunEnvironmentPrefix()));
    }

    public static Response startKyb(final String secretKey,
                                    final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .post(String.format("%s/v1/buyers/kyb", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response startKybNoApiKey(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .post(String.format("%s/v1/buyers/kyb", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getKyb(final String secretKey,
                                  final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .get(String.format("%s/v1/buyers/kyb", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response getKybNoApiKey(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get(String.format("%s/v1/buyers/kyb", getPaymentRunEnvironmentPrefix()));
    }
}
