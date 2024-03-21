package fpi.paymentrun.instruments.linkedexternalaccounts;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.paymentrun.services.InstrumentsService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_LINKED_EXTERNAL_ACCOUNTS)
public class DeleteExternalAccountTests extends BasePaymentRunSetup {

    /**
     * Required user role: CONTROLLER
     */

    @Test
    public void DeleteLinkedAccount_ValidRoleBuyer_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));
    }

    @Test
    public void DeleteLinkedAccount_MultipleRolesBuyer_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        BuyersHelper.assignAllRoles(secretKey, buyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));
    }

    @Test
    public void DeleteLinkedAccount_ValidRoleAuthUser_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, authUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));
    }

    @Test
    public void DeleteLinkedAccount_MultipleRolesAuthUser_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, authUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));
    }

    @Test
    public void DeleteLinkedAccount_AdminRoleBuyer_Forbidden() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        BuyersHelper.assignAdminRole(secretKey, buyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccountId));
    }

    @Test
    public void DeleteLinkedAccount_IncorrectRoleBuyer_Forbidden() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        BuyersHelper.assignCreatorRole(secretKey, buyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccountId));
    }

    @Test
    public void DeleteLinkedAccount_IncorrectRoleBuyerAuthUser_Forbidden() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, authUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccountId));
    }

    @Test
    public void DeleteLinkedAccount_OtherBuyerToken_LinkedAccountNotFound() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        final String newBuyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, newBuyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, newBuyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccountId));
    }

    @Test
    public void DeleteLinkedAccount_OtherBuyerAuthUserToken_LinkedAccountNotFound() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        final String newBuyerToken = createBuyer().getRight();
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, newBuyerToken);
        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, authUserToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("LINKED_ACCOUNT_NOT_FOUND"))
                .body("message", equalTo("Linked account not found"));

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccountId));
    }

    @Test
    public void DeleteLinkedAccount_WrongLinkedAccountId_NotFound() {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        InstrumentsService.deleteLinkedAccounts("652d54fa9ed567207a7a9d65", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeleteLinkedAccount_InvalidLinkedAccountId_BadRequest() {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        InstrumentsService.deleteLinkedAccounts(RandomStringUtils.randomAlphanumeric(24), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linked_account_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void DeleteLinkedAccount_InvalidApiKey_Unauthorised() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        InstrumentsService.deleteLinkedAccounts(linkedAccountId, RandomStringUtils.randomAlphabetic(6), buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeleteLinkedAccount_InvalidToken_Unauthorised() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeleteLinkedAccount_NoToken_Unauthorised() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeleteLinkedAccount_BuyerLogout_Unauthorised() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);

        AuthenticationHelper.logout(secretKey, buyerToken);

        InstrumentsService.deleteLinkedAccounts(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static Pair<String, String> createBuyer() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        return Pair.of(buyer.getLeft(), buyer.getRight());
    }
}
