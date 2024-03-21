package opc.junit.openbanking.managedaccounts;

import com.google.common.collect.Iterables;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.BlockedReason;
import commons.enums.State;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedAccountsTests extends BaseSetup {

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
                ManagedAccountsHelper.createManagedAccounts(corporateManagedAccountProfileId,
                        corporateCurrency, corporateAuthenticationToken, secretKey, 2);

        consumerManagedAccounts =
                ManagedAccountsHelper.createManagedAccounts(consumerManagedAccountProfileId,
                        consumerCurrency, consumerAuthenticationToken, secretKey, 2);

        corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId));
        consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId,
                corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId,
                consumerConsent);
    }

    @BeforeEach
    public void Setup() throws Exception {
        corporateHeaders = OpenBankingHelper.generateHeaders(clientKeyId,
                ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID,
                        Optional.of(corporateConsent)));
        consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId,
                ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID,
                        Optional.of(consumerConsent)));
    }

    @Test
    public void GetManagedAccount_Corporate_Success() {

        final ValidatableResponse response = AccountInformationService.getManagedAccounts(sharedKey,
                        corporateHeaders, Optional.empty())
                .then()
                .statusCode(SC_OK);

        for (int i = 0; i < corporateManagedAccounts.size(); i++) {
            final Map.Entry<String, CreateManagedAccountModel> managedAccount = Iterables.get(
                    corporateManagedAccounts.entrySet(), i);

            response.body(String.format("accounts[%s].id", i), equalTo(managedAccount.getKey()))
                    .body(String.format("accounts[%s].tag", i), equalTo(managedAccount.getValue().getTag()))
                    .body(String.format("accounts[%s].friendlyName", i),
                            equalTo(managedAccount.getValue().getFriendlyName()))
                    .body(String.format("accounts[%s].currency", i), equalTo(corporateCurrency))
                    .body(String.format("accounts[%s].balances.availableBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].balances.actualBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].state.state", i), equalTo("ACTIVE"))
                    .body(String.format("accounts[%s].creationTimestamp", i), notNullValue());
        }

        response.body("count", equalTo(corporateManagedAccounts.size()))
                .body("responseCount", equalTo(corporateManagedAccounts.size()));
    }

    @Test
    public void GetManagedAccount_Consumer_Success() {

        final ValidatableResponse response = AccountInformationService.getManagedAccounts(sharedKey,
                        consumerHeaders, Optional.empty())
                .then()
                .statusCode(SC_OK);

        for (int i = 0; i < consumerManagedAccounts.size(); i++) {
            final Map.Entry<String, CreateManagedAccountModel> managedAccount = Iterables.get(
                    consumerManagedAccounts.entrySet(), i);

            response.body(String.format("accounts[%s].id", i), equalTo(managedAccount.getKey()))
                    .body(String.format("accounts[%s].tag", i), equalTo(managedAccount.getValue().getTag()))
                    .body(String.format("accounts[%s].friendlyName", i),
                            equalTo(managedAccount.getValue().getFriendlyName()))
                    .body(String.format("accounts[%s].currency", i), equalTo(consumerCurrency))
                    .body(String.format("accounts[%s].balances.availableBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].balances.actualBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].state.state", i), equalTo("ACTIVE"))
                    .body(String.format("accounts[%s].creationTimestamp", i), notNullValue());
        }

        response.body("count", equalTo(consumerManagedAccounts.size()))
                .body("responseCount", equalTo(consumerManagedAccounts.size()));
    }

    @Test
    public void GetManagedAccounts_WithAllFiltersActiveState_Success() {
        final Map.Entry<String, CreateManagedAccountModel> managedAccount = consumerManagedAccounts.entrySet()
                .iterator().next();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("friendlyName", managedAccount.getValue().getFriendlyName());
        filters.put("state", Collections.singletonList(State.ACTIVE));
        filters.put("currency", consumerCurrency);
        filters.put("createdFrom", Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("tag", managedAccount.getValue().getTag());

        AccountInformationService.getManagedAccounts(sharedKey, consumerHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(managedAccount.getKey()))
                .body("accounts[0].tag", equalTo(managedAccount.getValue().getTag()))
                .body("accounts[0].friendlyName", equalTo(managedAccount.getValue().getFriendlyName()))
                .body("accounts[0].currency", equalTo(consumerCurrency))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("ACTIVE"))
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccounts_LimitFilterCheck_Success() {
        final Map.Entry<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.entrySet()
                .iterator().next();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(managedAccount.getKey()))
                .body("accounts[0].tag", equalTo(managedAccount.getValue().getTag()))
                .body("accounts[0].friendlyName", equalTo(managedAccount.getValue().getFriendlyName()))
                .body("accounts[0].currency", equalTo(corporateCurrency))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("ACTIVE"))
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(corporateManagedAccounts.size()))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccounts_FilterFromMultipleEntries_Success() {
        final Map.Entry<String, CreateManagedAccountModel> expectedManagedAccount = corporateManagedAccounts.entrySet()
                .iterator().next();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("friendlyName", expectedManagedAccount.getValue().getFriendlyName());

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(expectedManagedAccount.getKey()))
                .body("accounts[0].tag", equalTo(expectedManagedAccount.getValue().getTag()))
                .body("accounts[0].friendlyName",
                        equalTo(expectedManagedAccount.getValue().getFriendlyName()))
                .body("accounts[0].currency", equalTo(corporateCurrency))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("ACTIVE"))
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccounts_NoEntries_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("friendlyName", RandomStringUtils.randomAlphabetic(10));

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccounts_FilterByState_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.BLOCKED));

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccounts_FilterByDifferentStates_Success() {
        //create consumer
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                corporateProfileId).build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final Map<String, CreateManagedAccountModel> managedAccounts =
                ManagedAccountsHelper.createManagedAccounts(corporateManagedAccountProfileId,
                        createCorporateModel.getBaseCurrency(), corporate.getRight(), secretKey, 2);

        final Map.Entry<String, CreateManagedAccountModel> expectedManagedAccount = Iterables.get(
                managedAccounts.entrySet(), 0);

        ManagedAccountsHelper.blockManagedAccount(expectedManagedAccount.getKey(), secretKey,
                corporate.getRight());

        final Map.Entry<String, CreateManagedAccountModel> expectedManagedAccount1 = Iterables.get(
                managedAccounts.entrySet(), 1);
        ManagedAccountsHelper.removeManagedAccount(expectedManagedAccount1.getKey(), secretKey,
                corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Arrays.asList(State.ACTIVE, State.BLOCKED, State.DESTROYED));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));
        filters.put("state.destroyedReason", Collections.singletonList(BlockedReason.USER));

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(managedAccounts.size()))
                .body("responseCount", equalTo(managedAccounts.size()));
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"ACTIVE", "DESTROYED"})
    public void GetManagedAccounts_FilterByBlockReasonAndWrongState_Conflict(final State state) {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_CONFLICT);

//        SC_BAD_REQUEST??
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"ACTIVE", "BLOCKED"})
    public void GetManagedAccounts_FilterByDestroyedReasonAndWrongState_BadRequest(
            final State state) {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.destroyedReason", Collections.singletonList(BlockedReason.USER));

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void GetManagedAccounts_InvalidSharedKey_Unauthorised() {
        AccountInformationService.getManagedAccounts("123", corporateHeaders, Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccounts_NoSharedKey_BadRequest() {
        AccountInformationService.getManagedAccounts("", corporateHeaders, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccounts_SharedKeyFromAnotherApp_Forbidden() {
        AccountInformationService.getManagedAccounts(sharedKeyAppTwo, corporateHeaders,
                        Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);

//        Expected status code <403> but was <200>.
    }

    @Test
    public void GetManagedAccount_DifferentInnovatorSharedKey_Unauthorised() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("sharedKey");

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccounts_DifferentIdentity_Forbidden() throws Exception {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        secretKey);

        final String consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId));
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumer.getRight(), tppId,
                consumerConsent);
        final Map<String, String> consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId,
                ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID,
                        Optional.of(consumerConsent)));

        final Map.Entry<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.entrySet()
                .iterator().next();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("friendlyName", managedAccount.getValue().getFriendlyName());

        AccountInformationService.getManagedAccounts(sharedKey, consumerHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_FORBIDDEN);

//        Expected status code <403> but was <200>
    }

    @Test
    public void GetManagedAccounts_CrossIdentity_Forbidden() {
        final Map<String, CreateManagedAccountModel> consumerManagedAccounts =
                ManagedAccountsHelper.createManagedAccounts(consumerManagedAccountProfileId,
                        consumerCurrency, consumerAuthenticationToken, secretKey, 2);

        AccountInformationService.getManagedAccounts(sharedKey, corporateHeaders, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

}