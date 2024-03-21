package fpi.paymentrun.uicomponents.ais;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.paymentrun.models.GetLinkedAccountsResponseModel;
import fpi.paymentrun.models.LinkedAccountResponseModel;
import fpi.paymentrun.services.uicomponents.AisUxComponentService;
import io.restassured.response.ValidatableResponse;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class GetAisConsentLinkedAccountInfoTests extends BasePaymentRunSetup {
    private String buyerToken;
    private LinkedAccountResponseModel linkedAccount;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeEach
    public void Setup() throws MalformedURLException, URISyntaxException {
        buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        linkedAccount = InstrumentsHelper.createLinkedAccount(buyerToken, secretKey, sharedKey)
                .extract().as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(0);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_ValidRoleBuyerToken_Success() {

        final ValidatableResponse getLinkedAccountInfo = AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_OK);
        assertResponse(linkedAccount, getLinkedAccountInfo);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_MultipleRolesBuyerToken_Success() {
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        final ValidatableResponse getLinkedAccountInfo = AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_OK);
        assertResponse(linkedAccount, getLinkedAccountInfo);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_ValidRoleAuthUserToken_Success() {

        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse getLinkedAccountInfo = AisUxComponentService.getAisConsentLinkedAccountInfo(authUserToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_OK);
        assertResponse(linkedAccount, getLinkedAccountInfo);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_MultipleRolesAuthUserToken_Success() {

        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse getLinkedAccountInfo = AisUxComponentService.getAisConsentLinkedAccountInfo(authUserToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_OK);
        assertResponse(linkedAccount, getLinkedAccountInfo);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_SeveralLinkedAccounts_Success() {
        final List<LinkedAccountResponseModel> linkedAccounts = InstrumentsHelper.createLinkedAccounts(buyerToken, secretKey, sharedKey, 2)
                .extract().as(GetLinkedAccountsResponseModel.class).getLinkedAccounts();

        final ValidatableResponse getFirstLinkedAccountInfo = AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccounts.get(0).getId())
                .then()
                .statusCode(SC_OK);

        final ValidatableResponse getSecondLinkedAccountInfo = AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccounts.get(1).getId())
                .then()
                .statusCode(SC_OK);

        assertResponse(linkedAccounts.get(0), getFirstLinkedAccountInfo);
        assertResponse(linkedAccounts.get(1), getSecondLinkedAccountInfo);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_AdminRoleBuyerToken_Forbidden() {
        BuyersHelper.assignAdminRole(secretKey, buyerToken);

        AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_IncorrectRoleBuyerToken_Forbidden() {
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_IncorrectRoleAuthUserToken_Forbidden() {
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        AisUxComponentService.getAisConsentLinkedAccountInfo(authUserToken, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_NoToken_Unauthorised() {
        AisUxComponentService.getAisConsentLinkedAccountInfo(null, sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_InvalidToken_Unauthorised() {
        AisUxComponentService.getAisConsentLinkedAccountInfo(RandomStringUtils.randomAlphabetic(18), sharedKey, linkedAccount.getId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_InvalidSharedKey_Unauthorised() {
        AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, RandomStringUtils.randomAlphabetic(18), linkedAccount.getId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_DifferentProgrammeSharedKey_Unauthorised() {
        AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKeyPluginsScaApp, linkedAccount.getId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_WrongLinkedAccountId_NotFound() {
        AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, "652d54fa9ed567207a7a9d65")
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));
    }

    @Test
    public void GetAisConsentLinkedAccountInfo_IncorrectLinkedAccountId_BadRequest() {
        AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, RandomStringUtils.randomAlphabetic(24))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", Matchers.equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", CoreMatchers.equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", Matchers.equalTo("linked_account_id"))
                .body("syntaxErrors.invalidFields[0].error", Matchers.equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", CoreMatchers.equalTo(1));
    }

    private void assertResponse(final LinkedAccountResponseModel linkedAccount,
                                final ValidatableResponse response) {
        response.body("linkedAccounts[0].id", equalTo(linkedAccount.getId()))
                .body("linkedAccounts[0].accountIdentification.sortCode", equalTo(linkedAccount.getAccountIdentification().getSortCode()))
                .body("linkedAccounts[0].accountIdentification.accountNumber", equalTo(linkedAccount.getAccountIdentification().getAccountNumber()))
                .body("linkedAccounts[0].currency", equalTo(linkedAccount.getCurrency()))
                .body("linkedAccounts[0].institution.id", equalTo(linkedAccount.getInstitution().getId()))
                .body("linkedAccounts[0].institution.displayName", equalTo(linkedAccount.getInstitution().getDisplayName()))
                .body("linkedAccounts[0].institution.countries", equalTo(linkedAccount.getInstitution().getCountries()))
                .body("linkedAccounts[0].institution.images.logo", equalTo(linkedAccount.getInstitution().getImages().getLogo()))
                .body("linkedAccounts[0].institution.images.icon", equalTo(linkedAccount.getInstitution().getImages().getIcon()))
                .body("linkedAccounts[0].institution.info.loginUrl", equalTo(linkedAccount.getInstitution().getInfo().getLoginUrl()))
                .body("linkedAccounts[0].institution.info.helplinePhoneNumber", equalTo(linkedAccount.getInstitution().getInfo().getHelplinePhoneNumber()))
                .body("linkedAccounts[0].consent.expiresAt", equalTo(linkedAccount.getConsent().getExpiresAt()))
                .body("linkedAccounts[0].consent.status", equalTo(linkedAccount.getConsent().getStatus()))
                .body("linkedAccounts[0].consent.expiresIn", notNullValue())
                .body("linkedAccounts[0].consent.consentId", notNullValue())
                .body("embedder.companyRegistrationName", equalTo("PluginsQAAutomationTesting"));
    }

    private static Pair<String, String> createBuyer() {
        return BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }
}
