package opc.junit.multi.directdebits;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetCollectionTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> corporateMandates;
    private static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> consumerMandates;
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

        corporateMandates =
                createMandatesWithCollections(corporateManagedAccount, corporateCurrency, corporateCollectionAmounts, corporateAuthenticationToken, 1);
        consumerMandates =
                createMandatesWithCollections(consumerManagedAccount, consumerCurrency, consumerCollectionAmounts, consumerAuthenticationToken, 1);
    }

    @Test
    public void GetCollection_Corporate_Success(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(1).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(corporateCurrency))
                .body("amount.amount", equalTo(corporateCollectionAmounts.get(1).intValue()))
                .body("state", equalTo(MandateCollectionState.PENDING.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_Consumer_Success(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = consumerMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(1).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(consumerCurrency))
                .body("amount.amount", equalTo(consumerCollectionAmounts.get(1).intValue()))
                .body("state", equalTo(MandateCollectionState.PENDING.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_CollectionRejected_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(300L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(300))
                .body("state", equalTo(MandateCollectionState.REJECTED.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_CollectionPaid_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createFundedManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(300L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(300))
                .body("state", equalTo(MandateCollectionState.PAID.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_CollectionUnpaid_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(300L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), MandateCollectionState.UNPAID, secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(300))
                .body("state", equalTo(MandateCollectionState.UNPAID.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_CollectionRefundedStillInPaidState_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
        final Pair<String, FasterPaymentsBankDetailsModel> managedAccount =
                createFundedManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandatesWithCollections(managedAccount, createConsumerModel.getBaseCurrency(), Collections.singletonList(300L),
                        consumer.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumer.getRight());

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(300))
                .body("state", equalTo(MandateCollectionState.REJECTED.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_MandateCancelledInternally_Success(){

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

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(301))
                .body("state", equalTo(MandateCollectionState.PENDING.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_MandateCancelledExternally_Success(){

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

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(301))
                .body("state", equalTo(MandateCollectionState.PENDING.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_MandateExpired_Success(){

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

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getValue().get(0).getLeft()))
                .body("mandateId", equalTo(mandate.getKey().getLeft()))
                .body("amount.currency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("amount.amount", equalTo(301))
                .body("state", equalTo(MandateCollectionState.PENDING.name()))
                .body("settlementTimestamp", notNullValue())
                .body("merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getKey().getRight().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("instrumentId.type", equalTo(mandate.getKey().getRight().getAccountId().getType()))
                .body("instrumentId.id", equalTo(mandate.getKey().getRight().getAccountId().getId()));
    }

    @Test
    public void GetCollection_UnknownMandate_NotFound(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, RandomStringUtils.randomNumeric(18), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetCollection_InvalidMandate_BadRequest(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = consumerMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, RandomStringUtils.randomAlphanumeric(6), mandate.getValue().get(0).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetCollection_UnknownCollection_NotFound(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), RandomStringUtils.randomNumeric(6), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetCollection_InvalidCollection_BadRequest(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = consumerMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), RandomStringUtils.randomAlphanumeric(6), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetCollection_InvalidApiKey_Unauthorised(){
        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection("abc", mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCollection_NoApiKey_BadRequest(){
        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection("", mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetCollection_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetCollection_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCollection_CrossIdentityChecks_NotFound(){

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = corporateMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .build())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}