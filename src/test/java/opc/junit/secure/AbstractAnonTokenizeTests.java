package opc.junit.secure;

import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.models.secure.AdditionalPropertiesModel;
import opc.models.secure.AnonTokenizeModel;
import opc.models.secure.TokenizePropertiesModel;
import opc.models.simulator.TokenizeModel;
import opc.services.secure.SecureService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class AbstractAnonTokenizeTests extends BaseSecureSetup {
    protected abstract String getIdentityToken();

    @Test
    public void AnonTokenize_Pin_Success() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(tokenize("9876", SecurityModelConfiguration.PIN, getIdentityToken()))
                                        .build())
                                .build()).build();

        SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tokens.additionalProp1", notNullValue());
    }

    @Test
    public void AnonTokenize_CarNumber_Success() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(tokenize("9876 1234 1234 567", SecurityModelConfiguration.CARD_NUMBER, getIdentityToken()))
                                        .build())
                                .build()).build();

        SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tokens.additionalProp1", notNullValue());
    }

    @Test
    public void AnonTokenize_Cvv_Success() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(tokenize("987", SecurityModelConfiguration.CVV, getIdentityToken()))
                                        .build())
                                .build()).build();

        SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tokens.additionalProp1", notNullValue());
    }

    @Test
    public void AnonTokenize_Password_Success() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.CVV, getIdentityToken()))
                                        .build())
                                .build()).build();

        SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("tokens.additionalProp1", notNullValue());
    }

    @Test
    public void AnonTokenize_ValueMissingInModel_BadRequest() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .build())
                                .build()).build();

        SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"))
                .body("validation.fields[0].name", equalTo("values[additionalProp1].value"));
    }

    @Test
    public void AnonTokenize_SharedKeyMissing_BadRequest() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.CVV, getIdentityToken()))
                                        .build())
                                .build()).build();

        SecureService.anonTokenize("", anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("programme-key"));
    }

    @Test
    public void AnonTokenize_RandomOrigin_Forbidden() {

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(tokenize("9876", SecurityModelConfiguration.PIN, getIdentityToken()))
                                        .build())
                                .build()).build();

        SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.of(RandomStringUtils.randomAlphabetic(7)))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private String tokenize(final String value,
                            final SecurityModelConfiguration field,
                            final String token) {
        return SimulatorService.tokenize(secretKey,
                        new TokenizeModel(new opc.models.simulator.TokenizePropertiesModel(new opc.models.simulator.AdditionalPropertiesModel(value, field.name()))), token)
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");
    }
}
