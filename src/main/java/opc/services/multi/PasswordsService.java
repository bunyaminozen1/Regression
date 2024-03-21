package opc.services.multi;

import io.restassured.response.Response;
import opc.models.PasswordValidationModel;
import opc.models.multi.passwords.*;
import commons.services.BaseService;

import java.util.Optional;

public class PasswordsService extends BaseService {

    public static Response createPassword(final CreatePasswordModel createPasswordModel,
                                          final String userId,
                                          final String secretKey){
        return getBodyApiKeyRequest(createPasswordModel, secretKey)
                .pathParam("user_id", userId)
                .when()
                .post("/multi/passwords/{user_id}/create");
    }

    public static Response updatePassword(final UpdatePasswordModel updatePasswordModel,
                                          final String secretKey,
                                          final String token){
        return getBodyApiKeyAuthenticationRequest(updatePasswordModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/passwords/update");
    }

    public static Response validatePassword(final PasswordValidationModel passwordModel,
                                            final String secretKey){
        return getBodyApiKeyRequest(passwordModel, secretKey)
                .when()
                .post("/multi/passwords/validate");
    }

    public static Response startLostPassword(final LostPasswordStartModel lostPasswordStartModel,
                                             final String secretKey){
        return getBodyApiKeyRequest(lostPasswordStartModel, secretKey)
                .when()
                .post("/multi/passwords/lost_password/start");
    }

    public static Response resumeLostPassword(final LostPasswordResumeModel lostPasswordStartModel,
                                              final String secretKey){
        return getBodyApiKeyRequest(lostPasswordStartModel, secretKey)
                .when()
                .post("/multi/passwords/lost_password/resume");
    }
}
