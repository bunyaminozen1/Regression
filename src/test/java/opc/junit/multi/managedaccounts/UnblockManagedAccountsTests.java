package opc.junit.multi.managedaccounts;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class UnblockManagedAccountsTests extends BaseManagedAccountsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void UnblockManagedAccount_Corporate_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedAccount_Consumer_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedAccount_CorporateNonFpsEnabledTenant_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenantDetails.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        createCorporateModel.getBaseCurrency(), nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        ManagedAccountsService.blockManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedAccount_ConsumerNonFpsEnabledTenant_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsEnabledTenantDetails.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        createConsumerModel.getBaseCurrency(), nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        ManagedAccountsService.blockManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedAccount_AlreadyUnblocked_InstrumentNotBlocked() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, "GBP", consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_BLOCKED"));
    }

    @Test
    public void UnblockManagedAccount_InActiveState_InstrumentNotBlocked() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_BLOCKED"));
    }

    @Test
    public void UnblockManagedAccount_InvalidApiKey_Unauthorised(){
        ManagedAccountsService.unblockManagedAccount("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UnblockManagedAccount_NoApiKey_BadRequest(){
        ManagedAccountsService.unblockManagedAccount("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UnblockManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService.unblockManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UnblockManagedAccount_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedAccountsService.unblockManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UnblockManagedAccount_UnknownManagedAccountId_NotFound() {
        ManagedAccountsService.unblockManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("UnblockManagedAccount_NoManagedAccountId_NotFound - DEV-2808 opened to return 404")
    public void UnblockManagedAccount_NoManagedAccountId_NotFound() {
        ManagedAccountsService.unblockManagedAccount(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void UnblockManagedAccount_CrossIdentityCheck_NotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void UnlockManagedAccount_InstrumentDestroyed_InstrumentDestroyed() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void UnblockManagedAccount_BackofficeCorporateImpersonator_Forbidden() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UnblockManagedAccount_BackofficeConsumerImpersonator_Forbidden() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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