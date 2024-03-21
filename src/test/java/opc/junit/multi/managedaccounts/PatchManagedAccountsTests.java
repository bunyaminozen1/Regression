package opc.junit.multi.managedaccounts;

import commons.enums.Currency;
import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedaccounts.PatchManagedAccountModel;
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

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PatchManagedAccountsTests extends BaseManagedAccountsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static String corporateManagedAccountId;
    private static String consumerManagedAccountId;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateManagedAccountId =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken).getLeft();

        consumerManagedAccountId =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken).getLeft();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedAccount_Corporate_Success(final Currency currency) {
        final String managedAccountId =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken).getLeft();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(currency.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedAccount_Consumer_Success(final Currency currency) {
        final String managedAccountId =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationToken).getLeft();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(currency.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedAccount_CorporateUnderNonFosEnabledTenant_Success(final Currency currency) {


        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(nonFpsEnabledTenantDetails.getCorporatesProfileId(),
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        currency.name(), nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(currency.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedAccount_ConsumerUnderNonFosEnabledTenant_Success(final Currency currency) {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(nonFpsEnabledTenantDetails.getConsumersProfileId(),
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        currency.name(), nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId,
                consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(currency.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedAccount_RequiredOnly_Success(final Currency currency) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationToken);

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(null,
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(managedAccount.getRight().getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(currency.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void PatchManagedAccount_AccountNotActive_Success() {
        final String managedAccountId =
                createPendingApprovalManagedAccount(corporateManagedAccountProfileId, corporateAuthenticationToken).getLeft();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(Currency.GBP.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("BLOCKED"))
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedAccount_SameCall_Success(final Currency currency) {

        final String managedAccountId =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationToken).getLeft();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphabetic(5),
                        RandomStringUtils.randomAlphabetic(5));

        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId,
                consumerAuthenticationToken));
        responses.add(ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId,
                consumerAuthenticationToken));

        responses.forEach(response -> response.then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(currency.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("creationTimestamp", notNullValue()));
    }

    @Test
    public void PatchManagedAccount_NoFriendlyName_BadRequest() {
        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphabetic(5),
                        "");

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, consumerManagedAccountId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedAccount_InvalidApiKey_Unauthorised(){
        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, "abc", corporateManagedAccountId, corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedAccount_NoApiKey_BadRequest(){
        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, "", corporateManagedAccountId, corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, consumerManagedAccountId, consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedAccount_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, corporateManagedAccountId, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedAccount_UnknownManagedAccountId_NotFound() {
        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService
                .patchManagedAccount(patchManagedAccountModel, secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedAccount_CrossIdentityManagedAccountId_NotFound() {
        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, consumerManagedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("PatchManagedAccount_NoManagedAccountId_MethodNotAllowed - DEV-2808 opened to return 404")
    public void PatchManagedAccount_NoManagedAccountId_MethodNotAllowed() {
        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, "", consumerAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void PatchManagedAccount_BackofficeCorporateImpersonator_Forbidden() {
        final String managedAccountId =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken).getLeft();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId,
                getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedAccount_BackofficeConsumerImpersonator_Forbidden() {
        final String managedAccountId =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken).getLeft();

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKey, managedAccountId,
                getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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
