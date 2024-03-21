package opc.junit.secure.openbanking.mobilesdk;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.secure.AnonTokenizeModel;
import opc.models.secure.DetokenizeModel;
import opc.models.secure.TokenizeModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.Matchers.equalTo;

public class SecureServiceMobileSDKTests extends BaseSetup {
    private static String corporateAuthenticationToken;

    @Test
    public void AnonTokenize_WrongSharedKey_BadRequest() {
        AnonTokenizeModel anonTokenizeModel = AnonTokenizeModel.builder().build();
        SecureService.anonTokenize(RandomStringUtils.randomAlphabetic(24), anonTokenizeModel, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid programme key"));
    }

    @Test
    public void Tokenize_WrongSharedKey_BadRequest() {
        corporateSetup();
        TokenizeModel tokenizeModel = TokenizeModel.builder().build();
        SecureService.tokenize(RandomStringUtils.randomAlphabetic(24), corporateAuthenticationToken, tokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid programme key"));
    }

    @Test
    public void Detokenize_WrongSharedKey_BadRequest() {
        corporateSetup();
        DetokenizeModel detokenizeModel = DetokenizeModel.builder().build();
        SecureService.detokenize(RandomStringUtils.randomAlphabetic(24), corporateAuthenticationToken, detokenizeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid programme key"));
    }

    @Test
    public void Associate_WrongSharedKey_BadRequest() {
        corporateSetup();
        SecureService.associate(RandomStringUtils.randomAlphabetic(24), corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid programme key"));
    }

    private void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
