package opc.junit.multiprivate;

import commons.enums.Currency;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;

public class ValidateAccessTokenTests extends BaseMultiPrivateSetup{

    private static String corporateAuthenticationToken;
    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Test
    public void ValidateAccessToken_Corporate_Success() {

        MultiPrivateService.validateAccessToken(corporateAuthenticationToken, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ValidateAccessToken_CorporateLoggedOut_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final String corporateToken = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey).getRight();
        AuthenticationHelper.logout(corporateToken, secretKey);

        MultiPrivateService.validateAccessToken(corporateToken, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ValidateAccessToken_CorporateRandomAccessToken_Unauthorised() {

        MultiPrivateService.validateAccessToken(RandomStringUtils.randomAlphabetic(23), secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ValidateAccessToken_CorporateNoAccessToken_Unauthorised() {

        MultiPrivateService.validateAccessToken(null, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ValidateAccessToken_CorporateNoSecretKey_Unauthorised() {

        MultiPrivateService.validateAccessToken(null, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
