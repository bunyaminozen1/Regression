package opc.junit.multi.managedaccounts;

import commons.enums.Currency;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class BlockManagedAccountsTests extends BaseManagedAccountsSetup {

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

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void BlockManagedAccount_Corporate_Success(final Currency currency) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void BlockManagedAccount_Consumer_Success(final Currency currency) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void BlockManagedAccount_CorporateUnderNonFpsTenant_Success(final Currency currency) {

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(nonFpsEnabledTenantDetails.getCorporatesProfileId(),
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        currency.name(), nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        ManagedAccountsService.blockManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void BlockManagedAccount_ConsumerUnderNonFpsTenant_Success(final Currency currency) {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(nonFpsEnabledTenantDetails.getConsumersProfileId(),
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        currency.name(), nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        ManagedAccountsService.blockManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void BlockManagedAccount_InstrumentWithFunds_Success(final Currency currency) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccount.getLeft(),
                currency.name(),
                1000L,
                secretKey,
                corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedAccount_AlreadyBlocked_InstrumentAlreadyBlocked() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_BLOCKED"));
    }

    @Test
    public void BlockManagedAccount_AccountNotActive_InstrumentInactive() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createPendingApprovalManagedAccount(consumerManagedAccountProfileId, consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_INACTIVE"));
    }

    @Test
    public void BlockManagedAccount_InvalidApiKey_Unauthorised(){
        ManagedAccountsService.blockManagedAccount("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void BlockManagedAccount_NoApiKey_BadRequest(){
        ManagedAccountsService.blockManagedAccount("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void BlockManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService.blockManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void BlockManagedAccount_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedAccountsService.blockManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void BlockManagedAccount_UnknownManagedAccountId_NotFound() {
        ManagedAccountsService.blockManagedAccount(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("BlockManagedAccount_NoManagedAccountId_NotFound - DEV-2808 opened to return 404")
    public void BlockManagedAccount_NoManagedAccountId_NotFound() {
        ManagedAccountsService.blockManagedAccount(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void BlockManagedAccount_CrossIdentityCheck_NotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void BlockManagedAccount_InstrumentDestroyed_InstrumentDestroyed() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));

        ManagedAccountsService.getManagedAccount(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void BlockManagedAccount_BackofficeCorporateImpersonator_Forbidden() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void BlockManagedAccount_BackofficeConsumerImpersonator_Forbidden() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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