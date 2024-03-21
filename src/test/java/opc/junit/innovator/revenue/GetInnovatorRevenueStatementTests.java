package opc.junit.innovator.revenue;

import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.FeeSpecModel;
import opc.models.admin.ReverseFeeModel;
import opc.models.innovator.RevenueStatementRequestModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.*;
import opc.services.admin.AdminFeesService;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetInnovatorRevenueStatementTests {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String secretKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String programmeId;

    private static String transfersProfileId;
    private static String sendsProfileId;
    private static String outgoingWireTransfersProfileId;

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountsProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKey = applicationOne.getSecretKey();
        programmeId = applicationOne.getProgrammeId();
        transfersProfileId = applicationOne.getTransfersProfileId();
        sendsProfileId = applicationOne.getSendProfileId();
        outgoingWireTransfersProfileId = applicationOne.getOwtProfileId();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        corporateSetup();
        consumerSetup();
    }

    @Test
    public void GetStatement_Deposit_Success(){

        final Long timestamp = Instant.now().toEpochMilli();
        final long depositAmount = 1000L;
        final int depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long balanceUpdated = Math.negateExact(depositAmount - depositFee);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, corporateCurrency).build(),
                        secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency, depositAmount, secretKey, corporateAuthenticationToken);

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder()
                        .setCurrencies(Collections.singletonList(corporateCurrency))
                        .setPaging(new PagingModel(0, 1))
                        .setFeeInstrumentOwner(new TxTypeIdModel(OwnerType.CORPORATE.getValue(), corporateId))
                        .setCreatedFrom(timestamp).build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("DEPOSIT"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(balanceUpdated)))
                .body("statement[0].amount.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(managedAccountId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CORPORATE.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(corporateId))
                .body("statement[0].fee.fee.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(depositFee)))
                .body("statement[0].fee.feeType", equalTo("DEPOSIT_FEE"))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_Transfer_Success(){

        final long transferAmount = 500L;
        final int transferFee = TestHelper.getFees(consumerCurrency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount().intValue();

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, consumerCurrency).build(),
                        secretKey, consumerAuthenticationToken);

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, consumerCurrency).build(),
                        secretKey, consumerAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(sourceManagedAccountId, consumerCurrency, 10000L, secretKey, consumerAuthenticationToken);

        final Long timestamp = Instant.now().toEpochMilli();
        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().setCreatedFrom(timestamp).build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("TRANSFER"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(transferFee)))
                .body("statement[0].amount.currency", equalTo(consumerCurrency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(sourceManagedAccountId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CONSUMER.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(consumerId))
                .body("statement[0].fee.fee.currency", equalTo(consumerCurrency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(transferFee)))
                .body("statement[0].fee.feeType", equalTo("TRANSFERS_FEE"))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_Send_Success(){

        final long sendAmount = 500L;
        final int sendFee = TestHelper.getFees(consumerCurrency).get(FeeType.MA_TO_MA_SEND_FEE).getAmount().intValue();

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, consumerCurrency).build(),
                        secretKey, consumerAuthenticationToken);

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, consumerCurrency).build(),
                        secretKey, consumerAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(sourceManagedAccountId, consumerCurrency, 10000L, secretKey, consumerAuthenticationToken);

        final Long timestamp = Instant.now().toEpochMilli();
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().setCreatedFrom(timestamp).build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("SEND"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(sendFee)))
                .body("statement[0].amount.currency", equalTo(consumerCurrency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(sourceManagedAccountId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CONSUMER.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(consumerId))
                .body("statement[0].fee.fee.currency", equalTo(consumerCurrency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(sendFee)))
                .body("statement[0].fee.feeType", equalTo("SEND_FEE"))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_OutgoingWireTransfer_Success(){

        consumerSetup(Currency.EUR);

        final long sendAmount = 500L;
        final int sendFee = TestHelper.getFees(consumerCurrency).get(FeeType.SEPA_OWT_FEE).getAmount().intValue();
        final String currency = Currency.EUR.name();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, consumerCurrency).build(),
                        secretKey, consumerAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, consumerCurrency, 10000L, secretKey, consumerAuthenticationToken);

        final Long timestamp = Instant.now().toEpochMilli();
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency, sendAmount, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().setCreatedFrom(timestamp).build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(sendFee)))
                .body("statement[0].amount.currency", equalTo(currency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(managedAccountId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CONSUMER.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(consumerId))
                .body("statement[0].fee.fee.currency", equalTo(currency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(sendFee)))
                .body("statement[0].fee.feeType", equalTo("OUTGOING_WIRE_TRANSFER_FEE"))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_ChargeFee_Success(){

        final long depositAmount = 1000L;
        final int depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, corporateCurrency).build(),
                        secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency, depositAmount, secretKey, corporateAuthenticationToken);

        final Long timestamp = Instant.now().toEpochMilli();
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().setCreatedFrom(timestamp).build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("CHARGE_FEE"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(depositFee)))
                .body("statement[0].amount.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(managedAccountId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CORPORATE.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(corporateId))
                .body("statement[0].fee.fee.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(depositFee)))
                .body("statement[0].fee.feeType", equalTo("CUSTOM"))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_PurchaseFee_Success(){

        final long depositAmount = 1000L;
        final long purchaseAmount = 100L;
        final int purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount().intValue();
        final long balanceUpdated = purchaseAmount + purchaseFee;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency).build(),
                        secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountsProfileId, transfersProfileId, managedCardId, corporateCurrency, depositAmount, secretKey, corporateAuthenticationToken);

        final Long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.simulateCardPurchaseById(secretKey, managedCardId, new CurrencyAmount(corporateCurrency, purchaseAmount));

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().setCreatedFrom(timestamp).setTxIdType("SETTLEMENT").build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("SETTLEMENT"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(balanceUpdated)))
                .body("statement[0].amount.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(managedCardId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CORPORATE.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(corporateId))
                .body("statement[0].fee.fee.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(purchaseFee)))
                .body("statement[0].fee.feeType", equalTo("PURCHASE_FEE"))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_ReverseFee_Success(){

        final long depositAmount = 1000L;
        final long feeAmount = 100L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, corporateCurrency).build(),
                        secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency, depositAmount, secretKey, corporateAuthenticationToken);

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.CUSTOM)
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(corporateCurrency, feeAmount)))).build();

        final String transactionId =
                AdminFeesService.chargeFee(chargeFeeModel, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()))
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("batch.transaction.id");

        final Long timestamp = Instant.now().toEpochMilli();

        final ReverseFeeModel reverseFeeModel =
                ReverseFeeModel.builder()
                        .setTxId(new TxTypeIdModel(AdminFeeType.CUSTOM.name(), transactionId))
                        .setInstrumentId(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .build();

        AdminFeesService.reverseFee(reverseFeeModel, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()))
                .then()
                .statusCode(SC_OK);

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().setCreatedFrom(timestamp).build();

        InnovatorHelper.getInnovatorRevenueStatement(innovatorId, innovatorToken, revenueStatementRequestModel, 1)
                .then()
                .statusCode(SC_OK)
                .body("statement[0].txId.type", equalTo("FEE_REVERSAL"))
                .body("statement[0].txId.id", notNullValue())
                .body("statement[0].amount.amount", equalTo(String.valueOf(Math.negateExact(feeAmount))))
                .body("statement[0].amount.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("statement[0].fee.instrument.id", equalTo(managedAccountId))
                .body("statement[0].fee.instrumentOwner.type", equalTo(OwnerType.CORPORATE.getValue()))
                .body("statement[0].fee.instrumentOwner.id", equalTo(corporateId))
                .body("statement[0].fee.fee.currency", equalTo(corporateCurrency))
                .body("statement[0].fee.fee.amount", equalTo(String.valueOf(Math.negateExact(feeAmount))))
                .body("statement[0].txTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetStatement_OtherInnovator_NoEntries(){

        final RevenueStatementRequestModel revenueStatementRequestModel =
                RevenueStatementRequestModel.builder().build();

        InnovatorService.getInnovatorRevenueStatement("4265", innovatorToken, revenueStatementRequestModel)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    private static void consumerSetup() {
        consumerSetup(Currency.getRandomCurrency());
    }

    private static void consumerSetup(final Currency currency) {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(currency.name())
                        .build();

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
