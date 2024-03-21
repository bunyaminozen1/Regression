package opc.junit.multi.managedaccounts;

import opc.enums.opc.BlockType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;

public class GetManagedAccountTests extends BaseManagedAccountsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static List<Pair<String, CreateManagedAccountModel>> corporateManagedAccounts;
    private static List<Pair<String, CreateManagedAccountModel>> consumerManagedAccounts;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateManagedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        consumerManagedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);
    }

    @Test
    public void GetManagedAccount_Corporate_Success(){

        final Pair<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.get(0);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
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

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
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
    public void GetManagedAccount_CorporateUnderNonFpsEnabledTenant_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenantDetails.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        createCorporateModel.getBaseCurrency()).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_ConsumerUnderNonFpsEnabledTenant_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsEnabledTenantDetails.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        createConsumerModel.getBaseCurrency()).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createConsumerModel.getBaseCurrency()))
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

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
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

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
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
    public void GetManagedAccount_ManagedAccountInactive_Success(){

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createPendingApprovalManagedAccount(consumerManagedAccountProfileId, consumerAuthenticationToken);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(managedAccount.getRight().getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("BLOCKED"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_UnknownManagedAccountId_NotFound(){

        ManagedAccountsService.getManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_InvalidApiKey_Unauthorised(){
        ManagedAccountsService.getManagedAccount("abc", consumerManagedAccounts.get(0).getLeft(), consumerAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccount_NoApiKey_BadRequest(){
        ManagedAccountsService.getManagedAccount("", consumerManagedAccounts.get(0).getLeft(), consumerAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService.getManagedAccount(secretKey, corporateManagedAccounts.get(0).getLeft(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccount_RootUserLoggedOut_Unauthorised(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ManagedAccountsService.getManagedAccount(secretKey, consumerManagedAccounts.get(0).getLeft(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccount_CrossIdentityChecks_NotFound(){

        ManagedAccountsService.getManagedAccount(secretKey, corporateManagedAccounts.get(0).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_BackofficeCorporateImpersonator_Forbidden(){

        final Pair<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.get(0);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccount_BackofficeConsumerImpersonator_Forbidden(){

        final Pair<String, CreateManagedAccountModel> managedAccount = consumerManagedAccounts.get(0);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest
    @EnumSource(BlockType.class)
    public void GetManagedAccount_ManagedAccountBlocked_Success(final BlockType blockType){

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        AdminHelper.blockManagedAccount(managedAccount.getLeft(), blockType, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(managedAccount.getRight().getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo(blockType.equals(BlockType.USER) ? "USER" : "SYSTEM"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
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
