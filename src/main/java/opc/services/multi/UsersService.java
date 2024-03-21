package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.users.ConsumerUserInviteModel;
import opc.models.multi.users.UserVerifyEmailModel;
import opc.models.multi.users.UsersModel;
import opc.models.multi.users.ValidateUserInviteModel;
import opc.models.shared.SendEmailVerificationModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class UsersService extends BaseService {

    public static Response createUser(final UsersModel usersModel,
                                      final String secretKey,
                                      final String authenticationToken,
                                      final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(usersModel, secretKey, authenticationToken, idempotencyRef)
                .when()
                .post("/multi/users");
    }

    public static Response getUsers(final String secretKey,
                                    final Optional<Map<String, Object>> filters,
                                    final String token){

        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/users");
    }

    public static Response getUser(final String secretKey,
                                   final String userId,
                                   final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("user_id", userId)
                .when()
                .get("/multi/users/{user_id}");
    }

    public static Response patchUser(final UsersModel usersModel,
                                     final String secretKey,
                                     final String userId,
                                     final String token,
                                     final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(usersModel, secretKey, token, idempotencyRef)
                .pathParam("user_id", userId)
                .when()
                .patch("/multi/users/{user_id}");
    }

    public static Response activateUser(final String secretKey,
                                        final String userId,
                                        final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("user_id", userId)
                .when()
                .post("/multi/users/{user_id}/activate");
    }

    public static Response deactivateUser(final String secretKey,
                                          final String userId,
                                          final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("user_id", userId)
                .when()
                .post("/multi/users/{user_id}/deactivate");
    }

    public static Response inviteUser(final String secretKey,
                                      final String userId,
                                      final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("user_id", userId)
                .when()
                .post("/multi/users/{user_id}/invite");
    }

    public static Response validateUserInvite(final ValidateUserInviteModel validateUserInviteModel,
                                              final String secretKey,
                                              final String userId){
        return getBodyApiKeyRequest(validateUserInviteModel, secretKey)
                .pathParam("user_id", userId)
                .when()
                .post("/multi/users/{user_id}/invite/validate");
    }

    public static Response consumeUserInvite(final ConsumerUserInviteModel consumerUserInviteModel,
                                             final String secretKey,
                                             final String userId){
        return getBodyApiKeyRequest(consumerUserInviteModel, secretKey)
                .pathParam("user_id", userId)
                .when()
                .post("/multi/users/{user_id}/invite/consume");
    }

    public static Response startUserKyc(final String secretKey,
                                        final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .post("/multi/users/kyc");
    }
    public static Response sendEmailVerification(final SendEmailVerificationModel sendEmailVerificationModel,
                                                 final String secretKey){
        return getBodyApiKeyRequest(sendEmailVerificationModel, secretKey)
            .when()
            .post("/multi/users/verification/email/send");
    }
    public static Response verifyUserEmail(final UserVerifyEmailModel userVerifyEmailModel,
                                           final String secretKey){
        return getBodyApiKeyRequest(userVerifyEmailModel, secretKey)
            .when()
            .post("/multi/users/verification/email/verify");
    }
}
