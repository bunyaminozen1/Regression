package opc.services.multiprivate;

import commons.services.BaseService;
import io.restassured.response.Response;
import opc.models.multi.challenges.ChallengesModel;

public class ChallengesService extends BaseService {

    public static Response issueOtpChallenges(final ChallengesModel challengesModel,
                                              final String channel,
                                              final String secretKey,
                                              final String token){
        return getBodyApiKeyAuthenticationRequest(challengesModel, secretKey, token)
                .pathParam("channel", channel)
                .when()
                .post("/multi_private/challenges/otp/{channel}");
    }

    public static Response verifyOtpChallenges(final ChallengesModel challengesModel,
                                               final String scaChallengeId,
                                               final String channel,
                                               final String secretKey,
                                               final String token){
        return getBodyApiKeyAuthenticationRequest(challengesModel, secretKey, token)
                .pathParam("channel", channel)
                .pathParam("sca_challenge_id", scaChallengeId)
                .when()
                .post("/multi_private/challenges/{sca_challenge_id}/otp/{channel}/verify");
    }
}
