package opc.services.multi;

import io.restassured.response.Response;
import opc.models.shared.VerificationModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class AuthenticationFactorsService extends BaseService {

    public static Response enrolOtp(final String channel,
                                    final String secretKey,
                                    final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post("/multi/authentication_factors/otp/{channel}");
    }

    public static Response enrolPush(final String channel,
                                     final String secretKey,
                                     final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post("/multi/authentication_factors/push/{channel}");
    }

    public static Response verifyEnrolment(final VerificationModel verificationModel,
                                           final String channel,
                                           final String secretKey,
                                           final String token){
        return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .post("/multi/authentication_factors/otp/{channel}/verify");
    }

    public static Response getAuthenticationFactors(final String secretKey,
                                                    final Optional<Map<String, Object>> filters,
                                                    final String token){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/authentication_factors");
    }

    public static Response unenrolPush(final String channel,
                                    final String secretKey,
                                    final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("channel", channel)
                .when()
                .delete("/multi/authentication_factors/push/{channel}");
    }

}
