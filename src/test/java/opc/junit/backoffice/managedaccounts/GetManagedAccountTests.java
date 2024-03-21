package opc.junit.backoffice.managedaccounts;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetManagedAccountTests extends BaseManagedAccountsSetup{
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static List<Pair<String, CreateManagedAccountModel>> corporateManagedAccounts;
    private static List<Pair<String, CreateManagedAccountModel>> consumerManagedAccounts;

    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();

        corporateManagedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        consumerManagedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

//        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
        corporateImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(corporateId,IdentityType.CORPORATE, secretKey);
    }

    @Test
    public void GetManagedAccount_Corporate_Success(){

        final Pair<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.get(0);

        BackofficeMultiService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(corporateCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_Consumer_Success(){

        final Pair<String, CreateManagedAccountModel> managedAccount = consumerManagedAccounts.get(0);

        BackofficeMultiService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(consumerCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }


    @Test
    public void GetManagedAccount_CorporateUpgradedManagedAccount_Success(){

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        BackofficeMultiService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(corporateCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_ConsumerUpgradedManagedAccount_Success(){

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccount.getLeft(), secretKey, consumerAuthenticationToken);

        BackofficeMultiService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(consumerCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }


    @Test
    public void GetManagedAccount_UnknownManagedAccountId_NotFound(){

        BackofficeMultiService.getManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), consumerImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_InvalidApiKey_Unauthorised(){
        BackofficeMultiService.getManagedAccount("abc", consumerManagedAccounts.get(0).getLeft(), consumerImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccount_NoApiKey_BadRequest(){
        BackofficeMultiService.getManagedAccount("", consumerManagedAccounts.get(0).getLeft(), consumerImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService.getManagedAccount(secretKey, corporateManagedAccounts.get(0).getLeft(), corporateImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccount_RootUserLoggedOut_Forbidden(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        BackofficeMultiService.getManagedAccount(secretKey, consumerManagedAccounts.get(0).getLeft(), token)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccount_CrossIdentityChecks_NotFound(){

        BackofficeMultiService.getManagedAccount(secretKey, corporateManagedAccounts.get(0).getLeft(), consumerImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_CorporateAuthToken_Forbidden(){

        final Pair<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.get(0);

        BackofficeMultiService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccount_ConsumerAuthToken_Forbidden(){

        final Pair<String, CreateManagedAccountModel> managedAccount = consumerManagedAccounts.get(0);

        BackofficeMultiService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
