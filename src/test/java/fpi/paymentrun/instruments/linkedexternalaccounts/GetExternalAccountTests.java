package fpi.paymentrun.instruments.linkedexternalaccounts;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.simulator.SimulateLinkedAccountModel;
import fpi.paymentrun.services.InstrumentsService;
import opc.services.multiprivate.MultiPrivateService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_LINKED_EXTERNAL_ACCOUNTS)
public class GetExternalAccountTests extends BasePaymentRunSetup {

    private static String buyerId;
    private static String buyerToken;
    private static String buyerCurrency;
    private static String institutionId;
    private static Pair<String, SimulateLinkedAccountModel> linkedAccount;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeAll
    public static void Setup() {
        controllerRoleBuyerSetup();
        institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        linkedAccount = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey);
    }

    @Test
    public void GetLinkedAccount_ValidRoleBuyer_Success() {
        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccount.getLeft()))
                .body("accountIdentification.sortCode", equalTo(linkedAccount.getRight().getAccountIdentification().getSortCode()))
                .body("accountIdentification.accountNumber", equalTo(linkedAccount.getRight().getAccountIdentification().getAccountNumber()))
                .body("currency", equalTo(buyerCurrency))
                .body("institution.id", equalTo(institutionId));
    }

    @Test
    public void GetLinkedAccount_ValidRoleAuthUser_Success() {
        final String userToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, userToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccount.getLeft()))
                .body("accountIdentification.sortCode", equalTo(linkedAccount.getRight().getAccountIdentification().getSortCode()))
                .body("accountIdentification.accountNumber", equalTo(linkedAccount.getRight().getAccountIdentification().getAccountNumber()))
                .body("currency", equalTo(buyerCurrency))
                .body("institution.id", equalTo(institutionId));
    }

    @Test
    public void GetLinkedAccount_MultipleRolesAuthUser_Success() {
        final String userToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, userToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(linkedAccount.getLeft()))
                .body("accountIdentification.sortCode", equalTo(linkedAccount.getRight().getAccountIdentification().getSortCode()))
                .body("accountIdentification.accountNumber", equalTo(linkedAccount.getRight().getAccountIdentification().getAccountNumber()))
                .body("currency", equalTo(buyerCurrency))
                .body("institution.id", equalTo(institutionId));
    }

    @Test
    public void GetLinkedAccount_IncorrectRoleAuthUser_Forbidden() {
        final String userToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetLinkedAccount_IncorrectRoleBuyer_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String creatorBuyerToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, creatorBuyerToken);

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, creatorBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetLinkedAccount_AdminRoleBuyer_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String adminBuyerToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, adminBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetLinkedAccount_CrossIdentityBuyer_NotFound() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String newBuyerToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignControllerRole(secretKey, newBuyerToken);

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, newBuyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetLinkedAccount_CrossIdentityAuthUser_NotFound() {
        final String newBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String userToken = getControllerRoleAuthUserToken(secretKey, newBuyerToken);

        InstrumentsService.getLinkedAccount(linkedAccount.getLeft(), secretKey, userToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetLinkedAccount_WrongLinkedAccountId_NotFound() {

        InstrumentsService.getLinkedAccount("652d54fa9ed567207a7a9d65", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetLinkedAccount_InvalidLinkedAccountId_BadRequest() {

        InstrumentsService.getLinkedAccount(RandomStringUtils.randomAlphanumeric(24), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linked_account_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void GetLinkedAccount_InvalidApiKey_Unauthorised() {
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        InstrumentsService.getLinkedAccount(linkedAccountId, RandomStringUtils.randomAlphabetic(6), buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetLinkedAccount_InvalidToken_Unauthorised() {
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetLinkedAccount_NoToken_Unauthorised() {
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetLinkedAccount_BuyerLogout_Unauthorised() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        final String buyerId = buyer.getLeft();
        final String buyerToken = buyer.getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        AuthenticationHelper.logout(secretKey, buyerToken);
        MultiPrivateService.validateAccessToken(buyerToken, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);

        InstrumentsService.getLinkedAccount(linkedAccountId, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static void controllerRoleBuyerSetup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(createBuyerModel, secretKey);
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
        buyerCurrency = createBuyerModel.getBaseCurrency();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
    }
}
