package fpi.paymentrun.services;

import io.restassured.response.Response;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.LoginModel;
import opc.models.shared.VerificationModel;

import java.util.Optional;

public class AuthenticationService extends BaseService {
    
    public static Response createPassword(final CreatePasswordModel createPasswordModel,
                                          final String userId,
                                          final String secretKey) {
        return getBodyApiKeyRequest(createPasswordModel, secretKey)
                .pathParam("user_id", userId)
                .when()
                .post(String.format("%s/v1/password/{user_id}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response loginWithPassword(final LoginModel loginModel,
                                             final String secretKey) {
        return getBodyApiKeyRequest(loginModel, secretKey)
                .when()
                .post(String.format("%s/v1/login_with_password", getPaymentRunEnvironmentPrefix()));
    }

    public static Response logout(final String secretKey,
                                  final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .when()
                .post(String.format("%s/v1/logout", getPaymentRunEnvironmentPrefix()));
    }

    public static Response enrolOtp(final String channel,
                                    final String secretKey,
                                    final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post(String.format("%s/v1/authentication_factors/otp/{channel}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response verifyEnrolment(final VerificationModel verificationModel,
                                           final String channel,
                                           final String secretKey,
                                           final String token) {
        return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post(String.format("%s/v1/authentication_factors/otp/{channel}/verify", getPaymentRunEnvironmentPrefix()));
    }

    public static Response startStepup(final String channel,
                                       final String secretKey,
                                       final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post(String.format("%s/v1/stepup/challenges/otp/{channel}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response verifyStepup(final VerificationModel verificationModel,
                                        final String channel,
                                        final String secretKey,
                                        final String token) {
        return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post(String.format("%s/v1/stepup/challenges/otp/{channel}/verify", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for NoApiKey cases
     */
    public static Response verifyStepupNoApiKey(final VerificationModel verificationModel,
                                                final String channel,
                                                final String token) {
        return getBodyAuthenticatedRequest(verificationModel, token)
                .pathParam("channel", channel)
                .when()
                .post(String.format("%s/v1/stepup/challenges/otp/{channel}/verify", getPaymentRunEnvironmentPrefix()));
    }
}
