package opc.junit.multi.directdebits;

import commons.enums.Currency;
import commons.enums.State;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.DirectDebitsHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class RejectCollectionTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount;
    private static Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount;

    @BeforeAll
    public static void Setup(){

        corporateSetup();
        consumerSetup();

        corporateManagedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount = createFundedManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
    }

    @ParameterizedTest
    @EnumSource(value = CollectionRejectionReason.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void RejectCollection_Corporate_Success(final CollectionRejectionReason rejectionReason) {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(rejectionReason),
                secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @ParameterizedTest
    @EnumSource(value = CollectionRejectionReason.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void RejectCollection_Consumer_Success(final CollectionRejectionReason rejectionReason) {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(consumerManagedAccount, consumerCurrency, 100L, consumerAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(rejectionReason),
                secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void RejectCollection_CollectionPaidThenRefundedAfterReject_Success() {

        final Long collectionAmount = 100L;
        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, collectionAmount, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(corporateManagedAccount.getLeft(), secretKey,
                corporateAuthenticationToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("OUTGOING_DIRECT_DEBIT_REFUND"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(collectionAmount.intValue()))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].transactionFee.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionFee.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(collectionAmount.intValue()))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(collectionAmount.intValue()))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.mandateId", equalTo(mandate.getKey().getLeft()))
                .body("entry[0].additionalFields.merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("entry[0].additionalFields.merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("OUTGOING_DIRECT_DEBIT_COLLECTION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact(collectionAmount.intValue())))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)(DEPOSIT_AMOUNT - collectionAmount.intValue())))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].transactionFee.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(DEPOSIT_AMOUNT - collectionAmount.intValue())))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact(collectionAmount.intValue())))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(DEPOSIT_AMOUNT - collectionAmount.intValue())))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(Math.negateExact(collectionAmount.intValue())))
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.mandateId", equalTo(mandate.getKey().getLeft()))
                .body("entry[1].additionalFields.merchantName", equalTo(mandate.getKey().getRight().getMerchantName()))
                .body("entry[1].additionalFields.merchantReference", equalTo(mandate.getKey().getRight().getMerchantReference()))
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].transactionFee.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionFee.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[2].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(DEPOSIT_AMOUNT.intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void RejectCollection_CollectionUnpaid_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 11000L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.collectCollection(mandate.getValue().get(0).getRight(), mandate.getKey().getRight().getDdiId(),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), MandateCollectionState.UNPAID, secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);
    }

    @Test
    public void RejectCollection_MandateCancelledInternally_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.cancelMandateInternally(mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);
    }

    @Test
    public void RejectCollection_MandateCancelledExternally_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.cancelMandateExternally(mandate.getKey().getLeft(), mandate.getKey().getRight().getDdiId(), secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);
    }

    @Test
    public void RejectCollection_MandateExpired_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsHelper.expireMandate(mandate.getKey().getLeft(), mandate.getKey().getRight().getDdiId(), secretKey, corporateAuthenticationToken);

        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                mandate.getValue().get(0).getLeft(), mandate.getKey().getLeft(), secretKey, corporateAuthenticationToken);
    }

    @Test
    public void RejectCollection_UnknownRejectionReason_NotFound() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.UNKNOWN),
                secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNKNOWN_REASON"));
    }

    @Test
    public void RejectCollection_UnknownMandate_NotFound() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, RandomStringUtils.randomNumeric(18), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RejectCollection_InvalidMandate_BadRequest() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, RandomStringUtils.randomAlphanumeric(6), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void RejectCollection_UnknownCollection_NotFound() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, mandate.getKey().getLeft(), RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RejectCollection_InvalidCollection_BadRequest() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, mandate.getKey().getLeft(), RandomStringUtils.randomAlphanumeric(6), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void RejectCollection_InvalidApiKey_Unauthorised(){

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                "abc", RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RejectCollection_NoApiKey_BadRequest(){

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                "", RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void RejectCollection_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void RejectCollection_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RejectCollection_CrossIdentityChecks_NotFound() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 100L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.rejectDirectDebitMandateCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
                secretKey, mandate.getKey().getLeft(), mandate.getValue().get(0).getLeft(), consumerAuthenticationToken)
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
