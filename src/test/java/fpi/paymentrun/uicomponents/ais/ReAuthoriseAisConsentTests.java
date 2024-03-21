package fpi.paymentrun.uicomponents.ais;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.helpers.simulator.MockBankHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.paymentrun.models.GetLinkedAccountsResponseModel;
import fpi.paymentrun.models.LinkedAccountResponseModel;
import fpi.paymentrun.models.ReAuthoriseAisConsentModel;
import fpi.paymentrun.services.uicomponents.AisUxComponentService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class ReAuthoriseAisConsentTests extends BasePaymentRunSetup {
    private String buyerToken;
    private LinkedAccountResponseModel linkedAccount;
    private String consentId;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeEach
    public void Setup() throws MalformedURLException, URISyntaxException {
        buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        linkedAccount = InstrumentsHelper.createLinkedAccount(buyerToken, secretKey, sharedKey)
                .extract().as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(0);
        consentId = AisUxComponentHelper.getConsentId(buyerToken, sharedKey, linkedAccount.getId());
    }

    @Test
    public void ReAuthoriseAisConsent_AisFlow_NewLinkedAccountCreated() throws MalformedURLException, URISyntaxException {
        InstrumentsHelper.getLinkedAccounts(secretKey, buyerToken)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        final String reAuthUrl = AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .extract()
                .jsonPath()
                .get("authorisationUrl");

        MockBankHelper.mockBankAis(reAuthUrl);

        InstrumentsHelper.getLinkedAccounts(secretKey, buyerToken)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ReAuthoriseAisConsent_ValidRoleBuyerToken_Success() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void ReAuthoriseAisConsent_MultipleRolesBuyerToken_Success() {
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void ReAuthoriseAisConsent_ValidRoleAuthUserToken_Success() {
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, authUserToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void ReAuthoriseAisConsent_MultipleRolesAuthUserToken_Success() {
        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, authUserToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void ReAuthoriseAisConsent_AdminRoleBuyerToken_Forbidden() {
        BuyersHelper.assignAdminRole(secretKey, buyerToken);

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReAuthoriseAisConsent_IncorrectRoleBuyerToken_Forbidden() {
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReAuthoriseAisConsent_IncorrectRoleAuthUserToken_Forbidden() {
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, authUserToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReAuthoriseAisConsent_NoRedirectUrl_BadRequest() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel()
                        .redirectUrl(null)
                        .build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", CoreMatchers.equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("redirectUrl"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", CoreMatchers.equalTo(1));
    }

    @Test
    public void ReAuthoriseAisConsent_NoState_Success() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel()
                        .state(null)
                        .build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void ReAuthoriseAisConsent_WrongRedirectUrlFormat_Conflict() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel()
                        .redirectUrl(RandomStringUtils.randomAlphabetic(6))
                        .build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_REDIRECT_URL"));
    }

    @Test
    public void ReAuthoriseAisConsent_WrongToken_Unauthorized() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, RandomStringUtils.randomAlphabetic(18), sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReAuthoriseAisConsent_NoToken_Unauthorized() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, null, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReAuthoriseAisConsent_InvalidSharedKey_Unauthorized() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReAuthoriseAisConsent_InvalidConsentId_Unauthorized() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, "abcü123", buyerToken, sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReAuthoriseAisConsent_NoConsentId_Unauthorized() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, "", buyerToken, sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReAuthoriseAisConsent_WrongConsentId_NotFound() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, "6516ae13922242686c6041d8", buyerToken, sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReAuthoriseAisConsent_DifferentProgrammeSharedKey_Unauthorized() {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static Pair<String, String> createBuyer() {
        return BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

}
