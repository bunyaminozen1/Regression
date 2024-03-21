package fpi.paymentrun.instruments.linkedexternalaccounts;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.simulator.SimulateLinkedAccountModel;
import fpi.paymentrun.services.InstrumentsService;
import io.restassured.response.ValidatableResponse;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_LINKED_EXTERNAL_ACCOUNTS)
public class GetExternalAccountsTests extends BasePaymentRunSetup {

    private static String buyerId;
    private static String buyerToken;
    private static String buyerCurrency;
    private static final List<Pair<String, SimulateLinkedAccountModel>> linkedAccounts = new ArrayList<>();

    /**
     * Required user role: CONTROLLER
     */

    @BeforeAll
    public static void Setup() {
        controllerRoleBuyerSetup();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        createLinkedAccounts(buyerId, institutionId);
    }

    @Test
    public void GetLinkedAccounts_ValidRoleBuyer_Success() {

        final ValidatableResponse response = InstrumentsService.getLinkedAccounts(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(response, linkedAccounts);
    }

    @Test
    public void GetLinkedAccounts_ValidRoleAuthUser_Success() {
        final String userToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = InstrumentsService.getLinkedAccounts(secretKey, userToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(response, linkedAccounts);
    }

    @Test
    public void GetLinkedAccounts_MultipleRolesAuthUser_Success() {
        final String multipleRolesAuthUser = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = InstrumentsService.getLinkedAccounts(secretKey, multipleRolesAuthUser)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(response, linkedAccounts);
    }

    @Test
    public void GetLinkedAccounts_CrossIdentityAuthUser_SuccessNoAccounts() {
        final String newBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String userToken = getControllerRoleAuthUserToken(secretKey, newBuyerToken);

        InstrumentsService.getLinkedAccounts(secretKey, userToken)
                .then()
                .statusCode(SC_OK)
                .body("linkedAccounts[0]", equalTo(null));
    }

    @Test
    public void GetLinkedAccounts_CrossIdentityBuyer_SuccessNoAccounts() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        InstrumentsService.getLinkedAccounts(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("linkedAccounts[0]", equalTo(null));
    }

    @Test
    public void GetLinkedAccounts_IncorrectRoleBuyer_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        InstrumentsService.getLinkedAccounts(secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetLinkedAccounts_AdminRoleBuyer_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();

        InstrumentsService.getLinkedAccounts(secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetLinkedAccounts_IncorrectRoleAuthUser_Forbidden() {
        final String creatorUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        InstrumentsService.getLinkedAccounts(secretKey, creatorUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetLinkedAccounts_InvalidApiKey_Unauthorised() {

        InstrumentsService.getLinkedAccounts(RandomStringUtils.randomAlphabetic(6), buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetLinkedAccounts_InvalidToken_Unauthorised() {

        InstrumentsService.getLinkedAccounts(secretKey, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetLinkedAccounts_NoToken_Unauthorised() {

        InstrumentsService.getLinkedAccounts(secretKey, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetLinkedAccounts_BuyerLogout_Unauthorised() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        final String buyerId = buyer.getLeft();
        final String buyerToken = buyer.getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        AuthenticationHelper.logout(secretKey, buyerToken);

        InstrumentsService.getLinkedAccounts(secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static void createLinkedAccounts(final String buyerId,
                                             final String institutionId) {
        IntStream.range(0, 3).forEach(i -> {
            final Pair<String, SimulateLinkedAccountModel> linkedAccount = SimulatorHelper.createLinkedAccount(buyerId, secretKey);
            linkedAccounts.add(linkedAccount);
        });
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final List<Pair<String, SimulateLinkedAccountModel>> linkedAccounts) {
        IntStream.range(0, linkedAccounts.size()).forEach(i ->
                response.body(String.format("linkedAccounts[%s].id", i), equalTo(linkedAccounts.get(i).getLeft()))
                        .body(String.format("linkedAccounts[%s].accountIdentification.sortCode", i), equalTo(linkedAccounts.get(i).getRight().getAccountIdentification().getSortCode()))
                        .body(String.format("linkedAccounts[%s].accountIdentification.accountNumber", i), equalTo(linkedAccounts.get(i).getRight().getAccountIdentification().getAccountNumber()))
                        .body(String.format("linkedAccounts[%s].currency", i), equalTo(buyerCurrency))
                        .body(String.format("linkedAccounts[%s].institution.id", i), equalTo(linkedAccounts.get(i).getRight().getInstitutionId()))
                        .body("count", equalTo(linkedAccounts.size()))
                        .body("responseCount", equalTo(linkedAccounts.size()))
        );
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
