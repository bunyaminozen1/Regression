package opc.junit.multi.transactions;

import io.restassured.path.json.JsonPath;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.backoffice.SpendLimitModel;
import opc.models.backoffice.SpendRulesModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS_BACKOFFICE_SETUP)
public class ManagedCardTransactionsBackofficeSetupTests extends BaseTransactionRulesSetup {

    private static String corporateAuthenticationToken;
    private static String corporateCurrency;
    private static String corporateId;
    private static String consumerAuthenticationToken;
    private static String consumerCurrency;
    private static String consumerId;
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void Setup(){

        corporateSetup();
        consumerSetup();

        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitCorporate_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_DebitConsumer_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.WEEKLY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), consumerAuthenticationToken,
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardMerchantRefund_DebitCorporate_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.MONTHLY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseFee - refundFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseFee - refundFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_ConsumerDebitCardSpendLimitExceeded_DeniedSpendControl(final LimitInterval interval){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 90L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), interval))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_CorporateDebitCardSpendLimitExceeded_DeniedSpendControl(final LimitInterval interval){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 90L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("purchaseLimits")
    public void CardPurchase_DebitMaximumMinimumLimitExceeded_DeniedSpendControl(final InstrumentType instrumentType,
                                                                                 final Long purchaseAmount){

        final Long availableToSpend = 1000L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(99L)
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("purchaseLimits")
    public void CardPurchase_PrepaidMaximumMinimumLimitExceeded_DeniedSpendControl(final InstrumentType instrumentType,
                                                                                   final Long purchaseAmount){

        final Long depositAmount = 1000L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(99L)
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, depositAmount.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @ValueSource(longs = {20L, 21L, 99L, 100L})
    public void CardPurchase_DebitPurchaseAmountWithinLimits_Success(final Long purchaseAmount){

        final Long availableToSpend = 1000L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(100L)
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitMaxMinLimitSetToZero_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 20L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setMinTransactionAmount(0L)
                        .setMaxTransactionAmount(0L)
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitMaxMinLimitNotSet_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 20L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_MaxLimitWithMultipleAuths_DeniedSpendControl(){

        final Long purchaseAmount1 = 2999L;
        final Long purchaseAmount2 = 3000L;
        final Long purchaseAmount3 = 3001L;
        final Long availableToSpend = 10000L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setMaxTransactionAmount(3000L)
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode1 = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount1));

        final String purchaseCode2 = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount2));

        final String purchaseCode3 = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount3));

        final int expectedAvailableToSpend =
                (int)(availableToSpend - purchaseAmount1 - purchaseAmount2 - purchaseFee - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount1 - purchaseAmount2 - purchaseFee - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode1);
        Assertions.assertEquals("APPROVED", purchaseCode2);
        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode3);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_MinLimitWithMultipleAuths_DeniedSpendControl(){

        final Long purchaseAmount1 = 21L;
        final Long purchaseAmount2 = 20L;
        final Long purchaseAmount3 = 19L;
        final Long availableToSpend = 10000L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setMinTransactionAmount(20L)
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken);

        final String purchaseCode1 = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount1));

        final String purchaseCode2 = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount2));

        final String purchaseCode3 = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount3));

        final int expectedAvailableToSpend =
                (int)(availableToSpend - purchaseAmount1 - purchaseAmount2 - purchaseFee - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount1 - purchaseAmount2 - purchaseFee - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode1);
        Assertions.assertEquals("APPROVED", purchaseCode2);
        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode3);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesPrepaidMerchantCategoryBlocked_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesDebitMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesPrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888"))
                        .build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesDebitMerchantCountryBlocked_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCountries(Collections.singletonList("MT"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesPrepaidMerchantCountryBlocked_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCountries(Collections.singletonList("MT"))
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesDebitMerchantCountryNotAllowed_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCountries(Collections.singletonList("IT"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesPrepaidMerchantCountryNotAllowed_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCountries(Collections.singletonList("IT"))
                        .build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesDebitMerchantCountryBothInAllowedAndBlocked_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCountries(Collections.singletonList("MT"))
                        .setBlockedMerchantCountries(Collections.singletonList("MT"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SetCardRulesPrepaidMerchantCountryBothInAllowedAndBlocked_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCountries(Collections.singletonList("MT"))
                        .setBlockedMerchantCountries(Collections.singletonList("MT"))
                        .build();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_SetCardRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), consumerAuthenticationToken,
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_SetCardRulesDebitMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), consumerAuthenticationToken,
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_SetCardRulesPrepaidMerchantCategoryBlocked_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), consumerAuthenticationToken,
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_SetCardRulesPrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888"))
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), consumerAuthenticationToken,
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_SetCardRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_SetCardRulesDebitMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888"))
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_SetCardRulesPrepaidMerchantCategoryBlocked_DeniedSpendControl(){

        final Long cardBalance = 100L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_SetCardRulesPrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long cardBalance = 100L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel()
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888"))
                        .build();

        BackofficeHelper.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
    }

    private String simulatePurchase(final String managedCardId,
                                    final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    private String simulateAuthReversal(final String managedCardId,
                                        final String token,
                                        final CurrencyAmount purchaseAmount){
        final JsonPath card =
                ManagedCardsService.getManagedCard(secretKey, managedCardId, token).jsonPath();

        return SimulatorHelper.simulateAuthReversal(secretKey,
                getCardNumber(card.get("cardNumber.value"), token),
                getCvv(card.get("cvv.value"), token),
                card.get("expiryMmyy"),
                purchaseAmount);
    }

    private String simulateMerchantRefund(final String managedCardId,
                                          final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateMerchantRefundById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(){
        return SpendRulesModel
                .builder()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                .setAllowedMerchantIds(new ArrayList<>())
                .setBlockedMerchantIds(new ArrayList<>())
                .setAllowContactless(true)
                .setAllowAtm(true)
                .setAllowECommerce(true)
                .setAllowCashback(true)
                .setAllowCreditAuthorisations(true)
                .setAllowedMerchantCountries(Collections.singletonList("MT"))
                .setBlockedMerchantCountries(Collections.singletonList("IT"))
                .setSpendLimit(new ArrayList<>());
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

    private static void consumerSetup() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static Stream<Arguments> purchaseLimits() {
        return Stream.of(Arguments.of(InstrumentType.PHYSICAL, 100L),
                Arguments.of(InstrumentType.VIRTUAL, 19L));
    }
}