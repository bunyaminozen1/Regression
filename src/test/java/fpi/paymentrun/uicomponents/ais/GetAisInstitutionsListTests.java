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
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class GetAisInstitutionsListTests extends BasePaymentRunSetup {
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
    public void GetInstitutionsList_ValidRoleBuyerToken_Success(){
        AisUxComponentService.getInstitutionsList(buyerToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue());
    }

    @Test
    public void GetInstitutionsList_MultipleRolesBuyerToken_Success(){
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        AisUxComponentService.getInstitutionsList(buyerToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue());
    }

    @Test
    public void GetInstitutionsList_ValidRoleAuthUserToken_Success(){
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getInstitutionsList(authUserToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue());
    }

    @Test
    public void GetInstitutionsList_MultipleRolesAuthUserToken_Success(){
        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getInstitutionsList(authUserToken, sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue());
    }

    @Test
    public void GetInstitutionsList_AdminRoleBuyerToken_Forbidden(){
        BuyersHelper.assignAdminRole(secretKey, buyerToken);

        AisUxComponentService.getInstitutionsList(buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetInstitutionsList_IncorrectRoleBuyerToken_Forbidden(){
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        AisUxComponentService.getInstitutionsList(buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetInstitutionsList_IncorrectRoleAuthUserToken_Forbidden(){
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getInstitutionsList(authUserToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetInstitutionsList_NoToken_Unauthorised(){
        AisUxComponentService.getInstitutionsList( null, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetInstitutionsList_WrongToken_Unauthorised(){
        AisUxComponentService.getInstitutionsList( RandomStringUtils.randomAlphabetic(18), sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetInstitutionsList_InvalidSharedKey_Unauthorised(){
        AisUxComponentService.getInstitutionsList( buyerToken, RandomStringUtils.randomAlphabetic(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetInstitutionsList_AnotherProgrammeSharedKey_Unauthorised(){
        AisUxComponentService.getInstitutionsList( buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetInstitutionsList_IdentityUnderMultiProgramme_Unauthorised(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdMultiApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyMultiApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        AisUxComponentService.getInstitutionsList(corporateAuthenticationToken, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static String createBuyer() {
        return BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
    }
}
