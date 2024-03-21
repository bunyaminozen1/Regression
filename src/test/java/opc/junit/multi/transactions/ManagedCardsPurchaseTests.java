package opc.junit.multi.transactions;

import io.restassured.response.Response;
import opc.enums.opc.BlockType;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.KycLevel;
import opc.enums.opc.LimitInterval;
import commons.enums.State;
import opc.junit.database.GpsSimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendLimitResponseModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS)
public class ManagedCardsPurchaseTests extends BaseTransactionsSetup {

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
    public void CardPurchase_DebitCorporate_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

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
    public void CardPurchase_MultipleSpendLimitIntervals_Success(){

        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

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

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
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
    public void CardPurchase_DebitConsumer_Success(final InstrumentType instrumentType){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS)),
                consumerAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount());

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
    public void CardPurchase_PrepaidCorporate_Success(final InstrumentType instrumentType){

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        if (instrumentType.equals(InstrumentType.PHYSICAL)){
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
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
    public void CardPurchase_ManagedAccountStatementChecks_Success(){

        final long availableToSpend = 1000L;
        final long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

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
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact(purchaseFee.intValue())))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(purchaseAmount + purchaseFee))))
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
                .body("entry[1].balanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
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
    public void CardPurchase_ManagedCardDebitStatementChecks_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

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

    @Test
    public void CardPurchase_ManagedCardPrepaidStatementChecks_Success(){

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int managedCardExpectedBalance =
                (int) (depositAmount - purchaseAmount - purchaseFee);

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, managedCardExpectedBalance);

        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getActualBalance());

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 3);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedCardExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(depositAmount - purchaseAmount - purchaseFee)))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact(purchaseFee.intValue())))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(depositAmount - purchaseAmount - purchaseFee)))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(purchaseAmount + purchaseFee))))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.authorisationRelatedId", equalTo(managedCardStatement.jsonPath().get("entry[1].transactionId.id")))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)(depositAmount - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(depositAmount - purchaseAmount)))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact(purchaseAmount.intValue())))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
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
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void CardPurchase_NoSpendLimit_DeniedSpendControl(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, 0);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(0L, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_PhysicalDebitCardNotActive_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount());

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
    public void CardPurchase_DebitCorporateForex_Success() throws SQLException {

        final long availableToSpend = 1000L;
        final long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        final Currency forexCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(corporateCurrency));

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(forexCurrency.name(), purchaseAmount))
                        .setForexPadding(10L)
                        .setForexFee(5L)
                        .build();

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                simulateCardPurchaseModel);

        final int expectedPurchaseAmount = Math.abs(Integer.parseInt(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final int expectedAvailableToSpend = (int)(availableToSpend - expectedPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee());

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee());

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
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
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee()))))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedAccountExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee())))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int) (simulateCardPurchaseModel.getForexPadding() - purchaseFee)))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee())))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(expectedPurchaseAmount + purchaseFee + simulateCardPurchaseModel.getForexFee()))))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.forexFeeCurrency", equalTo(corporateCurrency))
                .body("entry[0].additionalFields.forexFeeAmount", equalTo(simulateCardPurchaseModel.getForexFee().toString()))
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
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) (expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee() + simulateCardPurchaseModel.getForexPadding()))))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - simulateCardPurchaseModel.getForexFee() - simulateCardPurchaseModel.getForexPadding())))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - simulateCardPurchaseModel.getForexFee() - simulateCardPurchaseModel.getForexPadding())))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) (expectedPurchaseAmount + simulateCardPurchaseModel.getForexPadding() + simulateCardPurchaseModel.getForexFee()))))
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
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo(simulateCardPurchaseModel.getForexPadding().toString()))
                .body("entry[1].additionalFields.forexFeeCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexFeeAmount", equalTo(simulateCardPurchaseModel.getForexFee().toString()))
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

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 2);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee()))))
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
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) (expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee() + simulateCardPurchaseModel.getForexPadding()))))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexFeeAmount", equalTo(simulateCardPurchaseModel.getForexFee().toString()))
                .body("entry[1].additionalFields.forexFeeCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo(simulateCardPurchaseModel.getForexPadding().toString()))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void CardPurchase_PrepaidCorporateForex_Success() throws SQLException {

        final long depositAmount = 1000L;
        final long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        setSpendLimit(managedCard.getManagedCardId(),
                new ArrayList<>(),
                corporateAuthenticationToken);

        final Currency forexCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(corporateCurrency));

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(forexCurrency.name(), purchaseAmount))
                        .setForexPadding(10L)
                        .setForexFee(5L)
                        .build();

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                simulateCardPurchaseModel);

        final int expectedPurchaseAmount = Math.abs(Integer.parseInt(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final int managedCardExpectedBalance =
                (int) (depositAmount - expectedPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee());

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, managedCardExpectedBalance, managedCardExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getActualBalance());

        final Response managedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCard.getManagedCardId(), secretKey, corporateAuthenticationToken, 3);

        managedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int)(expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee()))))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedCardExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(managedCardExpectedBalance))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)(simulateCardPurchaseModel.getForexPadding() - purchaseFee)))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int)(depositAmount - expectedPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee())))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee() + purchaseFee))))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.authorisationRelatedId", equalTo(managedCardStatement.jsonPath().get("entry[1].transactionId.id")))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int)(expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee() + simulateCardPurchaseModel.getForexPadding()))))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int)(depositAmount - expectedPurchaseAmount - simulateCardPurchaseModel.getForexFee() - simulateCardPurchaseModel.getForexPadding())))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(depositAmount - expectedPurchaseAmount - simulateCardPurchaseModel.getForexFee() - simulateCardPurchaseModel.getForexPadding())))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int)(expectedPurchaseAmount + simulateCardPurchaseModel.getForexFee() + simulateCardPurchaseModel.getForexPadding()))))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) depositAmount))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo(simulateCardPurchaseModel.getForexPadding().toString()))
                .body("entry[1].additionalFields.forexFeeCurrency", equalTo(corporateCurrency))
                .body("entry[1].additionalFields.forexFeeAmount", equalTo(simulateCardPurchaseModel.getForexFee().toString()))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo((int) depositAmount))
                .body("entry[2].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo((int) depositAmount))
                .body("entry[2].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) depositAmount))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo((int) depositAmount))
                .body("entry[2].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) depositAmount))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int) depositAmount))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void CardPurchase_DebitCorporateExceedAvailableToSpendWithForex_Success() throws SQLException {

        final String baseCurrency = Currency.USD.name();
        final String forexCurrency = Currency.EUR.name();

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(baseCurrency).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final Long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(baseCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final long availableToSpend = 8L + purchaseFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, baseCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(baseCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(forexCurrency, purchaseAmount));

        final int expectedPurchaseAmount = Math.abs(Integer.parseInt(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final int expectedAvailableToSpend = (int)(availableToSpend - expectedPurchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - purchaseFee);

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
    public void CardPurchase_MultipleCardPurchasesUnderSameParent_Success(){

        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final long managedAccountBalance = 100000L;
        final int numberOfCards = 3;

        final String managedAccountId = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency, managedAccountBalance + depositFee,
                secretKey, corporateAuthenticationToken);

        IntStream.range(0, numberOfCards)
                .forEach(i -> {
                    final ManagedCardDetails managedCard = createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountId, corporateAuthenticationToken);

                    setSpendLimit(managedCard.getManagedCardId(),
                            Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 5000L), LimitInterval.ALWAYS)),
                            corporateAuthenticationToken);

                    simulatePurchase(managedCard.getManagedCardId(),
                            new CurrencyAmount(corporateCurrency, purchaseAmount));
                });

        final int managedAccountExpectedBalance =
                (int) (managedAccountBalance - (purchaseAmount + purchaseFee) * numberOfCards);

        final BalanceModel actualManagedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedAccountId,
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(managedAccountExpectedBalance, actualManagedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, actualManagedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitCardBlocked_CardInactive(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
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
    public void CardPurchase_DebitCardDestroyed_CardInactive(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
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
    public void CardPurchase_ManagedAccountBlocked_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedCard.getManagedCardModel().getParentManagedAccountId(),
                secretKey, corporateAuthenticationToken);

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
    public void CardPurchase_ManagedAccountBlockedByAdmin_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        AdminHelper.blockManagedAccount(managedCard.getManagedCardModel().getParentManagedAccountId(),
                BlockType.ADMIN, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

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
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_AvailableSpendLimitReached_Success(final LimitInterval interval){

        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long purchaseAmount = 100L;
        final Long availableToSpend = purchaseFee + purchaseAmount;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval)),
                corporateAuthenticationToken);

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
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_AvailableSpendLimitExceededByFee_SuccessFeeTakenFromManagedAccount(final LimitInterval interval){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval)),
                corporateAuthenticationToken);

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
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_AvailableSpendLimitExceededByFee_SuccessJustEnoughFundsToCoverFullFee(final LimitInterval interval){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();

        final long depositAmount = purchaseAmount + purchaseFee + depositFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency,
                        corporateAuthenticationToken, depositAmount);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval)),
                corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 0);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(0, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(0, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_AvailableSpendLimitExceededByFee_SuccessNotEnoughFundsToCoverFullFeeTakePartial(final LimitInterval interval){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final int missingAmount = 5;

        final long depositAmount = purchaseAmount + purchaseFee + depositFee - missingAmount;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency,
                        corporateAuthenticationToken, depositAmount);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval)),
                corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee) + missingAmount;

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 0);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(0, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(0, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_AvailableSpendLimitExceededByFee_SuccessNoFundsForFee(final LimitInterval interval){

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();

        final long depositAmount = purchaseAmount + depositFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency,
                        corporateAuthenticationToken, depositAmount);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval)),
                corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, 0);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 0, 0);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(0, remainingAvailableToSpend);
        Assertions.assertEquals(0, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(0, managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void CardPurchase_AvailableSpendLimitExceeded_DeniedSpendControl(final LimitInterval interval) {

        final Long purchaseAmount = 1001L;
        final Long availableToSpend = 1000L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final List<SpendLimitModel> spendLimitModels = Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), interval));

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(spendLimitModels)
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(3000L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final List<SpendLimitResponseModel> remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, spendLimitModels);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());

        spendLimitModels.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    remainingAvailableToSpend.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);

            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());
        });
    }

    @Test
    public void CardPurchase_ManagedAccountNoFunds_DeniedNotEnoughFunds(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        setSpendLimit(managedCard.getManagedCardId(), 
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
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
    public void CardPurchase_PrepaidCardNoFunds_DeniedNotEnoughFunds(){

        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, 0);

        Assertions.assertEquals("DENIED_NOT_ENOUGH_FUNDS", purchaseCode);
        Assertions.assertEquals(0, managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(0, managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitCardFundsSetToZero_DeniedNotEnoughFunds() {

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 1000L;
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final long depositAmount = purchaseAmount + depositFee + purchaseFee;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency,
                        corporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, 0, 0);

        Assertions.assertEquals("DENIED_NOT_ENOUGH_FUNDS", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(0, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(0, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_PrepaidCardFundsSetToZero_DeniedNotEnoughFunds() {

        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final long depositAmount = purchaseAmount + purchaseFee;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency,
                        corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, 0);

        Assertions.assertEquals("DENIED_NOT_ENOUGH_FUNDS", purchaseCode);
        Assertions.assertEquals(0, managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(0, managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitMerchantCategoryBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
    public void CardPurchase_PrepaidMerchantCategoryBlocked_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                SpendRulesModel.builder()
                        .setAllowedMerchantCategories(Collections.singletonList("9999"))
                        .setBlockedMerchantCategories(Collections.singletonList("5399"))
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setSpendLimit(new ArrayList<>())
                        .setAllowAtm(false)
                        .setAllowCashback(false)
                        .setAllowContactless(false)
                        .setAllowECommerce(false)
                        .setAllowCreditAuthorisations(false)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, cardBalance.intValue());

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance, managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance, managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_DebitMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
    public void CardPurchase_PrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

        final Long cardBalance = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, cardBalance, 1);

        final SpendRulesModel spendRulesModel =
                SpendRulesModel.builder()
                        .setAllowedMerchantCategories(Collections.singletonList("9999"))
                        .setBlockedMerchantCategories(new ArrayList<>())
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setSpendLimit(new ArrayList<>())
                        .setAllowAtm(false)
                        .setAllowCashback(false)
                        .setAllowContactless(false)
                        .setAllowECommerce(false)
                        .setAllowCreditAuthorisations(false)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, cardBalance.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(cardBalance, managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(cardBalance, managedCardBalance.getActualBalance());
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
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(99L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
                getDefaultSpendRulesModel(new ArrayList<>())
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(99L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
                getDefaultSpendRulesModel(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setMinTransactionAmount(20L)
                        .setMaxTransactionAmount(100L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
                getDefaultSpendRulesModel(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setMinTransactionAmount(0L)
                        .setMaxTransactionAmount(0L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
                getDefaultSpendRulesModel(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
                getDefaultSpendRulesModel(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setMaxTransactionAmount(3000L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
                getDefaultSpendRulesModel(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setMinTransactionAmount(20L)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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
    public void CardPurchase_DebitMerchantCountryBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setBlockedMerchantCountries(Collections.singletonList("MT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                simulateCardPurchaseModel);

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
    public void CardPurchase_DebitMerchantCountryNotAllowed_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setAllowedMerchantCountries(Arrays.asList("IT", "ES")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                simulateCardPurchaseModel);

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
    public void CardPurchase_DebitMerchantCountryInBothAllowedAndBlocked_DeniedSpendControl(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setBlockedMerchantCountries(Collections.singletonList("MT"))
                        .setAllowedMerchantCountries(Collections.singletonList("MT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                simulateCardPurchaseModel);

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
    public void CardPurchase_DebitMerchantCountryAllowed_Success(){

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)))
                        .setAllowedMerchantCountries(Collections.singletonList("MT")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setTransactionCountry("MLT")
                        .build();

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                simulateCardPurchaseModel);

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
    public void CardPurchase_KycLevel1WithinLimit_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 8000L;
        final Long purchaseFee = TestHelper.getFees(createConsumerModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();
        final Long purchaseAmount = 5000L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        transferFundsToCard(consumer.getRight(), IdentityType.CONSUMER, managedCard.getManagedCardId(),
                createConsumerModel.getBaseCurrency(), depositAmount, 1);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(createConsumerModel.getBaseCurrency(), purchaseAmount));

        final int managedCardExpectedBalance =
                (int) (depositAmount - purchaseAmount - purchaseFee);

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumer.getRight(), managedCardExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_KycLevel1LimitExceeded_DeniedSpendControl(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 8000L;
        final Long purchaseAmount = 5001L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        transferFundsToCard(consumer.getRight(), IdentityType.CONSUMER, managedCard.getManagedCardId(),
                createConsumerModel.getBaseCurrency(), depositAmount, 1);

        final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(createConsumerModel.getBaseCurrency(), purchaseAmount));

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, consumer.getRight(), depositAmount.intValue());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        Assertions.assertEquals(depositAmount.intValue(), managedCardBalance.getAvailableBalance());
        Assertions.assertEquals(depositAmount.intValue(), managedCardBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_KycLevel1MerchantCategoryBlocked_DeniedSpendControl(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 8000L;
        final Long purchaseAmount = 20L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        transferFundsToCard(consumer.getRight(), IdentityType.CONSUMER, managedCard.getManagedCardId(),
                createConsumerModel.getBaseCurrency(), depositAmount, 1);

        final List<Integer> mccList = Arrays.asList(4829, 6012, 6051, 6538, 6540, 7995, 6011, 6010);

        mccList.forEach(mcc -> {
            final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                    SimulateCardPurchaseByIdModel.builder()
                            .setTransactionAmount(new CurrencyAmount(createConsumerModel.getBaseCurrency(), purchaseAmount))
                            .setMerchantCategoryCode(String.valueOf(mcc))
                            .build();

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    simulateCardPurchaseModel);

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
        });
    }

    @Test
    public void CardPurchase_KycLevel2MerchantCategoryKycLevel1Blocked_Approved(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Long depositAmount = 8000L;
        final Long purchaseAmount = 20L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        transferFundsToCard(consumer.getRight(), IdentityType.CONSUMER, managedCard.getManagedCardId(),
                createConsumerModel.getBaseCurrency(), depositAmount, 1);

        final List<Integer> mccList = Arrays.asList(4829, 6012, 6051, 6538, 6540, 6011, 6010);

        mccList.forEach(mcc -> {
            final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                    SimulateCardPurchaseByIdModel.builder()
                            .setTransactionAmount(new CurrencyAmount(createConsumerModel.getBaseCurrency(), purchaseAmount))
                            .setMerchantCategoryCode(String.valueOf(mcc))
                            .build();

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    simulateCardPurchaseModel);

            Assertions.assertEquals("APPROVED", purchaseCode);
        });
    }

    private void setSpendLimit(final String managedCardId,
                               final List<SpendLimitModel> spendLimit,
                               final String authenticationToken){
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private String simulatePurchase(final String managedCardId,
                                    final SimulateCardPurchaseByIdModel simulateCardPurchaseByIdModel){
        return SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseByIdModel);
    }

    private String simulatePurchase(final String managedCardId,
                                    final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final List<SpendLimitModel> spendLimit){
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

    private static Stream<Arguments> purchaseLimits() {
        return Stream.of(Arguments.of(InstrumentType.PHYSICAL, 100L),
                Arguments.of(InstrumentType.VIRTUAL, 19L));
    }
}
