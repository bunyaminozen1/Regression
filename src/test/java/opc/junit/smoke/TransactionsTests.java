package opc.junit.smoke;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.RetryType;
import commons.enums.State;
import opc.enums.opc.TestMerchant;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendLimitResponseModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.DetokenizeModel;
import opc.models.simulator.SimulateCardAuthModel;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.multi.ManagedCardsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionsTests extends BaseSmokeSetup {
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardMerchantRefund_DebitCorporate_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseFee - refundFee);

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
    @MethodSource("getInstrumentTypes")
    public void CardMerchantRefund_DebitConsumer_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(consumerCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(consumerCurrency, availableToSpend), consumerAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseFee - refundFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseFee - refundFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_ManagedAccountStatementChecks_Success() {

        final Long availableToSpend = 1000L;
        final long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseFee - refundFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseFee - refundFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        final ValidatableResponse response =
                ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                                secretKey, corporateAuthenticationToken, 4)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("MERCHANT_REFUND"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int) purchaseAmount))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedAccountExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(refundFee.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("PURCHASE_REFUND"))
                .body("entry[0].additionalFields.authorisationRelatedId", nullValue())
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[1].additionalFields.authorisationRelatedId", equalTo(response.extract().jsonPath().get("entry[2].transactionId.id")))
                .body("entry[1].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[2].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[2].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[2].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[2].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[2].additionalFields.authorisationCode", notNullValue())
                .body("entry[2].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[2].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[3].transactionId.id", notNullValue())
                .body("entry[3].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[3].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[3].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[3].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[3].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[3].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[3].cardholderFee.amount", equalTo(TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[3].processedTimestamp", notNullValue())
                .body("count", equalTo(4))
                .body("responseCount", equalTo(4));
    }

    @Test
    public void CardMerchantRefund_ManagedCardStatementChecks_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseFee - refundFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseFee - refundFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        final ValidatableResponse response =
                ManagedCardsHelper
                        .getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 3)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("MERCHANT_REFUND"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(purchaseAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(refundFee.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("PURCHASE_REFUND"))
                .body("entry[0].additionalFields.authorisationRelatedId", nullValue())
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[1].additionalFields.authorisationRelatedId", equalTo(response.extract().jsonPath().get("entry[2].transactionId.id")))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[2].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[2].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[2].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[2].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[2].additionalFields.authorisationCode", notNullValue())
                .body("entry[2].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitCorporate_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.ATM_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        final String purchaseCode = simulateAtmWithdrawal(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

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
    public void CardPurchase_MultipleSpendLimitIntervals_Success() {

        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.ATM_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final List<SpendLimitModel> spendLimits =
                Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 5000L), LimitInterval.YEARLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 2500L), LimitInterval.QUARTERLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1250L), LimitInterval.MONTHLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 750L), LimitInterval.WEEKLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), LimitInterval.DAILY));

        setSpendLimit(managedCard.getManagedCardId(),
                spendLimits,
                corporateAuthenticationToken);

        final String purchaseCode = simulateAtmWithdrawal(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final List<SpendLimitModel> updatedSpendLimits = new ArrayList<>();
        spendLimits.forEach(limit -> {
            final Long amount = limit.getValue().getAmount() - purchaseAmount - purchaseFee;
            updatedSpendLimits
                    .add(new SpendLimitModel(new CurrencyAmount(corporateCurrency, amount),
                            LimitInterval.valueOf(limit.getInterval())));
        });

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final List<SpendLimitResponseModel> remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, updatedSpendLimits);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        updatedSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    remainingAvailableToSpend.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);

            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());
        });
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitConsumer_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.ATM_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS)),
                consumerAuthenticationToken);

        final String purchaseCode = simulateAtmWithdrawal(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - TestHelper.getFees(consumerCurrency).get(FeeType.ATM_FEE).getAmount());

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_PrepaidCorporate_Success(final InstrumentType instrumentType) {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.ATM_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final String purchaseCode = simulateAtmWithdrawal(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int managedCardExpectedBalance =
                (int) (depositAmount - purchaseAmount - purchaseFee);

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, managedCardExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_ManagedAccountStatementChecks_Success() {

        final long availableToSpend = 1000L;
        final long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.ATM_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        simulateAtmWithdrawal(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        final Response managedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 3);

        managedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedAccountExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact(purchaseFee.intValue())))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) (purchaseAmount + purchaseFee))))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.authorisationRelatedId", equalTo(managedAccountStatement.jsonPath().get("entry[1].transactionId.id")))
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[1].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[2].additionalFields.sender", equalTo("Sender Test"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void CardPurchase_ManagedCardDebitStatementChecks_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.ATM_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        simulateAtmWithdrawal(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 2);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.authorisationRelatedId", equalTo(managedCardStatement.jsonPath().get("entry[1].transactionId.id")))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitCorporateAuth_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - purchaseAmount);

        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);

        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());


        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertNotNull(settlementId);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SettlementAmountLargerThanAuth_Success() {

        final Long availableToSpend = 1000L;
        final long authAmount = 100L;
        final Long settlementAmount = 101L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - authAmount);

        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);

        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - settlementAmount);

        final int managedAccountExpectedAvailableBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);
        final int managedAccountExpectedActualBalance = managedCard.getInitialManagedAccountBalance();

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedAvailableBalance, managedAccountExpectedActualBalance);

        Assertions.assertNotNull(settlementId);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedAvailableBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedActualBalance, managedAccountBalance.getActualBalance());

        final int expectedAvailableToSpendAfterRetry = (int) (availableToSpend - settlementAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

        AdminHelper.retrySettlement(RetryType.FORCE_NO_LOSS, adminToken, settlementId);

        final int finalRemainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterRetry);

        final BalanceModel finalManagedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpendAfterRetry, finalRemainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, finalManagedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, finalManagedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SettlementAmountSmallerThanAuth_Success() {

        final long availableToSpend = 1000L;
        final long authAmount = 100L;
        final long settlementAmount = 99L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - authAmount);

        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);

        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - authAmount - purchaseFee);

        final int managedAccountExpectedAvailableBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount - purchaseFee);
        final int managedAccountExpectedActualBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedAvailableBalance, managedAccountExpectedActualBalance);

        Assertions.assertNotNull(settlementId);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedAvailableBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedActualBalance, managedAccountBalance.getActualBalance());

        final int expectedAvailableToSpendAfterClosingSettlement = (int) (availableToSpend - authAmount - purchaseFee - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount - purchaseFee - purchaseFee);

        simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, authAmount - settlementAmount));

        final int finalRemainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterClosingSettlement);

        final BalanceModel finalManagedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpendAfterClosingSettlement, finalRemainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, finalManagedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, finalManagedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_PartialSettlement_Success() {

        final long availableToSpend = 1000L;
        final long authAmount = 100L;
        final long settlementAmount = 50L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - authAmount);

        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);

        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - authAmount - purchaseFee);

        final int managedAccountExpectedAvailableBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount - purchaseFee);
        final int managedAccountExpectedActualBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedAvailableBalance, managedAccountExpectedActualBalance);

        Assertions.assertNotNull(settlementId);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedAvailableBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedActualBalance, managedAccountBalance.getActualBalance());

        final int expectedAvailableToSpendAfterClosingSettlement = (int) (availableToSpend - authAmount - purchaseFee - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount - purchaseFee - purchaseFee);

        simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, authAmount - settlementAmount));

        final int finalRemainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterClosingSettlement);

        final BalanceModel finalManagedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpendAfterClosingSettlement, finalRemainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, finalManagedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, finalManagedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_SettleMultipleAuths_Success() {

        final Long availableToSpend = 1000L;
        final long authAmount = 100L;
        final Long settlementAmount = 200L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - authAmount);

        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);

        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());

        simulateAuth(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterSecondAuth = (int) (remainingAvailableToSpendAfterAuth - authAmount);

        final int expectedManagedAccountBalanceAfterSecondAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount * 2);

        final int remainingAvailableToSpendAfterSecondAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterSecondAuth);

        final BalanceModel managedAccountBalanceAfterSecondAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterSecondAuth, remainingAvailableToSpendAfterSecondAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterSecondAuth, managedAccountBalanceAfterSecondAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterSecondAuth.getActualBalance());

        simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - settlementAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        final Response managedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 4);

        managedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(settlementAmount.intValue())))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedAccountExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(TestMerchant.valueOf(corporateCurrency).getMerchantName()))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[0].additionalFields.authorisationRelatedId", equalTo(managedAccountStatement.jsonPath().get("entry[2].transactionId.id")))
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))

                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) authAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - authAmount - authAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo(TestMerchant.valueOf(corporateCurrency).getMerchantName()))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[1].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))

                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(Math.negateExact((int) authAmount)))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - authAmount)))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].additionalFields.merchantName", equalTo(TestMerchant.valueOf(corporateCurrency).getMerchantName()))
                .body("entry[2].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[2].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[2].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[2].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[2].additionalFields.authorisationCode", notNullValue())
                .body("entry[2].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[2].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))

                .body("entry[3].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[3].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[3].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[3].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[3].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[3].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[3].cardholderFee.amount", equalTo(TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[3].processedTimestamp", notNullValue())
                .body("count", equalTo(4))
                .body("responseCount", equalTo(4));
    }

    @Test
    public void CardPurchase_MultipleAuthsPartialSettlement_Success() {

        final Long availableToSpend = 1000L;
        final Long authAmount = 100L;
        final long settlementAmount = 150L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - authAmount);

        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);

        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());

        simulateAuth(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, authAmount));

        final int expectedAvailableToSpendAfterSecondAuth = (int) (remainingAvailableToSpendAfterAuth - authAmount);

        final int expectedManagedAccountBalanceAfterSecondAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount * 2);

        final int remainingAvailableToSpendAfterSecondAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterSecondAuth);

        final BalanceModel managedAccountBalanceAfterSecondAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterSecondAuth, remainingAvailableToSpendAfterSecondAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterSecondAuth, managedAccountBalanceAfterSecondAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterSecondAuth.getActualBalance());

        simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpendAfterRetry = (int) (availableToSpend - authAmount - authAmount - purchaseFee);

        final int managedAccountExpectedAvailableBalanceAfterRetry =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount - authAmount - purchaseFee);
        final int managedAccountExpectedActualBalanceAfterRetry =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

        final int actualRemainingAvailableToSpendAfterRetry =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterRetry);

        final BalanceModel actualManagedAccountBalanceAfterRetry =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedAvailableBalanceAfterRetry, managedAccountExpectedActualBalanceAfterRetry);

        Assertions.assertEquals(expectedAvailableToSpendAfterRetry, actualRemainingAvailableToSpendAfterRetry);
        Assertions.assertEquals(managedAccountExpectedAvailableBalanceAfterRetry, actualManagedAccountBalanceAfterRetry.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedActualBalanceAfterRetry, actualManagedAccountBalanceAfterRetry.getActualBalance());

        final int expectedFinalAvailableToSpendAfterRetry = (int) (availableToSpend - authAmount - authAmount - purchaseFee - purchaseFee);

        final int managedAccountExpectedFinalBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount - authAmount - purchaseFee - purchaseFee);

        simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, (authAmount * 2) - settlementAmount));

        final int finalRemainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedFinalAvailableToSpendAfterRetry);

        final BalanceModel finalManagedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedFinalBalance);

        Assertions.assertEquals(expectedFinalAvailableToSpendAfterRetry, finalRemainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedFinalBalance, finalManagedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedFinalBalance, finalManagedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitAuthorisationDeclinedThroughOverruling_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(0)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId());

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals(availableToSpend.intValue(), remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());

        final Response managedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 2);

        managedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_DECLINE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", corporateCurrency.toLowerCase())))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].additionalFields.sender", equalTo("Sender Test"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 1);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_DECLINE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", corporateCurrency.toLowerCase())))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void CardPurchase_DebitAuthorisationCancelledThroughDelayedOverruling_Success() {

        final Long availableToSpend = 1000L;
        final long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(15)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId());

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals(availableToSpend.intValue(), remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());

        final Response managedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 3);

        managedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_CANCELLATION"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int) purchaseAmount))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int) purchaseAmount))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", corporateCurrency.toLowerCase())))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.relatedTransactionIdType", equalTo("AUTHORISATION"))
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", corporateCurrency.toLowerCase())))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.relatedTransactionIdType", equalTo("AUTHORISATION_CANCELLATION"))
                .body("entry[1].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[2].additionalFields.sender", equalTo("Sender Test"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 2);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_CANCELLATION"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int) purchaseAmount))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", corporateCurrency.toLowerCase())))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", corporateCurrency.toLowerCase())))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void CardPurchase_PrepaidAuthorisationDeclinedThroughOverruling_Success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, depositAmount, 1);

        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(consumerCurrency, purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(0)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId());

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, depositAmount.intValue());

        Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getActualBalance());

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, consumerAuthenticationToken, 2);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_DECLINE"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", consumerCurrency.toLowerCase())))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(consumerCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].transactionId.type", equalTo("TRANSFER"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void CardPurchase_PrepaidAuthorisationCancelledThroughDelayedOverruling_Success() {

        final Long depositAmount = 1000L;
        final long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, depositAmount, 1);

        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(consumerCurrency, purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(15)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId());

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, depositAmount.intValue());

        Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getActualBalance());

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, consumerAuthenticationToken, 3);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_CANCELLATION"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int) purchaseAmount))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int) purchaseAmount))
                .body("entry[0].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", consumerCurrency.toLowerCase())))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(consumerCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.relatedTransactionIdType", equalTo("AUTHORISATION"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (depositAmount.intValue() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (depositAmount.intValue() - purchaseAmount)))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo(String.format("old_simulator_%s", consumerCurrency.toLowerCase())))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(consumerCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.relatedTransactionIdType", equalTo("AUTHORISATION_CANCELLATION"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void CardPurchase_DebitAuthorisationOverrulingOff_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setIsOverruled(false)
                        .setOverrulingNotificationDelaySeconds(20)
                        .build();

        SimulatorHelper.simulateAuthorisation(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId());

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_DebitCorporate_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_DebitConsumer_Success(final InstrumentType instrumentType) {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(consumerCurrency, availableToSpend), consumerAuthenticationToken);

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

    @Test
    public void CardAuthReversal_ReversalAmountSameAsOriginal_ReverseOriginalValueSuccess() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long reversalAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_ReversalAmountLargerThanOriginal_ReverseOriginalValueSuccess() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long reversalAmount = 101L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_ReversalAmountSmallerThanOriginal_ReverseReversalValueSuccess() {

        final long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long reversalAmount = 99L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        final Long expectedAvailableToSpend = availableToSpend - (purchaseAmount - reversalAmount);

        final long expectedManagedAccountAvailableBalance =
                managedCard.getInitialManagedAccountBalance() - (purchaseAmount - reversalAmount);

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, (int) expectedManagedAccountAvailableBalance,
                        managedCard.getInitialManagedAccountBalance());

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals((int) expectedManagedAccountAvailableBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_ManagedAccountStatementChecks_Success() {

        final Long availableToSpend = 1000L;
        final long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_REVERSAL"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo((int) purchaseAmount))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationRelatedId", notNullValue())
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[1].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void CardAuthReversal_ManagedCardStatementChecks_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_REVERSAL"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(purchaseAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.authorisationRelatedId", notNullValue())
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Refundable.com"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void CardAuthReversal_SpendLimitReached_Success() {

        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long purchaseAmount = 100L;
        final Long availableToSpend = purchaseFee + purchaseAmount;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_SpendLimitReachedWithoutFee_Success() {

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_ManagedAccountBlocked_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedCard.getManagedCardModel().getParentManagedAccountId(),
                secretKey, corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_Forex_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final Currency forexCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(corporateCurrency));
        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(forexCurrency.name(), purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_NoFundsForFee_FeeNotRefundedSuccess() {

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();

        final long depositAmount = purchaseAmount + depositFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_NotEnoughFundsToCoverFullFee_OnlyPartialFeeRefundedSuccess() {

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();

        final long depositAmount = purchaseAmount + purchaseFee + depositFee - 5;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_NoSpendLimit_Success() {

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, 0);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(0L, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }


    private static ManagedCardDetails createFundedManagedAccountAndDebitCard(final String managedAccountProfileId,
                                                                             final String managedCardProfileId,
                                                                             final String currency,
                                                                             final String authenticationToken) {

        return createFundedManagedAccountAndDebitCard(managedAccountProfileId, managedCardProfileId,
                currency, authenticationToken, 10000L);
    }

    private static ManagedCardDetails createFundedManagedAccountAndDebitCard(final String managedAccountProfileId,
                                                                             final String managedCardProfileId,
                                                                             final String currency,
                                                                             final String authenticationToken,
                                                                             final Long depositAmount) {
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, depositAmount, secretKey, authenticationToken);

        final int balance = (int) (depositAmount - TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(DEBIT_MODE)
                .setInstrumentType(VIRTUAL)
                .setInitialManagedAccountBalance(balance)
                .setInitialDepositAmount(depositAmount.intValue())
                .build();
    }

    private void setSpendLimit(final String managedCardId,
                               final CurrencyAmount spendLimit,
                               final String authenticationToken) {
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private void setSpendLimit(final String managedCardId,
                               final List<SpendLimitModel> spendLimit,
                               final String authenticationToken) {
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final List<SpendLimitModel> spendLimit) {
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
                .setSpendLimit(spendLimit);
    }

    private String simulateMerchantRefund(final String managedCardId,
                                          final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundByIdModel) {
        return SimulatorHelper.simulateMerchantRefundById(secretKey,
                managedCardId,
                simulateCardMerchantRefundByIdModel);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final CurrencyAmount spendLimit) {
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
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(spendLimit, LimitInterval.ALWAYS)));
    }

    private String simulateSettlement(final String managedCardId,
                                      final String relatedAuthorisationId,
                                      final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateSettlement(innovatorId, Long.parseLong(relatedAuthorisationId), purchaseAmount, managedCardId);
    }

    private String simulateMerchantRefund(final String managedCardId,
                                          final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateMerchantRefundById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    private String simulateAtmWithdrawal(final String managedCardId,
                                         final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateAtmWithdrawalById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    protected static ManagedCardDetails createPrepaidManagedCard(final String managedCardProfileId,
                                                                 final String currency,
                                                                 final String authenticationToken) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(PREPAID_MODE)
                .setInstrumentType(VIRTUAL)
                .build();
    }

    protected static Pair<String, CreateManagedAccountModel> transferFundsToCard(final String token,
                                                                                 final IdentityType identityType,
                                                                                 final String managedCardId,
                                                                                 final String currency,
                                                                                 final Long depositAmount,
                                                                                 final int transferCount) {

        return TestHelper
                .simulateManagedAccountDepositAndTransferToCard(identityType.equals(IdentityType.CONSUMER) ?
                                consumerManagedAccountsProfileId : corporateManagedAccountsProfileId,
                        transfersProfileId, managedCardId, currency, depositAmount, secretKey, token, transferCount);
    }

    private static String createManagedAccount(final String managedAccountsProfileId, final String currency, final String token) {
        return ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(managedAccountsProfileId, currency).build(),
                        secretKey, token);
    }

    private String simulateAuth(final String managedCardId,
                                final String relatedAuthorisationId,
                                final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId);
    }

    private String simulateAuth(final String managedCardId,
                                final String relatedAuthorisationId,
                                final CurrencyAmount purchaseAmount,
                                final Boolean expectPendingSettlement) {
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId, expectPendingSettlement);
    }

    private String simulateAuth(final String managedCardId,
                                final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, null, managedCardId);
    }

    private String simulateAuthReversal(final String managedCardId,
                                        final String token,
                                        final CurrencyAmount purchaseAmount) {
        final JsonPath card =
                ManagedCardsService.getManagedCard(secretKey, managedCardId, token).jsonPath();

        return SimulatorHelper.simulateAuthReversal(secretKey,
                getCardNumber(card.get("cardNumber.value"), token),
                getCvv(card.get("cvv.value"), token),
                card.get("expiryMmyy"),
                purchaseAmount);
    }

    private String getCardNumber(final String encryptedCardNumber,
                                 final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.detokenize(secretKey,
                                new DetokenizeModel(encryptedCardNumber, "CARD_NUMBER"),
                                authenticationToken), SC_OK)
                .jsonPath().get("value");
    }

    private String getCvv(final String encryptedCvv,
                          final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.detokenize(secretKey,
                                new DetokenizeModel(encryptedCvv, "CARD_NUMBER"),
                                authenticationToken), SC_OK)
                .jsonPath().get("value");
    }

    private String simulateAuthReversalById(final String managedCardId,
                                            final String relatedAuthorisationId,
                                            final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateAuthReversalById(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}

