package opc.junit.multi.managedaccounts;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
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

public class RemoveManagedAccountsTests extends BaseManagedAccountsSetup {

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
    public void RemoveManagedAccount_Corporate_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedAccount_Consumer_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedAccount_CorporateUnderNonFosEnabledTenant_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenantDetails.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        createCorporateModel.getBaseCurrency(), nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        ManagedAccountsService.removeManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedAccount_ConsumerUnderNonFosEnabledTenant_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsEnabledTenantDetails.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        createConsumerModel.getBaseCurrency(), nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        ManagedAccountsService.removeManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedAccount_BlockedInstrument_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedAccount_AccountNotActive_InstrumentHasPendingActions() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createPendingApprovalManagedAccount(consumerManagedAccountProfileId, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_HAS_PENDING_ACTIONS"));
    }

    @Test
    public void RemoveManagedAccount_AlreadyDestroyed_InstrumentAlreadyRemoved() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REMOVED"));
    }

    @Test
    public void RemoveManagedAccount_ManagedAccountWithActiveLinkedDebitCards_InstrumentHasLinkedCards() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreateDebitManagedCardModel(consumerDebitManagedCardsProfileId, managedAccount.getLeft()).build();
        ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_HAS_LINKED_CARDS"));
    }

    @Test
    public void RemoveManagedAccount_ManagedAccountWithBlockedLinkedDebitCards_InstrumentHasLinkedCards() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreateDebitManagedCardModel(consumerDebitManagedCardsProfileId, managedAccount.getLeft()).build();
        final String managedCardId = ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_HAS_LINKED_CARDS"));
    }

    @Test
    public void RemoveManagedAccount_ManagedAccountWithDestroyedLinkedDebitCards_Success() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreateDebitManagedCardModel(consumerDebitManagedCardsProfileId, managedAccount.getLeft()).build();
        final String managedCardId = ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedAccount_InvalidApiKey_Unauthorised(){
        ManagedAccountsService.removeManagedAccount("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RemoveManagedAccount_NoApiKey_BadRequest(){
        ManagedAccountsService.removeManagedAccount("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void RemoveManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService.removeManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void RemoveManagedAccount_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedAccountsService.removeManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RemoveManagedAccount_UnknownManagedAccountId_NotFound() {
        ManagedAccountsService.removeManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("RemoveManagedAccount_NoManagedAccountId_NotFound - DEV-2808 opened to return 404")
    public void RemoveManagedAccount_NoManagedAccountId_NotFound() {
        ManagedAccountsService.removeManagedAccount(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void RemoveManagedAccount_CrossIdentityCheck_NotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void RemoveManagedAccount_InstrumentWithFunds_BalanceNotZero() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccount.getLeft(),
                corporateCurrency,
                1000L,
                secretKey,
                corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BALANCE_NOT_ZERO"));

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void RemoveManagedAccount_BackofficeCorporateImpersonator_Forbidden() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void RemoveManagedAccount_BackofficeConsumerImpersonator_Forbidden() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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