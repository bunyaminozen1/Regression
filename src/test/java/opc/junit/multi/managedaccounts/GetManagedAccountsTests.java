package opc.junit.multi.managedaccounts;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.BlockType;
import opc.enums.opc.BlockedReason;
import opc.enums.opc.IdentityType;
import commons.enums.State;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;

public class GetManagedAccountsTests extends BaseManagedAccountsSetup {

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

        Collections.reverse(corporateManagedAccounts);
        Collections.reverse(consumerManagedAccounts);
    }

    @Test
    public void GetManagedAccounts_Corporate_Success(){

        final ValidatableResponse response = ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        for (int i = 0; i < corporateManagedAccounts.size(); i++) {
            response.body(String.format("accounts[%s].id", i), equalTo(corporateManagedAccounts.get(i).getLeft()))
                    .body(String.format("accounts[%s].profileId", i), equalTo(corporateManagedAccountProfileId))
                    .body(String.format("accounts[%s].tag", i), equalTo(corporateManagedAccounts.get(i).getRight().getTag()))
                    .body(String.format("accounts[%s].friendlyName", i), equalTo(corporateManagedAccounts.get(i).getRight().getFriendlyName()))
                    .body(String.format("accounts[%s].currency", i), equalTo(corporateCurrency))
                    .body(String.format("accounts[%s].balances.availableBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].balances.actualBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].state.state", i), equalTo("ACTIVE"))
                    .body(String.format("accounts[%s].bankAccountDetails", i), nullValue())
                    .body(String.format("accounts[%s].creationTimestamp", i), notNullValue());
        }

        response.body("count", equalTo(corporateManagedAccounts.size()))
                .body("responseCount", equalTo(corporateManagedAccounts.size()));
    }

    @Test
    public void GetManagedAccounts_Consumer_Success(){

        final ValidatableResponse response = ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        for (int i = 0; i < consumerManagedAccounts.size(); i++) {
            response.body(String.format("accounts[%s].id", i), equalTo(consumerManagedAccounts.get(i).getLeft()))
                    .body(String.format("accounts[%s].profileId", i), equalTo(consumerManagedAccountProfileId))
                    .body(String.format("accounts[%s].tag", i), equalTo(consumerManagedAccounts.get(i).getRight().getTag()))
                    .body(String.format("accounts[%s].friendlyName", i), equalTo(consumerManagedAccounts.get(i).getRight().getFriendlyName()))
                    .body(String.format("accounts[%s].currency", i), equalTo(consumerCurrency))
                    .body(String.format("accounts[%s].balances.availableBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].balances.actualBalance", i), equalTo(0))
                    .body(String.format("accounts[%s].state.state", i), equalTo("ACTIVE"))
                    .body(String.format("accounts[%s].bankAccountDetails", i), nullValue())
                    .body(String.format("accounts[%s].creationTimestamp", i), notNullValue());
        }

        response.body("count", equalTo(consumerManagedAccounts.size()))
                .body("responseCount", equalTo(consumerManagedAccounts.size()));
    }

    @Test
    public void GetManagedAccounts_WithAllFilters_Success(){
        final Pair<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.get(0);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("profileId", managedAccount.getRight().getProfileId());
        filters.put("friendlyName", managedAccount.getRight().getFriendlyName());
        filters.put("state", Collections.singletonList(State.ACTIVE));
        filters.put("currency", managedAccount.getRight().getCurrency());
        filters.put("createdFrom", Instant.now().minus(10, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("tag", managedAccount.getRight().getTag());

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(managedAccount.getLeft()))
                .body("accounts[0].profileId", equalTo(corporateManagedAccountProfileId))
                .body("accounts[0].tag", equalTo(managedAccount.getRight().getTag()))
                .body("accounts[0].friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("accounts[0].currency", equalTo(corporateCurrency))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("ACTIVE"))
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccounts_LimitFilterCheck_Success(){
        final Pair<String, CreateManagedAccountModel> managedAccount = corporateManagedAccounts.get(0);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(managedAccount.getLeft()))
                .body("accounts[0].profileId", equalTo(corporateManagedAccountProfileId))
                .body("accounts[0].tag", equalTo(managedAccount.getRight().getTag()))
                .body("accounts[0].friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("accounts[0].currency", equalTo(corporateCurrency))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("ACTIVE"))
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(corporateManagedAccounts.size()))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccounts_FilterFromMultipleEntries_Success(){
        final Pair<String, CreateManagedAccountModel> expectedManagedAccount =
                corporateManagedAccounts.get(0);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("friendlyName", expectedManagedAccount.getRight().getFriendlyName());

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(expectedManagedAccount.getLeft()))
                .body("accounts[0].profileId", equalTo(corporateManagedAccountProfileId))
                .body("accounts[0].tag", equalTo(expectedManagedAccount.getRight().getTag()))
                .body("accounts[0].friendlyName", equalTo(expectedManagedAccount.getRight().getFriendlyName()))
                .body("accounts[0].currency", equalTo(corporateCurrency))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("ACTIVE"))
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccounts_NoEntries_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("friendlyName", RandomStringUtils.randomAlphabetic(10));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccounts_FilterByState_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.BLOCKED));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccounts_FilterByDifferentStates_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporate.getRight(), 3);

        ManagedAccountsHelper.blockManagedAccount(managedAccounts.get(0).getLeft(), secretKey, corporate.getRight());
        ManagedAccountsHelper.removeManagedAccount(managedAccounts.get(1).getLeft(), secretKey, corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Arrays.asList(State.ACTIVE, State.BLOCKED, State.DESTROYED));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));
        filters.put("state.destroyedReason", Collections.singletonList(BlockedReason.USER));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(managedAccounts.size()))
                .body("responseCount", equalTo(managedAccounts.size()));
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names =  { "ACTIVE", "DESTROYED" })
    public void GetManagedAccounts_FilterByBlockReasonAndWrongState_BadRequest(final State state){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names =  { "ACTIVE", "BLOCKED" })
    public void GetManagedAccounts_FilterByDestroyedReasonAndWrongState_BadRequest(final State state){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.destroyedReason", Collections.singletonList(BlockedReason.USER));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccounts_InvalidApiKey_Unauthorised(){
        ManagedAccountsService.getManagedAccounts("abc", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccounts_NoApiKey_BadRequest(){
        ManagedAccountsService.getManagedAccounts("", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccounts_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccounts_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccounts_BackofficeCorporateImpersonator_Forbidden(){
        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccounts_BackofficeConsumerImpersonator_Forbidden(){
        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest
    @EnumSource(BlockType.class)
    public void GetManagedAccounts_ManagedAccountBlocked_Success(final BlockType blockType){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String token = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey).getRight();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), token);

        AdminHelper.blockManagedAccount(managedAccount.getLeft(), blockType, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), token)
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(managedAccount.getLeft()))
                .body("accounts[0].profileId", equalTo(corporateManagedAccountProfileId))
                .body("accounts[0].tag", equalTo(managedAccount.getRight().getTag()))
                .body("accounts[0].friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("accounts[0].currency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("BLOCKED"))
                .body("accounts[0].state.blockedReason", equalTo(blockType.equals(BlockType.USER) ? "USER" : "SYSTEM"))
                .body("accounts[0].bankAccountDetails", nullValue())
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @ParameterizedTest
    @EnumSource(BlockType.class)
    public void GetManagedAccounts_ManagedAccountBlockedFilterByState_Success(final BlockType blockType){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String token = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey).getRight();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), token);

        AdminHelper.blockManagedAccount(managedAccount.getLeft(), blockType, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", List.of(State.BLOCKED));
        filters.put("state.blockedReason", Collections.singletonList(blockType));

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.of(filters), token)
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].id", equalTo(managedAccount.getLeft()))
                .body("accounts[0].profileId", equalTo(corporateManagedAccountProfileId))
                .body("accounts[0].tag", equalTo(managedAccount.getRight().getTag()))
                .body("accounts[0].friendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("accounts[0].currency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("accounts[0].balances.availableBalance", equalTo(0))
                .body("accounts[0].balances.actualBalance", equalTo(0))
                .body("accounts[0].state.state", equalTo("BLOCKED"))
                .body("accounts[0].state.blockedReason", equalTo(blockType.equals(BlockType.USER) ? "USER" : "SYSTEM"))
                .body("accounts[0].bankAccountDetails", nullValue())
                .body("accounts[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
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
