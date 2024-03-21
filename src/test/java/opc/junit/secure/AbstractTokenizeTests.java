package opc.junit.secure;

import io.restassured.response.ValidatableResponse;
import opc.junit.database.GpsDatabaseHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.secure.AdditionalPropertiesModel;
import opc.models.secure.TokenizeModel;
import opc.models.secure.TokenizePropertiesModel;
import opc.services.secure.SecureService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractTokenizeTests extends BaseSecureSetup {
    protected abstract String getAuthToken();

    @Test
    public void Tokenize_HappyPath_Success() throws SQLException {

        final String random = SecureService.associate(sharedKey, getAuthToken(), Optional.empty())
                .jsonPath()
                .getString("random");

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(random)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("1234")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        final ValidatableResponse tokenizedResponse = SecureService.tokenize(sharedKey, getAuthToken(), tokenizeModel)
                .then()
                .statusCode(SC_OK)
                .body("tokens.additionalProp1.size()", is(24));

        final String plainTextCardNumber = SecureHelper.detokenize(tokenizedResponse.extract().body().jsonPath().get("tokens.additionalProp1"), random, sharedKey, getAuthToken());

        final Map<Integer, Map<String, String>> cardDetails = GpsDatabaseHelper.getCardDetailsbyId(tokenizedResponse.extract().body().jsonPath().get("id"));

        cardDetails.forEach((key, value) -> assertEquals(plainTextCardNumber, value.get("card_number")));
    }

    @Test
    public void Tokenize_NoRandom_BadRequest() {

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom("")
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("1234")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize(sharedKey, getAuthToken(), tokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("random"))
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"));
    }

    @Test
    public void Tokenize_WrongRandom_UnAuthorized() {

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom("1234")
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("asd")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize(sharedKey, getAuthToken(), tokenizeModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Tokenize_NoValue_BadRequest() {

        final String random = SecureService.associate(sharedKey, getAuthToken(), Optional.empty())
                .jsonPath()
                .getString("random");

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(random)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize(sharedKey, getAuthToken(), tokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("values[additionalProp1].value"))
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"));
    }

    @Test
    public void Tokenize_NoSharedKey_BadRequest() {

        final String random = SecureService.associate(sharedKey, getAuthToken(), Optional.empty())
                .jsonPath()
                .getString("random");

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(random)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("1234")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize("", getAuthToken(), tokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("programme-key"));
    }

    @Test
    public void Tokenize_UnknownSharedKey_BadRequest() {

        final String random = SecureService.associate(sharedKey, getAuthToken(), Optional.empty())
                .jsonPath()
                .getString("random");

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(random)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("1234")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize("UNKNOWN", getAuthToken(), tokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid programme key"));
    }

    @Test
    public void Tokenize_NoIdentityToken_UnAuthorized() {

        final String random = SecureService.associate(sharedKey, getAuthToken(), Optional.empty())
                .jsonPath()
                .getString("random");

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(random)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("1234")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize(sharedKey, "", tokenizeModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Tokenize_UnknownIdentityKey_UnAuthorized() {

        final String random = SecureService.associate(sharedKey, getAuthToken(), Optional.empty())
                .jsonPath()
                .getString("random");

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(random)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue("1234")
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        SecureService.tokenize(sharedKey, "UNKNOWN", tokenizeModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
