package opc.junit.multi.directdebits;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.DirectDebitsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.directdebit.GetDirectDebitMandatesResponse;
import opc.models.multi.directdebit.GetMandateModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.DirectDebitsService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GetMandatesTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount;
    private static Map<String, SimulateCreateMandateModel> corporateMandates;
    private static Map<String, SimulateCreateMandateModel> consumerMandates;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        final Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount = createFundedManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateMandates = createMandates(corporateManagedAccount, corporateAuthenticationToken, 3);
        consumerMandates = createMandates(consumerManagedAccount, consumerAuthenticationToken, 2);
    }

    @Test
    public void GetMandates_Corporate_Success(){

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(corporateMandates.size()))
                        .body("responseCount", equalTo(corporateMandates.size()));

        final GetDirectDebitMandatesResponse mandates = response.extract().as(GetDirectDebitMandatesResponse.class);

        corporateMandates.forEach((key, value) -> {
            final GetMandateModel retrievedMandate =
                    mandates.getMandate().stream().filter(x -> x.getMerchantName().equals(value.getMerchantName())).collect(Collectors.toList()).get(0);

            assertEquals(key, retrievedMandate.getId());
            assertEquals(corporateOddProfileId, retrievedMandate.getProfileId());
            assertEquals(value.getAccountId().getId(), retrievedMandate.getInstrumentId().getId());
            assertEquals(value.getAccountId().getType(), retrievedMandate.getInstrumentId().getType());
            assertEquals(corporateId, retrievedMandate.getOwnerId().getId());
            assertEquals(OwnerType.CORPORATE.name(), retrievedMandate.getOwnerId().getType());
            assertEquals(value.getMerchantName(), retrievedMandate.getMerchantName());
            assertEquals(value.getMerchantAccountNumber(), retrievedMandate.getMerchantNumber());
            assertEquals(value.getMerchantReference(), retrievedMandate.getMerchantReference());
            assertEquals("ELECTRONIC", retrievedMandate.getType());
            assertEquals(MandateState.ACTIVE.name(), retrievedMandate.getState());
            assertNotNull(retrievedMandate.getCreationTimestamp());
        });
    }

    @Test
    public void GetMandates_Consumer_Success(){

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(consumerMandates.size()))
                        .body("responseCount", equalTo(consumerMandates.size()));

        final GetDirectDebitMandatesResponse mandates = response.extract().as(GetDirectDebitMandatesResponse.class);

        consumerMandates.forEach((key, value) -> {
            final GetMandateModel retrievedMandate =
                    mandates.getMandate().stream().filter(x -> x.getMerchantName().equals(value.getMerchantName())).collect(Collectors.toList()).get(0);

            assertEquals(key, retrievedMandate.getId());
            assertEquals(consumerOddProfileId, retrievedMandate.getProfileId());
            assertEquals(value.getAccountId().getId(), retrievedMandate.getInstrumentId().getId());
            assertEquals(value.getAccountId().getType(), retrievedMandate.getInstrumentId().getType());
            assertEquals(consumerId, retrievedMandate.getOwnerId().getId());
            assertEquals(OwnerType.CONSUMER.name(), retrievedMandate.getOwnerId().getType());
            assertEquals(value.getMerchantName(), retrievedMandate.getMerchantName());
            assertEquals(value.getMerchantAccountNumber(), retrievedMandate.getMerchantNumber());
            assertEquals(value.getMerchantReference(), retrievedMandate.getMerchantReference());
            assertEquals("ELECTRONIC", retrievedMandate.getType());
            assertEquals(MandateState.ACTIVE.name(), retrievedMandate.getState());
            assertNotNull(retrievedMandate.getCreationTimestamp());
        });
    }

    @Test
    public void GetMandates_WithAllFilters_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 2);
        filters.put("idempotency-ref", 1);
        filters.put("profileId", consumerOddProfileId);
        filters.put("instrumentId", consumerManagedAccount.getLeft());
        filters.put("state", Collections.singletonList(State.ACTIVE));
        filters.put("createdFrom", Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandates(secretKey, Optional.of(filters), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(consumerMandates.size()))
                        .body("responseCount", equalTo(consumerMandates.size()));

        final GetDirectDebitMandatesResponse mandates = response.extract().as(GetDirectDebitMandatesResponse.class);

        consumerMandates.forEach((key, value) -> {
            final GetMandateModel retrievedMandate =
                    mandates.getMandate().stream().filter(x -> x.getMerchantName().equals(value.getMerchantName())).collect(Collectors.toList()).get(0);

            assertEquals(key, retrievedMandate.getId());
            assertEquals(consumerOddProfileId, retrievedMandate.getProfileId());
            assertEquals(value.getAccountId().getId(), retrievedMandate.getInstrumentId().getId());
            assertEquals(value.getAccountId().getType(), retrievedMandate.getInstrumentId().getType());
            assertEquals(consumerId, retrievedMandate.getOwnerId().getId());
            assertEquals(OwnerType.CONSUMER.name(), retrievedMandate.getOwnerId().getType());
            assertEquals(value.getMerchantName(), retrievedMandate.getMerchantName());
            assertEquals(value.getMerchantAccountNumber(), retrievedMandate.getMerchantNumber());
            assertEquals(value.getMerchantReference(), retrievedMandate.getMerchantReference());
            assertEquals("ELECTRONIC", retrievedMandate.getType());
            assertEquals(MandateState.ACTIVE.name(), retrievedMandate.getState());
            assertNotNull(retrievedMandate.getCreationTimestamp());
        });
    }

    @Test
    public void GetMandates_FilterByActiveState_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.ACTIVE));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandates(secretKey, Optional.of(filters), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(consumerMandates.size()))
                        .body("responseCount", equalTo(consumerMandates.size()));

        final GetDirectDebitMandatesResponse mandates = response.extract().as(GetDirectDebitMandatesResponse.class);

        consumerMandates.forEach((key, value) -> {
            final GetMandateModel retrievedMandate =
                    mandates.getMandate().stream().filter(x -> x.getMerchantName().equals(value.getMerchantName())).collect(Collectors.toList()).get(0);

            assertEquals(key, retrievedMandate.getId());
            assertEquals(consumerOddProfileId, retrievedMandate.getProfileId());
            assertEquals(value.getAccountId().getId(), retrievedMandate.getInstrumentId().getId());
            assertEquals(value.getAccountId().getType(), retrievedMandate.getInstrumentId().getType());
            assertEquals(consumerId, retrievedMandate.getOwnerId().getId());
            assertEquals(OwnerType.CONSUMER.name(), retrievedMandate.getOwnerId().getType());
            assertEquals(value.getMerchantName(), retrievedMandate.getMerchantName());
            assertEquals(value.getMerchantAccountNumber(), retrievedMandate.getMerchantNumber());
            assertEquals(value.getMerchantReference(), retrievedMandate.getMerchantReference());
            assertEquals("ELECTRONIC", retrievedMandate.getType());
            assertEquals(MandateState.ACTIVE.name(), retrievedMandate.getState());
            assertNotNull(retrievedMandate.getCreationTimestamp());
        });
    }

    @Test
    public void GetMandates_FilterByCancelledStateCancelledExternally_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId,
                createCorporateModel.getBaseCurrency(), corporate.getRight());

        final Map<String, SimulateCreateMandateModel> mandates =
                createMandates(managedAccount, corporate.getRight(), 2);

        final Map.Entry<String, SimulateCreateMandateModel> mandate = mandates.entrySet().stream().findFirst().stream().collect(Collectors.toList()).get(0);

        DirectDebitsHelper.cancelMandateExternally(mandate.getKey(), mandate.getValue().getDdiId(), secretKey, corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateState.CANCELLED));

        DirectDebitsService.getDirectDebitMandates(secretKey, Optional.of(filters), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("mandate[0].id", equalTo(mandate.getKey()))
                .body("mandate[0].profileId", equalTo(corporateOddProfileId))
                .body("mandate[0].instrumentId.id", equalTo(managedAccount.getLeft()))
                .body("mandate[0].instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("mandate[0].ownerId.id", equalTo(corporate.getLeft()))
                .body("mandate[0].ownerId.type", equalTo(OwnerType.CORPORATE.name()))
                .body("mandate[0].merchantName", equalTo(mandate.getValue().getMerchantName()))
                .body("mandate[0].merchantNumber", equalTo(mandate.getValue().getMerchantAccountNumber()))
                .body("mandate[0].merchantReference", equalTo(mandate.getValue().getMerchantReference()))
                .body("mandate[0].type", equalTo("ELECTRONIC"))
                .body("mandate[0].state", equalTo(MandateState.CANCELLED.name()))
                .body("mandate[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetMandates_FilterByCancelledStateCancelledInternally_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId,
                createCorporateModel.getBaseCurrency(), corporate.getRight());

        final Map<String, SimulateCreateMandateModel> mandates =
                createMandates(managedAccount, corporate.getRight(), 2);

        final Map.Entry<String, SimulateCreateMandateModel> mandate = mandates.entrySet().stream().findFirst().stream().collect(Collectors.toList()).get(0);

        DirectDebitsHelper.cancelMandateInternally(mandate.getKey(), secretKey, corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateState.CANCELLED));

        DirectDebitsService.getDirectDebitMandates(secretKey, Optional.of(filters), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("mandate[0].id", equalTo(mandate.getKey()))
                .body("mandate[0].profileId", equalTo(corporateOddProfileId))
                .body("mandate[0].instrumentId.id", equalTo(managedAccount.getLeft()))
                .body("mandate[0].instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("mandate[0].ownerId.id", equalTo(corporate.getLeft()))
                .body("mandate[0].ownerId.type", equalTo(OwnerType.CORPORATE.name()))
                .body("mandate[0].merchantName", equalTo(mandate.getValue().getMerchantName()))
                .body("mandate[0].merchantNumber", equalTo(mandate.getValue().getMerchantAccountNumber()))
                .body("mandate[0].merchantReference", equalTo(mandate.getValue().getMerchantReference()))
                .body("mandate[0].type", equalTo("ELECTRONIC"))
                .body("mandate[0].state", equalTo(MandateState.CANCELLED.name()))
                .body("mandate[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetMandates_FilterByExpiredState_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId,
                createCorporateModel.getBaseCurrency(), corporate.getRight());

        final Map<String, SimulateCreateMandateModel> mandates =
                createMandates(managedAccount, corporate.getRight(), 2);

        final Map.Entry<String, SimulateCreateMandateModel> mandate =
                mandates.entrySet().stream().findFirst().stream().collect(Collectors.toList()).get(0);

        DirectDebitsHelper.expireMandate(mandate.getKey(), mandate.getValue().getDdiId(), secretKey, corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateState.EXPIRED));

        DirectDebitsService.getDirectDebitMandates(secretKey, Optional.of(filters), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("mandate[0].id", equalTo(mandate.getKey()))
                .body("mandate[0].profileId", equalTo(corporateOddProfileId))
                .body("mandate[0].instrumentId.id", equalTo(managedAccount.getLeft()))
                .body("mandate[0].instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("mandate[0].ownerId.id", equalTo(corporate.getLeft()))
                .body("mandate[0].ownerId.type", equalTo(OwnerType.CORPORATE.name()))
                .body("mandate[0].merchantName", equalTo(mandate.getValue().getMerchantName()))
                .body("mandate[0].merchantNumber", equalTo(mandate.getValue().getMerchantAccountNumber()))
                .body("mandate[0].merchantReference", equalTo(mandate.getValue().getMerchantReference()))
                .body("mandate[0].type", equalTo("ELECTRONIC"))
                .body("mandate[0].state", equalTo(MandateState.EXPIRED.name()))
                .body("mandate[0].creationTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetMandates_FilterByUnknownState_GetAllSuccess(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateState.UNKNOWN));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandates(secretKey, Optional.of(filters), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(corporateMandates.size()))
                        .body("responseCount", equalTo(corporateMandates.size()));

        final GetDirectDebitMandatesResponse mandates = response.extract().as(GetDirectDebitMandatesResponse.class);

        corporateMandates.forEach((key, value) -> {
            final GetMandateModel retrievedMandate =
                    mandates.getMandate().stream().filter(x -> x.getMerchantName().equals(value.getMerchantName())).collect(Collectors.toList()).get(0);

            assertEquals(key, retrievedMandate.getId());
            assertEquals(corporateOddProfileId, retrievedMandate.getProfileId());
            assertEquals(value.getAccountId().getId(), retrievedMandate.getInstrumentId().getId());
            assertEquals(value.getAccountId().getType(), retrievedMandate.getInstrumentId().getType());
            assertEquals(corporateId, retrievedMandate.getOwnerId().getId());
            assertEquals(OwnerType.CORPORATE.name(), retrievedMandate.getOwnerId().getType());
            assertEquals(value.getMerchantName(), retrievedMandate.getMerchantName());
            assertEquals(value.getMerchantAccountNumber(), retrievedMandate.getMerchantNumber());
            assertEquals(value.getMerchantReference(), retrievedMandate.getMerchantReference());
            assertEquals("ELECTRONIC", retrievedMandate.getType());
            assertEquals(MandateState.ACTIVE.name(), retrievedMandate.getState());
            assertNotNull(retrievedMandate.getCreationTimestamp());
        });
    }

    @Test
    public void GetMandates_InvalidApiKey_Unauthorised(){
        DirectDebitsService.getDirectDebitMandates("abc", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetMandates_NoApiKey_BadRequest(){
        DirectDebitsService.getDirectDebitMandates("", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetMandates_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetMandates_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.GBP.name()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .build())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}
