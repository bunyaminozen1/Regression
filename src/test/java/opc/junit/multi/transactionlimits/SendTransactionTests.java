package opc.junit.multi.transactionlimits;

import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.admin.*;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.testmodels.BalanceModel;
import opc.services.multi.SendsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendTransactionTests extends BaseTransactionLimitsSetup {

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
        destinationIdentitySetup();

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporateId);

        AdminHelper.setConsumerVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, consumerId);

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, destinationIdentityId);
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
    public void Send_ReachLimitAgainAfterPendingTransactionResumed_TransactionPending(){

        final long sendAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        final String pendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        AdminHelper.resumeSend(adminTenantImpersonationToken, pendingSendId);

        final String secondPendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(secondPendingSendId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingSendId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingSendId))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[3].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[3].transactionId.id",  equalTo(sendId))
                .body("entry[3].entryState",  equalTo("COMPLETED"))
                .body("entry[4].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[4].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[4].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[4].availableBalanceAfter.amount", equalTo(0))
                .body("entry[4].balanceAfter.amount", equalTo(0))
                .body("entry[4].cardholderFee.amount",  equalTo(0))
                .body("entry[4].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[4].transactionId.id",  equalTo(sendId))
                .body("entry[4].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount * 3), (int)(sendAmount * 2));
    }

    @Test
    public void Send_ReachLimitAgainAfterLimitReset_TransactionPending(){

        final long sendAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        final String pendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, destinationIdentityId);

        final String secondSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        final String secondPendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount * 4)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(secondPendingSendId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(sendAmount * 3)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(secondSendId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(sendAmount * 3)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[2].transactionId.id",  equalTo(secondSendId))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[3].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[3].transactionId.id",  equalTo(pendingSendId))
                .body("entry[3].entryState",  equalTo("PENDING"))
                .body("entry[4].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[4].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[4].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[4].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[4].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[4].cardholderFee.amount",  equalTo(0))
                .body("entry[4].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[4].transactionId.id",  equalTo(sendId))
                .body("entry[4].entryState",  equalTo("COMPLETED"))
                .body("entry[5].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[5].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[5].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[5].availableBalanceAfter.amount", equalTo(0))
                .body("entry[5].balanceAfter.amount", equalTo(0))
                .body("entry[5].cardholderFee.amount",  equalTo(0))
                .body("entry[5].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[5].transactionId.id",  equalTo(sendId))
                .body("entry[5].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount * 4), (int)(sendAmount * 2));
    }

    @Test
    public void Send_ReachLimitAcrossMultipleAccounts_TransactionPending(){

        final long sendAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String managedAccountSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        final String managedCardSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

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
                .body("entry[0].transactionId.id",  equalTo(managedAccountSendId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(managedAccountSendId))
                .body("entry[1].entryState",  equalTo("PENDING"));

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
                .body("entry[0].transactionId.id",  equalTo(managedCardSendId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int) sendAmount, (int) sendAmount);
        assertManagedCardBalance(managedCardId, destinationIdentityAuthenticationToken, (int) sendAmount, 0);
    }

    @Test
    public void Send_ReachLimitThenSendSmallerSend_TransactionPending(){

        final long sendAmount = 4000L;
        final long smallerSendAmount = 500L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        final String secondSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, smallerSendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)smallerSendAmount))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount + smallerSendAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)smallerSendAmount))
                .body("entry[0].transactionId.id",  equalTo(secondSendId))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(sendId))
                .body("entry[1].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount + smallerSendAmount), 0);
    }

    @Test
    public void Send_FundsSourceExactLimit_Success() {

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

        final long sendAmount = 3000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
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

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), (int)sendAmount, (int)sendAmount);
    }

    @Test
    public void Send_FundsSourceJustAboveLimit_TransactionPending(){

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

        final long sendAmount = 3001L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
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

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), (int)sendAmount, 0);
    }

    @Test
    public void Send_ResumeSendTransactionAlreadyResumed_Success(){

        final long sendAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String sendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "COMPLETED");

        final String pendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        AdminHelper.resumeSend(adminTenantImpersonationToken, pendingSendId);
        AdminHelper.resumeSend(adminTenantImpersonationToken, pendingSendId);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingSendId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingSendId))
                .body("entry[1].entryState",  equalTo("PENDING"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[2].transactionId.id",  equalTo(sendId))
                .body("entry[2].entryState",  equalTo("COMPLETED"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[3].transactionId.id",  equalTo(sendId))
                .body("entry[3].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount * 2), (int)(sendAmount * 2));
    }

    @Test
    public void Send_ResumeSendsOneTransactionAlreadyResumed_Success(){

        final long sendAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String pendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        final String pendingSendId1 =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        AdminHelper.resumeSend(adminTenantImpersonationToken, pendingSendId);
        AdminHelper.resumeSends(adminTenantImpersonationToken, Arrays.asList(pendingSendId, pendingSendId1));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingSendId1))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingSendId))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo(0))
                .body("entry[2].balanceAfter.amount", equalTo(0))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingSendId1))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)(sendAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[3].transactionId.id",  equalTo(pendingSendId))
                .body("entry[3].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount * 2), (int)(sendAmount * 2));
    }

    @Test
    public void Send_ResumeSends_Success(){

        final long sendAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, destinationIdentityAuthenticationToken);

        final String pendingSendId =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        final String pendingSendId1 =
                sendTransaction(corporateSourceManagedAccount, new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS),
                        corporateAuthenticationToken, sendAmount, "PENDING");

        AdminHelper.resumeSends(adminTenantImpersonationToken, Arrays.asList(pendingSendId, pendingSendId1));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, destinationIdentityAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[0].transactionId.id",  equalTo(pendingSendId))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].balanceAfter.amount", equalTo((int)sendAmount))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[1].transactionId.id",  equalTo(pendingSendId1))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int)(sendAmount * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo(0))
                .body("entry[2].balanceAfter.amount", equalTo(0))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[2].transactionId.id",  equalTo(pendingSendId1))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int)sendAmount))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int)(sendAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(0))
                .body("entry[3].transactionAmount.amount",  equalTo((int)sendAmount))
                .body("entry[3].transactionId.id",  equalTo(pendingSendId))
                .body("entry[3].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, destinationIdentityAuthenticationToken, (int)(sendAmount * 2), (int)(sendAmount * 2));
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

        AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);
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

        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, corporateId);
    }

    private static void destinationIdentitySetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        destinationIdentityId = authenticatedCorporate.getLeft();
        destinationIdentityAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, destinationIdentityId);

        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, destinationIdentityId);
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

    @AfterEach
    public void resetLimit(){
        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, corporateId);
        AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);
        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, destinationIdentityId);
    }
}
