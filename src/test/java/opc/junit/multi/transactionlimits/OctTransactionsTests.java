package opc.junit.multi.transactionlimits;

import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.KycLevel;
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
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.BalanceModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OctTransactionsTests extends BaseTransactionLimitsSetup {

    private final static String IDENTITY_CURRENCY = Currency.EUR.name();

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void setup(){

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

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporateId);

        AdminHelper.setConsumerVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, consumerId);
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
    public void Oct_PrepaidCardReachLimitAgainAfterPendingTransactionResumed_TransactionPending(){

        final long octAmount = 2000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        final String pendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        AdminHelper.resumeOct(adminTenantImpersonationToken, pendingOctId);

        final String secondPendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(secondPendingOctId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingOctId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingOctId))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[3].transactionId.id",  equalTo(octId))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)(octAmount * 3), (int)(octAmount * 2));
    }

    @Test
    public void Oct_DebitCardReachLimitAgainAfterPendingTransactionResumed_TransactionPending(){

        final long octAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String managedCardId =
                ManagedCardsHelper.createDebitManagedCard(applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId(), managedAccountId,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        final String pendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        AdminHelper.resumeOct(adminTenantImpersonationToken, pendingOctId);

        final String secondPendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(secondPendingOctId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingOctId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingOctId))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[3].transactionId.id",  equalTo(octId))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(secondPendingOctId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingOctId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingOctId))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[3].transactionId.id",  equalTo(octId))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, (int)(octAmount * 3), (int)(octAmount * 2));
    }

    @Test
    public void Oct_ReachLimitAgainAfterLimitReset_TransactionPending(){

        final long octAmount = 2000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, consumerAuthenticationToken);

        final String pendingOctId = simulateOct(managedCardId, octAmount, consumerAuthenticationToken);

        AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);

        final String secondOctId = simulateOct(managedCardId, octAmount, consumerAuthenticationToken);

        final String secondPendingOctId = simulateOct(managedCardId, octAmount, consumerAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 4)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(secondPendingOctId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(octAmount * 3)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(secondOctId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingOctId))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[3].transactionId.id",  equalTo(octId))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        assertManagedCardBalance(managedCardId, consumerAuthenticationToken, (int)(octAmount * 4), (int)(octAmount * 2));
    }

    @Test
    public void Oct_ReachLimitAcrossMultipleAccounts_TransactionPending(){

        final long octAmount = 2000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String managedCardId2 =
                ManagedCardsHelper.createDebitManagedCard(applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId(), managedAccountId,
                        secretKey, corporateAuthenticationToken);

        final String prepaidOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        final String debitOctId = simulateOct(managedCardId2, octAmount, corporateAuthenticationToken);

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
                .body("entry[0].transactionId.id",  equalTo(prepaidOctId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        ManagedCardsHelper.getManagedCardStatement(managedCardId2, secretKey, corporateAuthenticationToken)
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

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)octAmount, (int)octAmount);
        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, (int)octAmount, 0);
    }

    @Test
    public void Oct_ReachLimitThenSendSmallerOct_TransactionPending(){

        final long octAmount = 4000L;
        final long smallerOctAmount = 500L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        final String secondOctId = simulateOct(managedCardId, smallerOctAmount, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)smallerOctAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount + smallerOctAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)smallerOctAmount))
                .body("entry[0].transactionId.id",  equalTo(secondOctId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(octId))
                .body("entry[1].entryState",  equalTo("PENDING"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)(octAmount + smallerOctAmount), 0);
    }

    @Test
    public void Oct_GlobalExactLimit_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final long octAmount = 1500000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        final String octId = simulateOct(managedCardId, octAmount, corporate.getRight());

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporate.getRight())
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

        assertManagedCardBalance(managedCardId, corporate.getRight(), (int)octAmount, (int)octAmount);
    }

    @Test
    public void Oct_GlobalJustAboveLimit_TransactionPending(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final long octAmount = 1500001L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        final String octId = simulateOct(managedCardId, octAmount, corporate.getRight());

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporate.getRight())
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

        assertManagedCardBalance(managedCardId, corporate.getRight(), (int)octAmount, 0);
    }

    @Test
    public void Oct_FundsSourceExactLimit_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        AdminHelper.setCorporateFundsSourceLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporate.getLeft());

        final long octAmount = 3000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        final String octId = simulateOct(managedCardId, octAmount, corporate.getRight());

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporate.getRight())
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

        assertManagedCardBalance(managedCardId, corporate.getRight(), (int)octAmount, (int)octAmount);
    }

    @Test
    public void Oct_FundsSourceJustAboveLimit_TransactionPending(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        AdminHelper.setCorporateFundsSourceLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporate.getLeft());

        final long octAmount = 3001L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        final String octId = simulateOct(managedCardId, octAmount, corporate.getRight());

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporate.getRight())
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

        assertManagedCardBalance(managedCardId, corporate.getRight(), (int)octAmount, 0);
    }

    @Test
    public void Oct_ResumeOctTransactionAlreadyResumed_Success(){

        final long octAmount = 2000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String octId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        final String pendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        AdminHelper.resumeOct(adminTenantImpersonationToken, pendingOctId);
        AdminHelper.resumeOct(adminTenantImpersonationToken, pendingOctId);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingOctId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingOctId))
                .body("entry[1].entryState",  equalTo("PENDING"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(octId))
                .body("entry[2].entryState",  equalTo("COMPLETED"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)(octAmount * 2), (int)(octAmount * 2));
    }

    @Test
    public void Oct_ResumeOctsOneTransactionAlreadyResumed_Success(){

        final long octAmount = 4000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String pendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);
        final String pendingOctId1 = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        AdminHelper.resumeOct(adminTenantImpersonationToken, pendingOctId);
        AdminHelper.resumeOcts(adminTenantImpersonationToken, Arrays.asList(pendingOctId, pendingOctId1));

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingOctId1))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingOctId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo(0))
                .body("entry[2].balanceAfter.amount", equalTo(0))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingOctId1))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)(octAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[3].transactionId.id",  equalTo(pendingOctId))
                .body("entry[3].entryState",  equalTo("PENDING"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)(octAmount * 2), (int)(octAmount * 2));
    }

    @Test
    public void Oct_ResumeOcts_Success(){

        final long octAmount = 4000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        final String pendingOctId = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);
        final String pendingOctId1 = simulateOct(managedCardId, octAmount, corporateAuthenticationToken);

        AdminHelper.resumeOcts(adminTenantImpersonationToken, Arrays.asList(pendingOctId, pendingOctId1));

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingOctId1))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)octAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingOctId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(octAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo(0))
                .body("entry[2].balanceAfter.amount", equalTo(0))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingOctId1))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)octAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)(octAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)octAmount))
                .body("entry[3].transactionId.id",  equalTo(pendingOctId))
                .body("entry[3].entryState",  equalTo("PENDING"));

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, (int)(octAmount * 2), (int)(octAmount * 2));
    }

    @Test
    public void Oct_PrepaidLimitExceededKycLevel1_TransactionPending() {

        final long octAmount = 15001;

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), createConsumerModel.getBaseCurrency(),
                        secretKey, consumer.getRight());

        final String octId = simulateOct(managedCardId, octAmount, consumer.getRight());

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, consumer.getRight())
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

        assertManagedCardBalance(managedCardId, consumer.getRight(), (int)octAmount, 0);
    }

    @Test
    public void Oct_PrepaidWithinLimitKycLevel1_Success() {

        final long octAmount = 15000;

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), createConsumerModel.getBaseCurrency(),
                        secretKey, consumer.getRight());

        final String octId = simulateOct(managedCardId, octAmount, consumer.getRight());

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, consumer.getRight())
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

        assertManagedCardBalance(managedCardId, consumer.getRight(), (int)octAmount, (int)octAmount);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private String simulateOct(final String managedCardId,
                               final long amount,
                               final String authenticationToken) {

        final long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.simulateOct(secretKey, managedCardId, authenticationToken, new CurrencyAmount(IDENTITY_CURRENCY, amount));

        return TestHelper.ensureDatabaseResultAsExpected(60,
                () -> ManagedCardsDatabaseHelper.getLatestOct(managedCardId, timestamp),
                x -> x.size() > 0,
                Optional.of("No OCTs were returned.")).get(0).get("id");
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

    @AfterEach
    public void resetLimit(){
        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, corporateId);
        AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);
    }
}
