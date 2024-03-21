package opc.junit.multi.transactions;

import io.restassured.response.Response;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import commons.enums.State;
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
import opc.models.simulator.SimulateCardAuthModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS)
public class ManagedCardsAuthOverruleTests extends BaseTransactionsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void CardPurchase_DebitAuthorisationDeclinedThroughOverruling_Success(){

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
    public void CardPurchase_DebitAuthorisationCancelledThroughDelayedOverruling_Success(){

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
    public void CardPurchase_PrepaidAuthorisationDeclinedThroughOverruling_Success(){

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
    public void CardPurchase_PrepaidAuthorisationCancelledThroughDelayedOverruling_Success(){

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
                .body("entry[1].balanceAfter.amount", equalTo((int)(depositAmount.intValue() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int)(depositAmount.intValue() - purchaseAmount)))
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
    public void CardPurchase_DebitAuthorisationOverrulingOff_Success(){

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

        final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount);

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

    private void setSpendLimit(final String managedCardId,
                               final CurrencyAmount spendLimit,
                               final String authenticationToken){
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
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
