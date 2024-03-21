package fpi.paymentrun.services;

import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.ConsumeUserInviteModel;
import fpi.paymentrun.models.ValidateUserInviteModel;
import io.restassured.response.Response;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;

import java.util.Map;
import java.util.Optional;

public class BuyersAuthorisedUsersService extends BaseService {

    public static Response createUser(final BuyerAuthorisedUserModel createBuyerAuthorisedUserModel,
                                      final String apiKey,
                                      final String token) {
        return getBodyApiKeyAuthenticationRequest(createBuyerAuthorisedUserModel, apiKey, token)
                .when()
                .post(String.format("%s/v1/users", getPaymentRunEnvironmentPrefix()));
    }

    public static Response updateUser(final BuyerAuthorisedUserModel createBuyerAuthorisedUserModel,
                                      final String userId,
                                      final String apiKey,
                                      final String token) {
        return getBodyApiKeyAuthenticationRequest(createBuyerAuthorisedUserModel, apiKey, token)
                .pathParam("user_id", userId)
                .when()
                .patch(String.format("%s/v1/users/{user_id}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response sendUserInvite(final String userId,
                                          final String apiKey,
                                          final String token) {
        return getApiKeyAuthenticationRequest(apiKey, token)
                .pathParam("user_id", userId)
                .when()
                .post(String.format("%s/v1/users/{user_id}/invite", getPaymentRunEnvironmentPrefix()));
    }

    public static Response consumeUserInvite(final ConsumeUserInviteModel consumeUserInviteModel,
                                             final String userId,
                                             final String apiKey) {
        return getBodyApiKeyRequest(consumeUserInviteModel, apiKey)
                .pathParam("user_id", userId)
                .when()
                .post(String.format("%s/v1/users/{user_id}/invite/consume", getPaymentRunEnvironmentPrefix()));
    }

    public static Response validateUserInvite(final ValidateUserInviteModel validateUserInviteModel,
                                              final String userId,
                                              final String apiKey) {
        return getBodyApiKeyRequest(validateUserInviteModel, apiKey)
                .pathParam("user_id", userId)
                .when()
                .post(String.format("%s/v1/users/{user_id}/invite/validate", getPaymentRunEnvironmentPrefix()));
    }

    public static Response activateUser(final String userId,
                                        final String apiKey,
                                        final String token) {
        return getApiKeyAuthenticationRequest(apiKey, token)
                .pathParam("user_id", userId)
                .when()
                .post(String.format("%s/v1/users/{user_id}/activate", getPaymentRunEnvironmentPrefix()));
    }

    public static Response deactivateUser(final String userId,
                                          final String apiKey,
                                          final String token) {
        return getApiKeyAuthenticationRequest(apiKey, token)
                .pathParam("user_id", userId)
                .when()
                .post(String.format("%s/v1/users/{user_id}/deactivate", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getUser(final String userId,
                                   final String apiKey,
                                   final String token) {
        return getApiKeyAuthenticationRequest(apiKey, token)
                .pathParam("user_id", userId)
                .when()
                .get(String.format("%s/v1/users/{user_id}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getUsers(final String apiKey,
                                    final String token,
                                    final Optional<Map<String, Object>> filters) {
        return assignQueryParams(getApiKeyAuthenticationRequest(apiKey, token), filters)
                .when()
                .get(String.format("%s/v1/users", getPaymentRunEnvironmentPrefix()));
    }

    public static Response sendEmailVerification(final SendEmailVerificationModel sendEmailVerificationModel,
                                                 final String apiKey) {
        return getBodyApiKeyRequest(sendEmailVerificationModel, apiKey)
                .when()
                .post(String.format("%s/v1/users/verification/email/send", getPaymentRunEnvironmentPrefix()));
    }

    public static Response verifyEmail(final EmailVerificationModel emailVerificationModel,
                                       final String apiKey) {
        return getBodyApiKeyRequest(emailVerificationModel, apiKey)
                .when()
                .post(String.format("%s/v1/users/verification/email/verify", getPaymentRunEnvironmentPrefix()));
    }
}
