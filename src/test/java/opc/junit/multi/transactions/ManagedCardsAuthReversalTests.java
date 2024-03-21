package opc.junit.multi.transactions;

import io.restassured.path.json.JsonPath;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.GetAuthorisationsModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS)
public class ManagedCardsAuthReversalTests extends BaseTransactionsSetup {

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
    public void CardAuthReversal_DebitCorporate_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
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
    public void CardAuthReversal_DebitConsumer_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
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
    public void CardAuthReversal_ReversalAmountSameAsOriginal_ReverseOriginalValueSuccess(){

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
    public void CardAuthReversal_ReversalAmountLargerThanOriginal_ReverseOriginalValueSuccess(){

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
    public void CardAuthReversal_ReversalAmountSmallerThanOriginal_ReverseReversalValueSuccess(){

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
    public void CardAuthReversal_ManagedAccountStatementChecks_Success(){

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
                .body("entry[1].balanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
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
    public void CardAuthReversal_ManagedCardStatementChecks_Success(){

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
    public void CardAuthReversal_SpendLimitReached_Success(){

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
    public void CardAuthReversal_SpendLimitReachedWithoutFee_Success(){

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
    public void CardAuthReversal_ManagedAccountBlocked_Success(){

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
    public void CardAuthReversal_Forex_Success(){

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
    public void CardAuthReversal_NoFundsForFee_FeeNotRefundedSuccess(){

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
    public void CardAuthReversal_NotEnoughFundsToCoverFullFee_OnlyPartialFeeRefundedSuccess(){

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
    public void CardAuthReversal_NoSpendLimit_Success(){

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

    @Test
    public void CardAuthReversal_DebitCardBlocked_DeniedCardInactive(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_CARD_INACTIVE", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_DebitCardDestroyed_DeniedCardInactive(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_CARD_INACTIVE", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_ManagedAccountNoFunds_DeniedNotEnoughFunds(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_NOT_ENOUGH_FUNDS", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_MerchantCategoryBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_MerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(new CurrencyAmount(corporateCurrency, availableToSpend))
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulateAuthReversal(managedCard.getManagedCardId(), corporateAuthenticationToken,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardAuthReversal_SpendLimitExceeded_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 1001L;

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

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_DebitCorporate_AuthorisationNotCompleted_ReversalNotProcessed(final InstrumentType instrumentType) throws SQLException {
        final Long availableToSpend = 1000L;
        final long purchaseAmount = 100L;
        final Long reversalAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateFailedAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        checkAuthReversalState(managedCard.getManagedCardId(), "RETRY");

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final int expectedManagedAccountBalance = (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, expectedManagedAccountBalance);

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(expectedManagedAccountBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());

        AdminHelper.getAuthorisations(new GetAuthorisationsModel(managedCard.getManagedCardId()), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].details.id", equalTo(authReversalId))
                .body("entry[0].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[0].state", equalTo("RETRY"))
                .body("entry[1].details.id", equalTo(authorisationId))
                .body("entry[1].details.authorisationType", equalTo("AUTHORISED"))
                .body("entry[1].state", equalTo("RETRY"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_DebitConsumer_AuthorisationNotCompleted_ReversalNotProcessed(final InstrumentType instrumentType) throws SQLException {
        final Long availableToSpend = 1000L;
        final long purchaseAmount = 100L;
        final Long reversalAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency,
                        consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(consumerCurrency, availableToSpend), consumerAuthenticationToken);

        final String authorisationId = simulateFailedAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(consumerCurrency, reversalAmount));

        checkAuthReversalState(managedCard.getManagedCardId(), "RETRY");

        final int remainingAvailableToSpend =
                ManagedCardsHelper
                        .getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, availableToSpend.intValue());

        final int expectedManagedAccountBalance = (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken, expectedManagedAccountBalance);

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(expectedManagedAccountBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());

        AdminHelper.getAuthorisations(new GetAuthorisationsModel(managedCard.getManagedCardId()), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].details.id", equalTo(authReversalId))
                .body("entry[0].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[0].state", equalTo("RETRY"))
                .body("entry[1].details.id", equalTo(authorisationId))
                .body("entry[1].details.authorisationType", equalTo("AUTHORISED"))
                .body("entry[1].state", equalTo("RETRY"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        Assertions.assertNotNull(authReversalId);

    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_PrepaidCorporate_AuthorisationNotCompleted_ReversalNotProcessed(final InstrumentType instrumentType) throws SQLException {
        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long reversalAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final String authorisationId = simulateFailedAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        checkAuthReversalState(managedCard.getManagedCardId(), "RETRY");

        final int managedCardExpectedBalance = (int) (depositAmount - purchaseAmount);

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, managedCardExpectedBalance);

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(depositAmount, managedCardBalance.getActualBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedCardBalance.getAvailableBalance());

        AdminHelper.getAuthorisations(new GetAuthorisationsModel(managedCard.getManagedCardId()), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].details.id", equalTo(authReversalId))
                .body("entry[0].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[0].state", equalTo("RETRY"))
                .body("entry[1].details.id", equalTo(authorisationId))
                .body("entry[1].details.authorisationType", equalTo("AUTHORISED"))
                .body("entry[1].state", equalTo("RETRY"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_PrepaidConsumer_AuthorisationNotCompleted_ReversalNotProcessed(final InstrumentType instrumentType) throws SQLException {
        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long reversalAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, depositAmount, 1);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        final String authorisationId = simulateFailedAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(consumerCurrency, reversalAmount));

        checkAuthReversalState(managedCard.getManagedCardId(), "RETRY");

        final int managedCardExpectedBalance = (int) (depositAmount - purchaseAmount);

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumerAuthenticationToken, managedCardExpectedBalance);

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(depositAmount, managedCardBalance.getActualBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedCardBalance.getAvailableBalance());

        AdminHelper.getAuthorisations(new GetAuthorisationsModel(managedCard.getManagedCardId()), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].details.id", equalTo(authReversalId))
                .body("entry[0].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[0].state", equalTo("RETRY"))
                .body("entry[1].details.id", equalTo(authorisationId))
                .body("entry[1].details.authorisationType", equalTo("AUTHORISED"))
                .body("entry[1].state", equalTo("RETRY"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardAuthReversal_PrepaidCorporate_AuthorisationNotCompleted_RetryReversal(final InstrumentType instrumentType) throws SQLException {
        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long reversalAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

//        Authorisation in RETRY state -> Reversal not processed
        final String authorisationId = simulateFailedAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String authReversalId = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        checkAuthReversalState(managedCard.getManagedCardId(), "RETRY");

        AdminHelper.getAuthorisations(new GetAuthorisationsModel(managedCard.getManagedCardId()), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].details.id", equalTo(authReversalId))
                .body("entry[0].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[0].state", equalTo("RETRY"))
                .body("entry[1].details.id", equalTo(authorisationId))
                .body("entry[1].details.authorisationType", equalTo("AUTHORISED"))
                .body("entry[1].state", equalTo("RETRY"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

//        Authorisation in CLOSED state -> Reversal was processed
        ManagedCardsDatabaseHelper.updateAuthState("CLOSED", "AUTHORISED", managedCard.getManagedCardId());

        final String authReversalIdSecond = simulateAuthReversalById(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, reversalAmount));

        checkAuthReversalState(managedCard.getManagedCardId(), "CLOSED");

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, depositAmount.intValue());

        Assertions.assertNotNull(authReversalId);
        Assertions.assertEquals(depositAmount, managedCardBalance.getActualBalance());
        Assertions.assertEquals(depositAmount, managedCardBalance.getAvailableBalance());

        AdminHelper.getAuthorisations(new GetAuthorisationsModel(managedCard.getManagedCardId()), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].details.id", equalTo(authReversalIdSecond))
                .body("entry[0].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[0].state", equalTo("CLOSED"))
                .body("entry[1].details.id", equalTo(authReversalId))
                .body("entry[1].details.authorisationType", equalTo("ONLINE_REVERSE"))
                .body("entry[1].state", equalTo("RETRY"))
                .body("entry[2].details.id", equalTo(authorisationId))
                .body("entry[2].details.authorisationType", equalTo("AUTHORISED"))
                .body("entry[2].state", equalTo("CLOSED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    private void checkAuthReversalState(final String managedCardId,
                                        final String expectedState){

        TestHelper.ensureAsExpected(60,
                () -> AdminService.getAuthorisations(new GetAuthorisationsModel(managedCardId), adminToken),
                x -> x.statusCode() == SC_OK &&
                x.jsonPath().get("entry[0].details.authorisationType").equals("ONLINE_REVERSE") &&
                x.jsonPath().get("entry[0].state").equals(expectedState),
                Optional.of(String.format("Expecting 200 with an Auth Reversal in state %s, for card %s check logged payload", expectedState, managedCardId)));
    }

    private void setSpendLimit(final String managedCardId,
                               final CurrencyAmount spendLimit,
                               final String authenticationToken){
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private String simulateAuthReversalById(final String managedCardId,
                                            final String relatedAuthorisationId,
                                            final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateAuthReversalById(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId);
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

    private String simulateAuth(final String managedCardId,
                                final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, null, managedCardId);
    }

    private String simulateFailedAuth(final String managedCardId,
                                      final CurrencyAmount purchaseAmount) throws SQLException {
        final String authId = SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount,null,managedCardId);
        ManagedCardsDatabaseHelper.updateAuthState("RETRY", "AUTHORISED", managedCardId);
        return authId;

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
