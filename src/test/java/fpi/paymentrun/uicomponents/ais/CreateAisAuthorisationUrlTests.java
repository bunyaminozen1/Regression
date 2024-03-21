package fpi.paymentrun.uicomponents.ais;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateAisAuthorisationUrlRequestModel;
import fpi.paymentrun.services.uicomponents.AisUxComponentService;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class CreateAisAuthorisationUrlTests extends BasePaymentRunSetup {
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
    public void CreateAisAuthorisationUrl_ValidRoleBuyerToken_Success() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreateAisAuthorisationUrl_MultipleRolesBuyerToken_Success() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        BuyersHelper.assignAllRoles(secretKey, buyerToken);
        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreateAisAuthorisationUrl_ValidRoleAuthUserToken_Success() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, authUserToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreateAisAuthorisationUrl_MultipleRolesAuthUserToken_Success() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, authUserToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreateAisAuthorisationUrl_AdminRoleBuyerToken_Forbidden() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        BuyersHelper.assignAdminRole(secretKey, buyerToken);
        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateAisAuthorisationUrl_IncorrectRoleBuyerToken_Forbidden() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        BuyersHelper.assignCreatorRole(secretKey, buyerToken);
        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateAisAuthorisationUrl_IncorrectRoleAuthUserToken_Forbidden() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, authUserToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateAisAuthorisationUrl_WrongInstitutionId_Conflict() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel()
                        .institutionId(RandomStringUtils.randomAlphabetic(8))
                        .build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTITUTION_NOT_FOUND"));
    }

    @Test
    public void CreateAisAuthorisationUrl_WrongRedirectUrlFormat_Conflict() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel()
                        .redirectUrl(RandomStringUtils.randomAlphabetic(6))
                        .build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_REDIRECT_URL"));
    }

    @Test
    public void CreateAisAuthorisationUrl_NoInstitutionId_BadRequest() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel()
                        .institutionId(null)
                        .build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", CoreMatchers.equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("institutionId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", CoreMatchers.equalTo(1));
    }

    @Test
    public void CreateAisAuthorisationUrl_NoRedirectUrl_BadRequest() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel()
                        .redirectUrl(null)
                        .build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", CoreMatchers.equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("redirectUrl"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", CoreMatchers.equalTo(1));
    }

    @Test
    public void CreateAisAuthorisationUrl_WrongToken_Unauthorized() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, RandomStringUtils.randomAlphabetic(18), sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateAisAuthorisationUrl_NoToken_Unauthorized() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, null, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateAisAuthorisationUrl_InvalidSharedKey_Unauthorized() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateAisAuthorisationUrl_DifferentProgrammeSharedKey_Unauthorized() {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateAisAuthorisationUr_IdentityUnderMultiProgramme_Unauthorized() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdMultiApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyMultiApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel().build();

        AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, corporateAuthenticationToken, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static String createBuyer() {
        return BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
    }
}
