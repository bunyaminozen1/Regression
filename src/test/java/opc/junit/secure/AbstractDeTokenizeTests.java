package opc.junit.secure;

import io.restassured.path.json.JsonPath;
import opc.junit.database.GpsDatabaseHelper;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.secure.DetokenizeModel;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractDeTokenizeTests extends BaseSecureSetup {
    protected abstract String getAuthToken();
    protected abstract String getPrepaidManagedCardsProfileId();
    protected abstract String getCurrency();
    protected abstract String getAssociateRandom();

    @Test
    public void DeTokenize_CardNumber_Success() throws SQLException
    {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getPrepaidManagedCardsProfileId(), getCurrency())
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getAuthToken(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel cardNumberDetokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cardNumber.value"))
                        .setRandom(getAssociateRandom())
                        .build();

        final String plainTextCardNumber = SecureService.detokenize(sharedKey, getAuthToken(), cardNumberDetokenizeModel)
                .then()
                .statusCode(SC_OK)
                .body("value", notNullValue())
                .extract().path("value");

        final Map<Integer, Map<String, String>> cardDetails = GpsDatabaseHelper.getCardDetailsbyId(jsonPath.get("id"));

        cardDetails.forEach((key, value) -> assertEquals(plainTextCardNumber, value.get("card_number")));
    }

    @Test
    public void DeTokenize_Cvv_Success() throws SQLException
    {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getPrepaidManagedCardsProfileId(), getCurrency())
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getAuthToken(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel cvvDetokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cvv.value"))
                        .setRandom(getAssociateRandom())
                        .build();

        final String plainCvvDetails = SecureService.detokenize(sharedKey, getAuthToken(), cvvDetokenizeModel)
                .then()
                .statusCode(SC_OK)
                .body("value", notNullValue())
                .extract().path("value");

        final Map<Integer, Map<String, String>> cvvDetails = GpsDatabaseHelper.getCardDetailsbyId(jsonPath.get("id"));

        cvvDetails.forEach((key, value) -> assertEquals(plainCvvDetails, value.get("cvv")));
    }

    @Test
    public void DeTokenize_WrongToken_BadRequest()
    {
        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken("UNKNOWN")
                        .setRandom(getAssociateRandom())
                        .build();

        SecureService.detokenize(sharedKey, getAuthToken(), detokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("token.token"));
    }

    @Test
    public void DeTokenize_WrongRandom_Unauthorized()
    {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getPrepaidManagedCardsProfileId(), getCurrency())
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getAuthToken(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cvv.value"))
                        .setRandom("UNKNOWN")
                        .build();

        SecureService.detokenize(sharedKey, getAuthToken(), detokenizeModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeTokenize_BadSharedKey_BadRequest()
    {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getPrepaidManagedCardsProfileId(), getCurrency())
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getAuthToken(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cvv.value"))
                        .setRandom(getAssociateRandom())
                        .build();

        SecureService.detokenize("UNKNOWN", getAuthToken(), detokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void DeTokenize_WrongAuthToken_Unauthorized()
    {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getPrepaidManagedCardsProfileId(), getCurrency())
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getAuthToken(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cvv.value"))
                        .setRandom(getAssociateRandom())
                        .build();

        SecureService.detokenize(sharedKey, "UNKNOWN", detokenizeModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
