package opc.services.multi;

import io.restassured.response.Response;
import opc.models.shared.Identity;
import opc.models.shared.LoginModel;
import opc.models.shared.LoginWithBiometricModel;
import opc.models.shared.VerificationModel;
import commons.services.BaseService;

import java.util.Optional;

public class AuthenticationService extends BaseService {

    public static Response loginWithPassword(final LoginModel login,
                                             final String secretKey) {
        return getBodyApiKeyRequest(login, secretKey)
                .when()
                .post("/multi/login_with_password");
    }

    public static Response loginWithBiometric(final LoginWithBiometricModel login,
                                             final String secretKey) {
        return getBodyApiKeyRequest(login, secretKey)
                .when()
                .post("/multi/login_via_biometrics");
    }

    public static Response logout(final String secretKey, final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .post("/multi/logout");
    }

    public static Response startStepup(final String channel,
                                       final String secretKey,
                                       final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post("multi/stepup/challenges/otp/{channel}");
    }

    public static Response verifyStepup(final VerificationModel verificationModel,
                                        final String channel,
                                        final String secretKey,
                                        final String token) {
        return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post("/multi/stepup/challenges/otp/{channel}/verify");
    }

    public static Response issuePushStepup(final String channel,
                                           final String secretKey,
                                           final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post("multi/stepup/challenges/push/{channel}");
    }

    public static Response loginWithIam(final String secretKey) {
        return getApiKeyRequest(secretKey)
                .when()
                .post("/multi/login_with_iam");
    }

    public static Response accessToken(final Identity identity,
                                       final String secretKey,
                                       final String token) {
        return getBodyApiKeyAuthenticationRequest(identity, secretKey, token, Optional.empty())
                .when()
                .post("/multi/access_token");
    }
}
