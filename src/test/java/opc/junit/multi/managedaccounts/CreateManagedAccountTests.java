package opc.junit.multi.managedaccounts;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.admin.UpdateKybModel;
import opc.models.admin.UpdateKycModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CreateManagedAccountTests extends BaseManagedAccountsSetup {

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
    public void CreateManagedAccount_Corporate_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_Consumer_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void CreateManagedAccount_ApprovedGBPAccount_Success() {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                Currency.GBP)
                        .build();

        final ValidatableResponse response =
                ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsHelper.getManagedAccount(secretKey,
                        response.extract().jsonPath().get("id"), consumerAuthenticationToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("state.state").equals("ACTIVE"),
                Optional.of("Expecting 200 with a managed account in state ACTIVE, check logged response"));

                response.body("id", notNullValue())
                        .body("profileId", equalTo(consumerManagedAccountProfileId))
                        .body("tag", equalTo(createManagedAccountModel.getTag()))
                        .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                        .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0))
                        .body("state.state", equalTo("BLOCKED"))
                        .body("bankAccountDetails", nullValue())
                        .body("creationTimestamp", notNullValue());
    }



    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_CorporateUnderNonFpsEnabledTenant_Success(final Currency currency) {

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(nonFpsEnabledTenantDetails.getCorporatesProfileId(),
                        nonFpsEnabledTenantDetails.getSecretKey());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_ConsumerUnderNonFpsEnabledTenant_Success(final Currency currency) {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(nonFpsEnabledTenantDetails.getConsumersProfileId(),
                        nonFpsEnabledTenantDetails.getSecretKey());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, nonFpsEnabledTenantDetails.getSecretKey(),
                consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo("ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_RequiredOnly_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                currency)
                        .setTag(null)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void CreateManagedAccount_SameIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(corporateManagedAccountProfileId))
                        .body("tag", equalTo(createManagedAccountModel.getTag()))
                        .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                        .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0))
                        .body("state.state", equalTo(createCorporateModel.getBaseCurrency().equals(Currency.GBP.name()) ?
                                "BLOCKED" : "ACTIVE"))
                        .body("bankAccountDetails", nullValue())
                        .body("creationTimestamp", notNullValue()));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1", ManagedAccountsHelper.getManagedAccounts(secretKey, authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    @DisplayName("CreateManagedAccount_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void CreateManagedAccount_SameIdempotencyRefDifferentPayload_PreconditionFailed() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        final CreateManagedAccountModel createManagedAccountModel1 =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)).then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(corporateManagedAccountProfileId))
                        .body("tag", equalTo(createManagedAccountModel.getTag()))
                        .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                        .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0))
                        .body("state.state", equalTo("ACTIVE"))
                        .body("creationTimestamp", notNullValue());

        ManagedAccountsService.createManagedAccount(createManagedAccountModel1,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)).then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedAccount_DifferentIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)));
        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.empty()));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(corporateManagedAccountProfileId))
                        .body("tag", equalTo(createManagedAccountModel.getTag()))
                        .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                        .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0))
                        .body("state.state", equalTo(createCorporateModel.getBaseCurrency().equals(Currency.GBP.name())
                                ? "BLOCKED" : "ACTIVE"))
                        .body("bankAccountDetails", nullValue())
                        .body("creationTimestamp", notNullValue()));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3", ManagedAccountsHelper.getManagedAccounts(secretKey, authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedAccount_DifferentIdempotencyRefDifferentPayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                        .build();
        final CreateManagedAccountModel createManagedAccountModel1 =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final Map<Response, CreateManagedAccountModel> responses = new HashMap<>();

        responses.put(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                createManagedAccountModel);
        responses.put(ManagedAccountsService.createManagedAccount(createManagedAccountModel1,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)),
                createManagedAccountModel1);
        responses.put(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.empty()),
                createManagedAccountModel);

        responses.forEach((key, value) ->

            key.then()
                    .statusCode(SC_OK)
                    .body("id", notNullValue())
                    .body("profileId", equalTo(corporateManagedAccountProfileId))
                    .body("tag", equalTo(value.getTag()))
                    .body("friendlyName", equalTo(value.getFriendlyName()))
                    .body("currency", equalTo(value.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0))
                    .body("state.state", equalTo(createCorporateModel.getBaseCurrency().equals(Currency.GBP.name()) ? "BLOCKED" : "ACTIVE"))
                    .body("bankAccountDetails", nullValue())
                    .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertNotEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertNotEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responseList.get(1).jsonPath().getString("id"), responseList.get(2).jsonPath().getString("id"));
        assertNotEquals(responseList.get(1).jsonPath().getString("creationTimestamp"), responseList.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedAccount_LongIdempotencyRef_RequestTooLong() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
                .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_REQUEST_TOO_LONG);
    }

    @Test
    public void CreateManagedAccount_ExpiredIdempotencyRef_NewRequestSuccess() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        TimeUnit.SECONDS.sleep(18);

        responses.add(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response -> {

            response.then()
                    .statusCode(SC_OK)
                    .body("id", notNullValue())
                    .body("profileId", equalTo(corporateManagedAccountProfileId))
                    .body("tag", equalTo(createManagedAccountModel.getTag()))
                    .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                    .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0))
                    .body("state.state", equalTo(createCorporateModel.getBaseCurrency().equals(Currency.GBP.name()) ? "BLOCKED" : "ACTIVE"))
                    .body("bankAccountDetails", nullValue())
                    .body("creationTimestamp", notNullValue());
        });

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("2",
                ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedAccount_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel.Builder createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency());

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel.setProfileId("123").build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_CONFLICT);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel.setProfileId(corporateManagedAccountProfileId).build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        assertEquals("1",
                ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedAccount_SameIdempotencyRefSamePayloadWithChange_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final Map<Response, State> responses = new HashMap<>();

        final Response initialResponse =
                ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference));

        responses.put(initialResponse, State.ACTIVE);

        ManagedAccountsHelper.removeManagedAccount(initialResponse.jsonPath().getString("id"), secretKey, authenticatedCorporate.getRight());

        responses.put(ManagedAccountsService.createManagedAccount(createManagedAccountModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                State.DESTROYED);

        responses.forEach((key, value) ->

            key.then()
                    .statusCode(SC_OK)
                    .body("id", notNullValue())
                    .body("profileId", equalTo(corporateManagedAccountProfileId))
                    .body("tag", equalTo(createManagedAccountModel.getTag()))
                    .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                    .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0))
                    .body("state.state", equalTo(value.name()))
                    .body("bankAccountDetails", nullValue())
                    .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedAccount_CorporateNotVerified_OwnerIdentityNotVerified() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_CorporateEmailNotVerified_OwnerIdentityNotVerified(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_CorporateMobileNumberNotVerified_OwnerIdentityNotVerified(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_CorporateOnlyKYCApproved_OwnerIdentityNotVerified(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_STARTED", "INITIATED", "PENDING_REVIEW", "REJECTED"})
    public void CreateManagedAccount_CorporateKYCNotApproved_OwnerIdentityNotVerified(final String kybStatus){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified(kybStatus)
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_CorporateGradualVerifyingInOneSession_Success(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);


//KYC: approved, email: false, mobile: false
        UpdateKybModel updateKybModelKycApproved = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();
        AdminHelper.updateCorporateKyb(updateKybModelKycApproved, authenticatedCorporate.getLeft(), adminToken);
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                createCorporateModel.getBaseCurrency())
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: false
        UpdateKybModel updateKybModelEmailVerified = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();
        AdminHelper.updateCorporateKyb(updateKybModelEmailVerified, authenticatedCorporate.getLeft(), adminToken);
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: true
        UpdateKybModel updateKybModelFullVerified = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified("APPROVED")
                .build();
        AdminHelper.updateCorporateKyb(updateKybModelFullVerified, authenticatedCorporate.getLeft(), adminToken);
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedAccount_ConsumerNotVerified_OwnerIdentityNotVerified() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                createConsumerModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_ConsumerEmailNotVerified_OwnerIdentityNotVerified(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(true)
                .setFullDueDiligence("APPROVED")
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                createConsumerModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_ConsumerMobileNumberNotVerified_OwnerIdentityNotVerified(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                createConsumerModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_ConsumerOnlyKYCApproved_OwnerIdentityNotVerified(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                createConsumerModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_STARTED", "INITIATED", "PENDING_REVIEW", "REJECTED"})
    public void CreateManagedAccount_ConsumerKYCNotApproved_OwnerIdentityNotVerified(final String kycStatus){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(true)
                .setFullDueDiligence(kycStatus)
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                createConsumerModel.getBaseCurrency())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedAccount_ConsumerGradualVerifyingInOneSession_Success(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

//KYC: approved, email: false, mobile: false
        UpdateKycModel updateKycModelKycApproved = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();
        AdminHelper.updateConsumerKyc(updateKycModelKycApproved, authenticatedConsumer.getLeft(), adminToken);
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                createConsumerModel.getBaseCurrency())
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: false
        UpdateKycModel updateKycModelEmailVerified = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();
        AdminHelper.updateConsumerKyc(updateKycModelEmailVerified, authenticatedConsumer.getLeft(), adminToken);
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: true
        UpdateKycModel updateKycModelFullVerified = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(true)
                .setFullDueDiligence("APPROVED")
                .build();
        AdminHelper.updateConsumerKyc(updateKycModelFullVerified, authenticatedConsumer.getLeft(), adminToken);
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedAccount_InvalidApiKey_Unauthorised(){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                corporateCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, "abc", corporateAuthenticationToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateManagedAccount_NoApiKey_BadRequest(){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                corporateCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, "", corporateAuthenticationToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedAccount_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                consumerCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedAccount_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                corporateCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateManagedAccount_UnknownManagedAccountProfileIdProfileNotFound_Conflict() {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(RandomStringUtils.randomNumeric(18),
                                consumerCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void CreateManagedAccount_CrossIdentityManagedAccountProfileId_ModelConstraintsViolated() {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                consumerCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MODEL_CONSTRAINTS_VIOLATED"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedAccount_NoManagedAccountProfileId_BadRequest(final String managedAccountProfileId) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                corporateCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedAccount_NoCurrency_BadRequest(final String currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EU", "EURO"})
    public void CreateManagedAccount_InvalidCurrency_BadRequest(final String currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Disabled
    @Test
    @DisplayName("CreateManagedAccount_UnknownCurrency_CurrencyNotSupported - will be fixed by DEV-2807")
    public void CreateManagedAccount_UnknownCurrency_CurrencyNotSupported() {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                "ABC")
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_NOT_SUPPORTED_BY_PROFILE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedAccount_NoFriendlyName_BadRequest(final String friendlyName) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, corporateCurrency)
                        .setFriendlyName(friendlyName)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedAccount_BackofficeCorporateImpersonator_Forbidden() {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                corporateCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedAccount_BackofficeConsumerImpersonator_Forbidden() {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                consumerCurrency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER), Optional.empty())
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
