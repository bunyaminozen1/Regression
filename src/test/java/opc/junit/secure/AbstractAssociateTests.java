package opc.junit.secure;

import opc.junit.helpers.secure.SecureHelper;
import opc.models.secure.AdditionalPropertiesModel;
import opc.models.secure.TokenizeModel;
import opc.models.secure.TokenizePropertiesModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class AbstractAssociateTests extends BaseSecureSetup {
    protected abstract String getAuthenticationToken();

    @Test
    public void AssociateTests_Identity_Success() {
        SecureService.associate(sharedKey, getAuthenticationToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("random", notNullValue());
    }

    @Test
    public void AssociateTests_SharedKeyEmpty_BadRequest() {
        SecureService.associate("", getAuthenticationToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("programme-key"));
    }

    @Test
    public void AssociateTests_AuthenticationTokenEmpty_Unauthorized() {
        SecureService.associate(sharedKey, null, Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void AssociateTests_WrongOrigin_Forbidden() {
        SecureService.associate(sharedKey, getAuthenticationToken(), Optional.of(RandomStringUtils.randomAlphabetic(7)))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void Associate_TokenizeAndAssociate_Success() {
        final String random = SecureHelper.associate(getAuthenticationToken(), sharedKey);

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

        SecureService.tokenize(sharedKey, getAuthenticationToken(), tokenizeModel)
                .then()
                .statusCode(SC_OK)
                .body("tokens.additionalProp1", notNullValue());
    }
}
