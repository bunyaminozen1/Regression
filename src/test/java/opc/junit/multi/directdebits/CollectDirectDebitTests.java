package opc.junit.multi.directdebits;

import com.google.common.collect.Iterables;
import commons.enums.State;
import opc.enums.opc.*;
import commons.enums.Currency;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.DirectDebitsHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.directdebit.GetDirectDebitMandatesResponse;
import opc.models.multi.directdebit.GetMandateModel;
import opc.models.multi.directdebit.RejectCollectionModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.multi.DirectDebitsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CollectDirectDebitTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount;
    private static Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount;
    private static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> corporateMandates;
    private static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> consumerMandates;
    private static List<Long> corporateCollectionAmounts;
    private static List<Long> consumerCollectionAmounts;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateManagedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount = createFundedManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateCollectionAmounts = Arrays.asList(100L, 11000L, 102L, 103L);
        consumerCollectionAmounts = Arrays.asList(200L, 201L);

        corporateMandates =
                createMandatesWithCollections(corporateManagedAccount, corporateCurrency, corporateCollectionAmounts, corporateAuthenticationToken, 3);
        consumerMandates =
                createMandatesWithCollections(consumerManagedAccount, consumerCurrency, consumerCollectionAmounts, consumerAuthenticationToken, 1);
    }

    @Test
    public void CollectDebit_Corporate_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = Iterables.get(corporateMandates.entrySet(), 0);

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(corporateManagedAccount.getLeft(), secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("OUTGOING_DIRECT_DEBIT_COLLECTION"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(corporateCollectionAmounts.get(0).intValue())))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo((int) (DEPOSIT_AMOUNT - corporateCollectionAmounts.get(0).intValue())))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].transactionFee.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionFee.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) (DEPOSIT_AMOUNT - corporateCollectionAmounts.get(0).intValue())))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact(corporateCollectionAmounts.get(0).intValue())))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) (DEPOSIT_AMOUNT - corporateCollectionAmounts.get(0).intValue())))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact(corporateCollectionAmounts.get(0).intValue())))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.mandateId", equalTo(mandate.getKey().getLeft()))
                .body("entry[0].additionalFields.merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("entry[0].additionalFields.merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].transactionFee.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        final GetDirectDebitMandatesResponse responseMandates = DirectDebitsHelper.getDirectDebitMandates(secretKey, corporateAuthenticationToken)
                .then()
                .extract()
                .as(GetDirectDebitMandatesResponse.class);

        GetMandateModel paidMandate = getPaidMandate(responseMandates);
        assertNotNull(paidMandate.getLastPaymentDate());
        assertNotNull(paidMandate.getLastPaymentAmount());
        assertEquals(corporateCurrency, paidMandate.getLastPaymentAmount().get("currency"));
        assertEquals(corporateCollectionAmounts.get(0).toString(), paidMandate.getLastPaymentAmount().get("amount"));

        DirectDebitsHelper.getDirectDebitMandate(secretKey, paidMandate.getId(), corporateAuthenticationToken)
                .then()
                .body("lastPaymentDate", notNullValue())
                .body("lastPaymentAmount", notNullValue())
                .body("lastPaymentAmount.currency", equalTo(corporateCurrency))
                .body("lastPaymentAmount.amount", equalTo(corporateCollectionAmounts.get(0).intValue()));
    }

    @Test
    public void CollectDebit_Consumer_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = consumerMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(consumerManagedAccount.getLeft(), secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("OUTGOING_DIRECT_DEBIT_COLLECTION"))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        final GetDirectDebitMandatesResponse responseMandates = DirectDebitsHelper.getDirectDebitMandates(secretKey, consumerAuthenticationToken)
                .then()
                .extract()
                .as(GetDirectDebitMandatesResponse.class);

        assertNotNull(responseMandates.getMandate().get(0).getLastPaymentDate());
        assertNotNull(responseMandates.getMandate().get(0).getLastPaymentAmount());
        assertEquals(consumerCurrency, responseMandates.getMandate().get(0).getLastPaymentAmount().get("currency"));
        assertEquals(consumerCollectionAmounts.get(0).toString(), responseMandates.getMandate().get(0).getLastPaymentAmount().get("amount"));

        DirectDebitsHelper.getDirectDebitMandate(secretKey, mandate.getKey().getLeft(), consumerAuthenticationToken)
                .then()
                .body("lastPaymentDate", notNullValue())
                .body("lastPaymentAmount", notNullValue())
                .body("lastPaymentAmount.currency", equalTo(consumerCurrency))
                .body("lastPaymentAmount.amount", equalTo(consumerCollectionAmounts.get(0).intValue()));
    }

    @Test
    public void CollectDebit_NotEnoughFunds_CollectionStateUnpaid() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = Iterables.get(corporateMandates.entrySet(), 0);

        DirectDebitsHelper.collectCollection(mandate.getValue().get(1).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(1).getLeft(), mandate.getKey().getLeft(), MandateCollectionState.UNPAID, secretKey, corporateAuthenticationToken);

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.UNPAID.name()));
    }

    @Test
    public void CollectDebit_NoFunds_CollectionStateUnpaid() {

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
                createMandatesWithCollections(managedAccount, createCorporateModel.getBaseCurrency(), Collections.singletonList(100L),
                        corporate.getRight(), 1)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), MandateCollectionState.UNPAID, secretKey, corporate.getRight());

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.UNPAID.name()));
    }

    @Test
    public void CollectDebit_CollectionAlreadyPaid_CollectionInvalidState() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = consumerMandates.entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(1).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(1).getLeft(), mandate.getKey().getLeft(), secretKey, consumerAuthenticationToken);

        SimulatorService.collectCollection(mandate.getValue().get(1).getRight(), mandate.getKey().getRight().getDdiId(),
                        secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COLLECTION_INVALID_STATE"));

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.PAID.name()));
    }

    @Test
    public void CollectDebit_CollectionRejected_CollectionInvalidState() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = Iterables.get(corporateMandates.entrySet(), 0);

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.AMOUNT_DIFFERS),
                mandate.getValue().get(3).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        SimulatorService.collectCollection(mandate.getValue().get(3).getRight(), mandate.getKey().getRight().getDdiId(),
                        secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COLLECTION_INVALID_STATE"));

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(3).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.REJECTED.name()));
    }

    @Test
    public void CollectDebit_CollectionUnpaid_CollectionInvalidState() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = Iterables.get(corporateMandates.entrySet(), 0);

        DirectDebitsHelper.collectCollection(mandate.getValue().get(1).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(1).getLeft(), mandate.getKey().getLeft(), MandateCollectionState.UNPAID, secretKey, corporateAuthenticationToken);

        SimulatorService.collectCollection(mandate.getValue().get(1).getRight(), mandate.getKey().getRight().getDdiId(),
                        secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("COLLECTION_INVALID_STATE"));

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(1).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.UNPAID.name()));
    }

    @Test
    public void CollectDebit_MandateCancelled_Success() {

        // Cancelled and expired mandates will be processed anyway, it's up to the user to reject the collection.
        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = Iterables.get(corporateMandates.entrySet(), 1);

        DirectDebitsHelper.cancelMandateInternally(mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.PAID.name()));
    }

    @Test
    public void CollectDebit_MandateExpired_Success() {

        // Cancelled and expired mandates will be processed anyway, it's up to the user to reject the collection.
        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate = Iterables.get(corporateMandates.entrySet(), 2);

        DirectDebitsHelper.expireMandate(mandate.getKey().getLeft(), mandate.getKey().getRight().getDdiId(), secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(MandateCollectionState.PAID.name()));
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

    private static GetMandateModel getPaidMandate(GetDirectDebitMandatesResponse responseMandates) {
        Iterator<GetMandateModel> mandates = responseMandates.getMandate().iterator();
        GetMandateModel mandate = null;

        while (mandates.hasNext()) {
            GetMandateModel paidMandate = mandates.next();
            if (paidMandate.getLastPaymentDate() != null) {
                mandate = paidMandate;
            }
        }
        return mandate;
    }
}
