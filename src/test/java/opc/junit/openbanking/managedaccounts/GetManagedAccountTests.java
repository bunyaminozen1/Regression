package opc.junit.openbanking.managedaccounts;

import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.innovator.InnovatorService;
import opc.services.openbanking.AccountInformationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedAccountTests extends BaseSetup {

    private static String corporateConsent;
    private static String consumerConsent;
    private static Map<String, String> corporateHeaders;
    private static Map<String, String> consumerHeaders;

    private static Map<String, CreateManagedAccountModel> corporateManagedAccounts;
    private static Map<String, CreateManagedAccountModel> consumerManagedAccounts;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {

        corporateSetup();
        consumerSetup();

        corporateManagedAccounts =
                ManagedAccountsHelper.createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, secretKey,2);

        consumerManagedAccounts =
                ManagedAccountsHelper.createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, secretKey,2);

        corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);
    }

    @BeforeEach
    public void Setup() throws Exception {
        corporateHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(corporateConsent)));
        consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsent)));
    }

    @Test
    public void GetManagedAccount_Corporate_Success() {

        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                corporateManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, corporateHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccount.getKey()))
                .body("tag", equalTo(managedAccount.getValue().getTag()))
                .body("friendlyName", equalTo(managedAccount.getValue().getFriendlyName()))
                .body("currency", equalTo(corporateCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_Consumer_Success() {

        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccount.getKey()))
                .body("tag", equalTo(managedAccount.getValue().getTag()))
                .body("friendlyName", equalTo(managedAccount.getValue().getFriendlyName()))
                .body("currency", equalTo(consumerCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_CorporateUnderNonFpsEnabledTenant_Success() throws Exception {
//        create corporate
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenantDetails.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

//       create consent and headers
        final String corporateConsentNonFpsEnabledTenant = OpenBankingAccountInformationHelper.createConsent(nonFpsEnabledTenantDetails.getSharedKey(), OpenBankingHelper.generateHeaders(clientKeyId));
        OpenBankingSecureServiceHelper.authoriseConsent(nonFpsEnabledTenantDetails.getSharedKey(), corporate.getRight(), tppId, corporateConsentNonFpsEnabledTenant);
        final Map<String, String> corporateHeadersNonFpsEnabledTenant = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(corporateConsentNonFpsEnabledTenant)));

//        create MA
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        createCorporateModel.getBaseCurrency()).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        AccountInformationService.getManagedAccount(nonFpsEnabledTenantDetails.getSharedKey(), corporateHeadersNonFpsEnabledTenant, managedAccountId)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_ConsumerUnderNonFpsEnabledTenant_Success() throws Exception {
//       create consumer
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsEnabledTenantDetails.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

//       create consent and headers
        final String consumerConsentNonFpsEnabledTenant = OpenBankingAccountInformationHelper.createConsent(nonFpsEnabledTenantDetails.getSharedKey(), OpenBankingHelper.generateHeaders(clientKeyId));
        OpenBankingSecureServiceHelper.authoriseConsent(nonFpsEnabledTenantDetails.getSharedKey(), consumer.getRight(), tppId, consumerConsentNonFpsEnabledTenant);
        final Map<String, String> consumerHeadersNonFpsEnabledTenant = OpenBankingHelper.generateHeaders(clientKeyId,
                ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsentNonFpsEnabledTenant)));

//        create MA
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        createConsumerModel.getBaseCurrency()).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        AccountInformationService.getManagedAccount(nonFpsEnabledTenantDetails.getSharedKey(), consumerHeadersNonFpsEnabledTenant, managedAccountId)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_CorporateUpgradedManagedAccount_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        AccountInformationService.getManagedAccount(sharedKey, corporateHeaders, managedAccount.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccount.getLeft()))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(corporateCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());

    }

    @Test
    public void GetManagedAccount_ConsumerUpgradedManagedAccount_Success(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccount.getLeft(), secretKey, consumerAuthenticationToken);

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccount.getLeft()))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(consumerCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_ManagedAccountInactive_Success(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createPendingApprovalManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccount.getLeft()))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("currency", equalTo(consumerCurrency))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("BLOCKED"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetManagedAccount_UnknownManagedAccountId_NotFound(){
        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_InvalidSharedKey_Unauthorised(){
        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount("123", consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccount_NoSharedKey_BadRequest(){
        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount("", consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccount_DifferentInnovatorSharedKey_Unauthorised(){
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("sharedKey");

        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_UNAUTHORIZED);
//        SC_FORBIDDEN??
    }

    @Test
    public void GetManagedAccounts_SharedKeyFromAnotherApp_Forbidden(){
        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKeyAppTwo, consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_UNAUTHORIZED);
//        Expected status code <403> but was <200>.
    }

    @Test
    public void GetManagedAccount_DifferentIdentityHeaders_NotFound() throws Exception {
//create consumer
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        secretKey);
//create consumer consent
        final String consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumer.getRight(), tppId, consumerConsent);
        final Map<String, String> consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsent)));

        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_CrossIdentityHeaders_NotFound(){
        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, corporateHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccount_ExpiredHeaders_Unauthorised() throws Exception {
        final Map<String, String> consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.of("Tue, 16 Aug 2022 13:11:40 GMT"), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsent)));

        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccount_WrongDigest_BadRequest() throws Exception {
        final Map<String, String> consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.of("SHA-256=47DEQpj8HBSa+/TImW+5JWeuQeRkm5NMpJWZG3hSuFU="), TPP_CONSENT_ID, Optional.of(consumerConsent)));

        final Map.Entry<String, CreateManagedAccountModel> managedAccount =
                consumerManagedAccounts.entrySet().iterator().next();

        AccountInformationService.getManagedAccount(sharedKey, consumerHeaders, managedAccount.getKey())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }

    protected static Pair<String, CreateManagedAccountModel> createPendingApprovalManagedAccount(final String managedAccountProfileId,
                                                                                                 final String currency,
                                                                                                 final String authenticationToken){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createPendingApprovalManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }
}
