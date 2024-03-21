package opc.junit.smoke;

import io.restassured.path.json.JsonPath;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.CurrencyMinMaxModel;
import opc.models.admin.FinancialLimitValueModel;
import opc.models.admin.LimitContextModel;
import opc.models.admin.LimitValueModel;
import opc.models.admin.MaxAmountModel;
import opc.models.admin.SetGlobalLimitModel;
import opc.models.admin.WindowWidthModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.simulator.SimulateDepositModel;
import opc.models.testmodels.BalanceModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionLimitsTests extends BaseSmokeSetup {
    private final static String IDENTITY_CURRENCY = Currency.EUR.name();

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String destinationIdentityAuthenticationToken;
    private static String corporateId;
    private static String consumerId;
    private static String destinationIdentityId;
    private static String corporateSourceManagedAccount;
    private static String consumerSourceManagedAccount;

    @BeforeAll
    public static void Setup() {
        final SetGlobalLimitModel setLimitModel = SetGlobalLimitModel.builder()
                .setContext(new LimitContextModel())
                .setWindowWidth(new WindowWidthModel(1, "DAY"))
                .setLimitValue(new LimitValueModel(new MaxAmountModel("1500000")))
                .setWideWindowMultiple("365")
                .setFinancialLimitValue(FinancialLimitValueModel.builder()
                        .setRefCurrency("EUR")
                        .setCurrencyMinMax(CurrencyMinMaxModel.builder()
                                .setGbp(new LimitValueModel(new MaxAmountModel("1350000")))
                                .setUsd(new LimitValueModel(new MaxAmountModel("1750000")))
                                .build())
                        .build())
                .build();

        AdminHelper.setGlobalCorporateVelocityLimit(setLimitModel, adminToken, "CORPORATE_VELOCITY_AGGREGATE");
        AdminHelper.setGlobalConsumerVelocityLimit(setLimitModel, adminToken, "CONSUMER_VELOCITY_AGGREGATE");

        corporateSetup();
        consumerSetup();
        destinationIdentitySetup();

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminImpersonatedTenantToken, corporateId);

        AdminHelper.setConsumerVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminImpersonatedTenantToken, consumerId);

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminImpersonatedTenantToken, destinationIdentityId);
    }

    @Test
    public void Send_LimitExceededOnManagedAccount_TransactionPending(){

        final long sendAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)sendAmount, 0);
    }

    @Test
    public void Send_LimitExceededOnPrepaidCard_TransactionPending(){

        final long sendAmount = 4000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedCardBalance(managedCardId, destinationIdentityAuthenticationToken, (int)sendAmount, 0);
    }

    @Test
    public void Send_SourceManagedAccountChecks_TransactionPending(){

        final long sendAmount = 1000L;
        final long depositAmount = 3000L;
        final int sendFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.MA_TO_MC_SEND_FEE).getAmount().intValue();
        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, IDENTITY_CURRENCY, depositAmount, secretKey, corporateAuthenticationToken);

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(destinationManagedAccountId, IDENTITY_CURRENCY, depositAmount, secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS), new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + sendFeeAmount))))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(depositAmount - sendAmount - depositFeeAmount - sendFeeAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + sendFeeAmount))))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(depositAmount - sendAmount - depositFeeAmount - sendFeeAmount)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(depositAmount - sendAmount - depositFeeAmount - sendFeeAmount)))
                .body("entry[0].cardholderFee.amount",  equalTo(sendFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo(Math.negateExact((int)(sendAmount + sendFeeAmount))))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken,
                (int)(depositAmount - sendAmount - depositFeeAmount - sendFeeAmount),
                (int)(depositAmount - sendAmount - depositFeeAmount - sendFeeAmount));
    }

    @Test
    public void Send_SourceManagedCardChecks_TransactionPending(){

        final long sendAmount = 3100L;
        final long transferAmount = 4000L;
        final int sendFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.MA_TO_MC_SEND_FEE).getAmount().intValue();

        final String sourceManagedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        TestHelper.transferFundsToCard(corporateSourceManagedAccount, applicationOne.getTransfersProfileId(),
                sourceManagedCardId, IDENTITY_CURRENCY, transferAmount, secretKey, corporateAuthenticationToken);

        final String destinationManagedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(new ManagedInstrumentTypeId(sourceManagedCardId, MANAGED_CARDS), new ManagedInstrumentTypeId(destinationManagedCardId, MANAGED_CARDS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedCardsHelper.getManagedCardStatement(sourceManagedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + sendFeeAmount))))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(transferAmount - sendAmount - sendFeeAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + sendFeeAmount))))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(transferAmount - sendAmount - sendFeeAmount)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(transferAmount - sendAmount - sendFeeAmount)))
                .body("entry[0].cardholderFee.amount",  equalTo(sendFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo(Math.negateExact((int)(sendAmount + sendFeeAmount))))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) transferAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) transferAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) transferAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) transferAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int) transferAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int) transferAmount))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedCardBalance(sourceManagedCardId, corporateAuthenticationToken,
                (int)(transferAmount - sendAmount - sendFeeAmount), (int)(transferAmount - sendAmount - sendFeeAmount));
    }

    @Test
    public void Send_LimitExceededAfterSuccessfulSend_TransactionPending(){

        final long sendAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(consumerSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        consumerAuthenticationToken, sendAmount, "COMPLETED");

        final String pendingSendId =
                sendTransaction(consumerSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        consumerAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingSendId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(sendId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo(0))
                .body("entry[2].balanceAfter.amount", equalTo(0))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[2].transactionId.id",  equalTo(sendId))
                .body("entry[2].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount * 2), (int)sendAmount);
    }

    @Test
    public void Send_JustAboveLimit_TransactionPending(){

        final long sendAmount = 3001L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)sendAmount, 0);
    }

    @Test
    public void Send_ExactLimit_Success(){

        final long sendAmount = 3000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(sendId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(sendId))
                .body("entry[1].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)sendAmount, (int)sendAmount);
    }

    @Test
    public void Oct_PrepaidLimitExceeded_TransactionPending() {

        final long octAmount = 4000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(octId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)octAmount, 0);
    }

    @Test
    public void Oct_DebitLimitExceeded_TransactionPending(){

        final long octAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String managedCardId =
                ManagedCardsHelper.createDebitManagedCard(applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId(), managedAccountId,
                        secretKey, corporateAuthenticationToken);

        final String debitOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(debitOctId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(debitOctId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, (int)octAmount, 0);
    }

    @Test
    public void Oct_DebitWithFundsLimitExceeded_TransactionPending(){

        final long octAmount = 1000L;
        final long depositAmount = 3000L;
        final long depositFee = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, IDENTITY_CURRENCY, depositAmount, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                ManagedCardsHelper.createDebitManagedCard(applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId(), managedAccountId,
                        secretKey, corporateAuthenticationToken);

        final String debitOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(debitOctId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount + depositAmount - depositFee)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(depositAmount - depositFee)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(depositAmount - depositFee)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(debitOctId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken,
                (int)(octAmount + depositAmount - depositFee), (int)(depositAmount - depositFee));
    }

    @Test
    public void Oct_LimitExceededAfterSuccessfulOct_TransactionPending(){

        final long octAmount = 2000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, consumerAuthenticationToken);
        final String pendingOctId = simulateOct(managedCardId, octAmount, consumerAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingOctId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(octId))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedCardBalance(managedCardId, consumerAuthenticationToken, (int)(octAmount * 2), (int)octAmount);
    }

    @Test
    public void Oct_JustAboveLimit_TransactionPending(){

        final long octAmount = 3001L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(octId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)octAmount, 0);
    }

    @Test
    public void Oct_ExactLimit_Success(){

        final long octAmount = 3000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(octId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)octAmount, (int)octAmount);
    }

    @Test
    public void Deposit_LimitExceeded_DepositPending(){

        final long depositAmount = 4000L;
        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        simulatePendingDeposit(managedAccountId, corporateAuthenticationToken, depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath().get("entry[0].details.id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, expectedActualBalance, 0);
    }

    @Test
    public void Deposit_LimitExceededAfterSuccessfulDeposit_DepositPending(){

        final long depositAmount = 2000L;
        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedAvailableBalance = (int) (depositAmount - depositFeeAmount);
        final int expectedActualBalanceAdjustment = (int) ( depositAmount - depositFeeAmount);
        final int expectedActualBalance = (int) ( expectedAvailableBalance + depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedAvailableBalance);
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedActualBalance);

        final JsonPath deposits =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalanceAdjustment))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].details.id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].actualBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].details.id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedActualBalance, expectedAvailableBalance);
    }

    @Test
    public void Deposit_DepositFeeNotAffectingLimit_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3000L + depositFeeAmount;
        final long secondDepositAmount = 100L + depositFeeAmount;
        final int expectedAvailableBalance = (int) (depositAmount - depositFeeAmount);
        final int expectedActualBalanceAdjustment = (int) ( secondDepositAmount - depositFeeAmount);
        final int expectedActualBalance = (int) ( expectedAvailableBalance + secondDepositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedAvailableBalance);
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, secondDepositAmount, expectedActualBalance);

        final JsonPath deposits =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalanceAdjustment))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)secondDepositAmount))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].details.id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].actualBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].details.id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedActualBalance, expectedAvailableBalance);
    }

    @Test
    public void Deposit_JustAboveLimit_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3001L + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath().get("entry[0].details.id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedActualBalance, 0);
    }

    @Test
    public void Deposit_ExactLimit_Success(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3000L + depositFeeAmount;
        final int expectedBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedBalance);

        final String depositId =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath().get("entry[0].details.id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedBalance, expectedBalance);
    }

    @Test
    public void Deposit_ReachLimitAgainAfterPendingDepositResumed_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        final JsonPath depositsToResume =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath();
        AdminHelper.resumeDeposit(adminImpersonatedTenantToken, depositsToResume.get("entry[0].details.id"));

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 3));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].details.id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].details.id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[1].details.id")))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[3].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[3].transactionId.id",  equalTo(deposits.get("entry[2].details.id")))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 3), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    @Test
    public void Deposit_ReachLimitAgainAfterPendingDepositsResumed_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        final JsonPath depositsToResume =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath();
        AdminHelper.resumeDeposits(adminImpersonatedTenantToken,
                Arrays.asList(depositsToResume.get("entry[0].details.id"), depositsToResume.get("entry[1].details.id")));

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 3));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminImpersonatedTenantToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].details.id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].details.id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[2].details.id")))
                .body("entry[2].entryState",  equalTo("COMPLETED"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[3].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[3].transactionId.id",  equalTo(deposits.get("entry[1].details.id")))
                .body("entry[3].entryState",  equalTo("PENDING"))
                .body("entry[4].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[4].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[4].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[4].availableBalanceAfter.amount", equalTo(0))
                .body("entry[4].balanceAfter.amount", equalTo(0))
                .body("entry[4].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[4].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[4].transactionId.id",  equalTo(deposits.get("entry[2].details.id")))
                .body("entry[4].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 3), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    private static void destinationIdentitySetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        destinationIdentityId = authenticatedCorporate.getLeft();
        destinationIdentityAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, destinationIdentityId);

        AdminHelper.resetCorporateLimit(adminImpersonatedTenantToken, destinationIdentityId);
    }
    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        consumerSourceManagedAccount =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY, secretKey, consumerAuthenticationToken);

        final Long depositFee = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount();
        final long depositAmount = 1400000L + depositFee;
        TestHelper.simulateManagedAccountDeposit(consumerSourceManagedAccount, IDENTITY_CURRENCY, depositAmount, secretKey, consumerAuthenticationToken);

        AdminHelper.resetConsumerLimit(adminImpersonatedTenantToken, consumerId);
    }

    private String sendTransaction(final String sourceInstrumentId,
                                   final ManagedInstrumentTypeId destinationInstrument,
                                   final String token,
                                   final Long sendAmount,
                                   final String expectedState){
        return sendTransaction(new ManagedInstrumentTypeId(sourceInstrumentId, MANAGED_ACCOUNTS), destinationInstrument, token, sendAmount, expectedState);
    }

    private String sendTransaction(final ManagedInstrumentTypeId sourceInstrument,
                                   final ManagedInstrumentTypeId destinationInstrument,
                                   final String token,
                                   final Long sendAmount,
                                   final String expectedState){

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(applicationOne.getSendProfileId())
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), sendAmount))
                        .setSource(sourceInstrument)
                        .setDestination(destinationInstrument)
                        .build();

        return SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(expectedState))
                .extract()
                .jsonPath()
                .get("id");

    }

    private void assertManagedAccountBalance(final String managedAccountId,
                                             final String authenticationToken,
                                             final int expectedActualBalance,
                                             final int expectedAvailableBalance){

        final BalanceModel actualBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, authenticationToken);
        assertEquals(expectedActualBalance, actualBalance.getActualBalance());
        assertEquals(expectedAvailableBalance, actualBalance.getAvailableBalance());
    }

    private void assertManagedCardBalance(final String managedCardId,
                                          final String authenticationToken,
                                          final int expectedActualBalance,
                                          final int expectedAvailableBalance){

        final BalanceModel actualBalance = ManagedCardsHelper.getManagedCardBalance(managedCardId, secretKey, authenticationToken);
        assertEquals(expectedActualBalance, actualBalance.getActualBalance());
        assertEquals(expectedAvailableBalance, actualBalance.getAvailableBalance());
    }

    private String simulateOct(final String managedCardId,
                               final long amount,
                               final String authenticationToken) {

        final long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.simulateOct(secretKey, managedCardId, authenticationToken, new CurrencyAmount(IDENTITY_CURRENCY, amount));

        final String[] octId = new String[1];

        final BooleanSupplier booleanSupplier = () -> {
            final Map<Integer, Map<String, String>> octTransactions;
            try {
                octTransactions = ManagedCardsDatabaseHelper.getLatestOct(managedCardId, timestamp);

                if (octTransactions.size() > 0){
                    octId[0] = octTransactions.get(0).get("id");
                    return true;
                }
                return false;
            } catch (SQLException exception) {
                return false;
            }
        };

        TestHelper.isConditionSatisfied(60, booleanSupplier);

        return octId[0];
    }

    private void simulatePendingDeposit(final String managedAccountId,
                                        final String token,
                                        final Long depositAmount,
                                        final int expectedActualBalance){

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(IDENTITY_CURRENCY, depositAmount));
        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final BooleanSupplier booleanSupplier = () ->
                ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, token)
                        .getActualBalance() == expectedActualBalance;

        TestHelper.isConditionSatisfied(120, booleanSupplier);
    }

    private void simulateSuccessfulDeposit(final String managedAccountId,
                                           final String token,
                                           final Long depositAmount,
                                           final int expectedAvailableBalance){

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(IDENTITY_CURRENCY, depositAmount));
        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final BooleanSupplier booleanSupplier = () ->
                ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, token)
                        .getAvailableBalance() == expectedAvailableBalance;

        TestHelper.isConditionSatisfied(120, booleanSupplier);
    }

    private static void checkAndUpgradeIbanIfUnassigned(final String managedAccountId,
        final String secretKey,
        final String token) {
        if ( ManagedAccountsService.getManagedAccountIban(secretKey,managedAccountId, token).then().extract().jsonPath().get("state").equals("UNALLOCATED")) {
            ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccountId, token).then()
                .statusCode(SC_OK);
        }
    }


    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        corporateSourceManagedAccount =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY, secretKey, corporateAuthenticationToken);

        final Long depositFee = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount();
        final long depositAmount = 1400000L + depositFee;
        TestHelper.simulateManagedAccountDeposit(corporateSourceManagedAccount, IDENTITY_CURRENCY, depositAmount, secretKey, corporateAuthenticationToken);

        AdminHelper.resetCorporateLimit(adminImpersonatedTenantToken, corporateId);
    }
}

