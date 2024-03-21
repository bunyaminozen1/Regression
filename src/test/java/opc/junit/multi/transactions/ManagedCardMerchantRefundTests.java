package opc.junit.multi.transactions;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS)
public class ManagedCardMerchantRefundTests extends BaseTransactionsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
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

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

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
    @MethodSource("getInstrumentTypes")
    public void CardMerchantRefund_DebitConsumer_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(consumerCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(consumerCurrency, availableToSpend), consumerAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseFee - refundFee);

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
    public void CardMerchantRefund_ManagedAccountStatementChecks_Success(){

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

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseFee - refundFee);

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
                .body("entry[2].balanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
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
    public void CardMerchantRefund_ManagedCardStatementChecks_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseFee - refundFee);

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

    @Test
    public void CardMerchantRefund_SpendLimitReached_Success(){

        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();
        final Long purchaseAmount = 100L;
        final Long availableToSpend = purchaseFee + purchaseAmount + refundFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

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

    @Test
    public void CardMerchantRefund_SpendLimitExceededByPurchaseFee_Success(){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

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

    @Test
    public void CardMerchantRefund_ManagedAccountBlocked_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedCard.getManagedCardModel().getParentManagedAccountId(),
                secretKey, corporateAuthenticationToken);

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

    @Test
    public void CardMerchantRefund_Forex_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final Currency forexCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(corporateCurrency));
        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(forexCurrency.name(), purchaseAmount));

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

    @Test
    public void CardMerchantRefund_LossesFromBothPurchaseAndRefundFees_Success(){

        final Long purchaseAmount = 10L;
        final Long availableToSpend = 10L;
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();

        final long depositAmount = purchaseAmount + depositFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = 0;
        final int managedAccountExpectedBalance = 0;

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_NotEnoughFundsToCoverFullPurchaseFee_NoFeeRefundedSuccess() {

        final Long purchaseAmount = 1000L;
        final Long availableToSpend = 1000L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final int missingAmount = 15;

        final long depositAmount = purchaseAmount + purchaseFee + depositFee + refundFee - missingAmount;
        final int lostAmount = Math.abs((int) (depositAmount - depositFee - purchaseAmount - purchaseFee));

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend  - refundFee - (purchaseFee - lostAmount));

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - refundFee - (purchaseFee - lostAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_EnoughFundsForFeesAvailableToSpendGoesNegative_NoFeeRefundedSuccess(){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final int missingAmount = 5;

        final long depositAmount = purchaseAmount + purchaseFee + depositFee + refundFee - missingAmount;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

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

    @Test
    public void CardMerchantRefund_NoSpendLimit_Success(){

        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
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

    @Test
    public void CardMerchantRefund_DebitCardBlocked_DeniedCardInactive(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_CARD_INACTIVE", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_DebitCardDestroyed_DeniedCardInactive(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_CARD_INACTIVE", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_ManagedAccountNoFunds_DeniedNotEnoughFunds(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_NOT_ENOUGH_FUNDS", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardMerchantRefund_MerchantCategoryBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
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
    public void CardMerchantRefund_MerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
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
    public void CardMerchantRefund_SpendLimitExceeded_DeniedSpendControl(){

        final Long purchaseAmount = 1001L;
        final Long availableToSpend = 1000L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
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
    public void CardMerchantRefund_MerchantCountryBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setBlockedMerchantCountries(Collections.singletonList("MT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                simulateCardMerchantRefundModel);

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
    public void CardMerchantRefund_MerchantCountryNotAllowed_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setAllowedMerchantCountries(Collections.singletonList("IT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                simulateCardMerchantRefundModel);

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
    public void CardMerchantRefund_MerchantCountryInBothAllowedAndBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setBlockedMerchantCountries(Collections.singletonList("MT"))
                        .setAllowedMerchantCountries(Collections.singletonList("MT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                simulateCardMerchantRefundModel);

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
    public void CardMerchantRefund_MerchantCountryAllowed_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(corporateCurrency).get(FeeType.REFUND_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel   spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setAllowedMerchantCountries(Collections.singletonList("MT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                simulateCardMerchantRefundModel);

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

    private void setSpendLimit(final String managedCardId,
                               final CurrencyAmount spendLimit,
                               final String authenticationToken){
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private String simulateMerchantRefund(final String managedCardId,
                                          final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundByIdModel){
        return SimulatorHelper.simulateMerchantRefundById(secretKey,
                managedCardId,
                simulateCardMerchantRefundByIdModel);
    }

    private String simulateMerchantRefund(final String managedCardId,
                                          final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateMerchantRefundById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final CurrencyAmount spendLimit){
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