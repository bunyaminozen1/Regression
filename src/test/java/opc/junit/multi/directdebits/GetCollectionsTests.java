package opc.junit.multi.directdebits;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.DirectDebitsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.directdebit.RejectCollectionModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.DirectDebitsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetCollectionsTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> corporateMandate;
    private static Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> consumerMandate;
    private static List<Long> corporateCollectionAmounts;
    private static List<Long> consumerCollectionAmounts;

    @BeforeAll
    public static void Setup(){

        corporateSetup();
        consumerSetup();

        final Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount = createManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount = createManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateCollectionAmounts = Arrays.asList(100L, 101L);
        consumerCollectionAmounts = Arrays.asList(200L, 201L, 202L);

        final Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> corporateMandates =
                createMandatesWithCollections(corporateManagedAccount, corporateCurrency, corporateCollectionAmounts, corporateAuthenticationToken, 1);
        final Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> consumerMandates =
                createMandatesWithCollections(consumerManagedAccount, consumerCurrency, consumerCollectionAmounts, consumerAuthenticationToken, 1);

        Collections.reverse(corporateCollectionAmounts);
        Collections.reverse(consumerCollectionAmounts);
        corporateMandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();
        consumerMandate = consumerMandates.entrySet().stream().findFirst().orElseThrow();
        Collections.reverse(corporateMandate.getValue());
        Collections.reverse(consumerMandate.getValue());
    }

    @Test
    public void GetCollections_Corporate_Success(){

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, corporateMandate.getKey().getLeft(), Optional.empty(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                .body("count", equalTo(corporateCollectionAmounts.size()))
                .body("responseCount", equalTo(corporateCollectionAmounts.size()));

        IntStream.range(0, corporateCollectionAmounts.size())
                .forEach(i ->
                        response.body(String.format("collection[%s].id", i), equalTo(corporateMandate.getValue().get(i).getLeft()))
                                .body(String.format("collection[%s].mandateId", i), equalTo(corporateMandate.getKey().getLeft()))
                                .body(String.format("collection[%s].amount.currency", i), equalTo(corporateCurrency))
                                .body(String.format("collection[%s].amount.amount", i), equalTo(corporateCollectionAmounts.get(i).intValue()))
                                .body(String.format("collection[%s].state", i), equalTo(MandateCollectionState.PENDING.name()))
                                .body(String.format("collection[%s].settlementTimestamp", i), notNullValue())
                                .body(String.format("collection[%s].merchantName", i), equalTo(corporateMandate.getKey().getRight().getMerchantName()))
                                .body(String.format("collection[%s].merchantNumber", i), equalTo(corporateMandate.getKey().getRight().getMerchantAccountNumber()))
                                .body(String.format("collection[%s].merchantReference", i), equalTo(corporateMandate.getKey().getRight().getMerchantReference()))
                                .body(String.format("collection[%s].instrumentId.type", i), equalTo(corporateMandate.getKey().getRight().getAccountId().getType()))
                                .body(String.format("collection[%s].instrumentId.id", i), equalTo(corporateMandate.getKey().getRight().getAccountId().getId())));
    }

    @Test
    public void GetCollections_Consumer_Success(){

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, consumerMandate.getKey().getLeft(), Optional.empty(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(consumerCollectionAmounts.size()))
                        .body("responseCount", equalTo(consumerCollectionAmounts.size()));

        IntStream.range(0, consumerCollectionAmounts.size())
                .forEach(i ->
                        response.body(String.format("collection[%s].id", i), equalTo(consumerMandate.getValue().get(i).getLeft()))
                                .body(String.format("collection[%s].mandateId", i), equalTo(consumerMandate.getKey().getLeft()))
                                .body(String.format("collection[%s].amount.currency", i), equalTo(consumerCurrency))
                                .body(String.format("collection[%s].amount.amount", i), equalTo(consumerCollectionAmounts.get(i).intValue()))
                                .body(String.format("collection[%s].state", i), equalTo(MandateCollectionState.PENDING.name()))
                                .body(String.format("collection[%s].settlementTimestamp", i), notNullValue())
                                .body(String.format("collection[%s].merchantName", i), equalTo(consumerMandate.getKey().getRight().getMerchantName()))
                                .body(String.format("collection[%s].merchantNumber", i), equalTo(consumerMandate.getKey().getRight().getMerchantAccountNumber()))
                                .body(String.format("collection[%s].merchantReference", i), equalTo(consumerMandate.getKey().getRight().getMerchantReference()))
                                .body(String.format("collection[%s].instrumentId.type", i), equalTo(consumerMandate.getKey().getRight().getAccountId().getType()))
                                .body(String.format("collection[%s].instrumentId.id", i), equalTo(consumerMandate.getKey().getRight().getAccountId().getId())));
    }

    @Test
    public void GetCollections_WithAllFilters_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 5);
        filters.put("state", Collections.singletonList(MandateCollectionState.PENDING));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, consumerMandate.getKey().getLeft(), Optional.of(filters), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(consumerCollectionAmounts.size()))
                        .body("responseCount", equalTo(consumerCollectionAmounts.size()));

        IntStream.range(0, consumerCollectionAmounts.size())
                .forEach(i ->
                        response.body(String.format("collection[%s].id", i), equalTo(consumerMandate.getValue().get(i).getLeft()))
                                .body(String.format("collection[%s].mandateId", i), equalTo(consumerMandate.getKey().getLeft()))
                                .body(String.format("collection[%s].amount.currency", i), equalTo(consumerCurrency))
                                .body(String.format("collection[%s].amount.amount", i), equalTo(consumerCollectionAmounts.get(i).intValue()))
                                .body(String.format("collection[%s].state", i), equalTo(MandateCollectionState.PENDING.name()))
                                .body(String.format("collection[%s].settlementTimestamp", i), notNullValue())
                                .body(String.format("collection[%s].merchantName", i), equalTo(consumerMandate.getKey().getRight().getMerchantName()))
                                .body(String.format("collection[%s].merchantNumber", i), equalTo(consumerMandate.getKey().getRight().getMerchantAccountNumber()))
                                .body(String.format("collection[%s].merchantReference", i), equalTo(consumerMandate.getKey().getRight().getMerchantReference()))
                                .body(String.format("collection[%s].instrumentId.type", i), equalTo(consumerMandate.getKey().getRight().getAccountId().getType()))
                                .body(String.format("collection[%s].instrumentId.id", i), equalTo(consumerMandate.getKey().getRight().getAccountId().getId())));
    }

    @Test
    public void GetCollections_FilterByPendingState_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(301L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateCollectionState.PENDING));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.of(filters), consumer.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.PENDING.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_FilterByPaidState_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createFundedManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(301L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumer.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateCollectionState.PAID));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.of(filters), consumer.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.PAID.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_FilterByUnpaidState_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .build())
                        .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(corporateModulrManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporate.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createCorporateModel.getBaseCurrency(), Collections.singletonList(301L),
                        corporate.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), MandateCollectionState.UNPAID, secretKey, corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateCollectionState.UNPAID));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.of(filters), corporate.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createCorporateModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.UNPAID.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_FilterByRejectedState_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(301L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumer.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateCollectionState.REJECTED));

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.of(filters), consumer.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.REJECTED.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_FilterByUnknownState_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(MandateCollectionState.UNKNOWN));

        DirectDebitsService.getDirectDebitMandateCollections(secretKey, consumerMandate.getKey().getLeft(), Optional.of(filters), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(consumerCollectionAmounts.size()))
                .body("responseCount", equalTo(consumerCollectionAmounts.size()));
    }

    @Test
    public void GetCollections_CrossIdentityChecks_NotFound(){

        DirectDebitsService.getDirectDebitMandateCollections(secretKey, corporateMandate.getKey().getLeft(), Optional.empty(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetCollections_NoEntries_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final String mandate = createMandate(managedAccount, consumer.getRight()).getKey();

        DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate, Optional.empty(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetCollections_MandateCancelledInternally_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(301L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.cancelMandateInternally(mandate.getKey().getLeft(), secretKey, consumer.getRight());

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.empty(), consumer.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.PENDING.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_MandateCancelledExternally_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(301L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.cancelMandateExternally(mandate.getKey().getLeft(), mandate.getKey().getRight().getDdiId(), secretKey, consumer.getRight());

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.empty(), consumer.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.PENDING.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_MandateExpired_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(301L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.expireMandate(mandate.getKey().getLeft(), mandate.getKey().getRight().getDdiId(), secretKey, consumer.getRight());

        final ValidatableResponse response =
                DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandate.getKey().getLeft(), Optional.empty(), consumer.getRight())
                        .then()
                        .statusCode(SC_OK)
                        .body("count", equalTo(1))
                        .body("responseCount", equalTo(1));

        response.body("collection[0].id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("collection[0].mandateId", equalTo(mandate.getKey().getLeft()))
                .body("collection[0].amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("collection[0].amount.amount", equalTo(301))
                .body("collection[0].state", equalTo(MandateCollectionState.PENDING.name()))
                .body("collection[0].settlementTimestamp", notNullValue())
                .body("collection[0].merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("collection[0].merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("collection[0].merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("collection[0].instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("collection[0].instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollections_InvalidApiKey_Unauthorised(){
        DirectDebitsService.getDirectDebitMandateCollections("abc", RandomStringUtils.randomNumeric(18), Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCollections_NoApiKey_BadRequest(){
        DirectDebitsService.getDirectDebitMandateCollections("", RandomStringUtils.randomNumeric(18), Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetCollections_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        DirectDebitsService.getDirectDebitMandateCollections(secretKey, RandomStringUtils.randomNumeric(18), Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetCollections_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        DirectDebitsService.getDirectDebitMandateCollections(secretKey, RandomStringUtils.randomNumeric(18), Optional.empty(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("GBP").build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency("GBP").setCompany(CompanyModel.defaultCompanyModel()
                        .setType(CompanyType.LLC.name())
                        .build()).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}
