package fpi.paymentrun.uicomponents.ais;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.services.uicomponents.AisUxComponentService;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class GetAisConsentInfoTests extends BasePaymentRunSetup {
    private String buyerToken;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeEach
    public void Setup() {
        buyerToken = createBuyer();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
    }

    @Test
    public void GetAisConsentInfo_ValidRoleBuyerToken_Success(){
        AisUxComponentService.getAisConsentInfo(buyerToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("embedder.companyRegistrationName", equalTo(pluginsApp.getInnovatorName()));
    }

    @Test
    public void GetAisConsentInfo_MultipleRolesBuyerToken_Success(){
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        AisUxComponentService.getAisConsentInfo(buyerToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("embedder.companyRegistrationName", equalTo(pluginsApp.getInnovatorName()));
    }

    @Test
    public void GetAisConsentInfo_ValidRoleAuthUserToken_Success(){
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getAisConsentInfo(authUserToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("embedder.companyRegistrationName", equalTo(pluginsApp.getInnovatorName()));
    }

    @Test
    public void GetAisConsentInfo_MultipleRolesAuthUserToken_Success(){
        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getAisConsentInfo(authUserToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("embedder.companyRegistrationName", equalTo(pluginsApp.getInnovatorName()));
    }

    @Test
    public void GetAisConsentInfo_AdminRoleBuyerToken_Forbidden(){
        BuyersHelper.assignAdminRole(secretKey, buyerToken);

        AisUxComponentService.getAisConsentInfo(buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAisConsentInfo_IncorrectRoleBuyerToken_Forbidden(){
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        AisUxComponentService.getAisConsentInfo(buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAisConsentInfo_IncorrectRoleAuthUserToken_Forbidden(){
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getAisConsentInfo(authUserToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAisConsentInfo_NoToken_Unauthorised(){
        AisUxComponentService.getAisConsentInfo( null, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentInfo_InvalidToken_Unauthorised(){
        AisUxComponentService.getAisConsentInfo( RandomStringUtils.randomAlphabetic(18), sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentInfo_InvalidSharedKey_Unauthorised(){
        AisUxComponentService.getAisConsentInfo(buyerToken, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentInfo_DifferentProgrammeSharedKey_Unauthorised(){
        AisUxComponentService.getAisConsentInfo(buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentInfo_IdentityUnderMultiProgramme_Unauthorised(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdMultiApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyMultiApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        AisUxComponentService.getAisConsentInfo(corporateAuthenticationToken, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static String createBuyer() {
        return BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
    }
}
