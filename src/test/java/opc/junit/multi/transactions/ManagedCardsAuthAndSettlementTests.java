package opc.junit.multi.transactions;

import commons.enums.Currency;
import io.restassured.response.Response;
import opc.enums.opc.FeeType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.RetryType;
import opc.enums.opc.TestMerchant;
import opc.junit.database.GpsDatabaseHelper;
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
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS)
public class ManagedCardsAuthAndSettlementTests extends BaseTransactionsSetup {

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
    public void CardPurchase_DebitCorporate_Success(final InstrumentType instrumentType) {

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

        String aa = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpendAfterRetry = (int) (availableToSpend - settlementAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

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
    public void CardPurchase_PartialAuthReversalAndSettlement_Success() {

        final Long availableToSpend = 1000L;
        final long authAmount = 100L;
        final long authReversalAmount = 80L;
        final Long settlementAmount = 20L;
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

        simulateAuthReversal(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, authReversalAmount));

        final int expectedAvailableToSpendAfterReversal = (int) (remainingAvailableToSpendAfterAuth + authReversalAmount);
        final int remainingAvailableToSpendAfterAfterReversal =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterReversal);

        final int expectedManagedAccountBalanceAfterReversal =
                (int) (managedCard.getInitialManagedAccountBalance() - authAmount + authReversalAmount);
        final BalanceModel managedAccountBalanceAfterReversal =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, expectedManagedAccountBalanceAfterReversal);

        Assertions.assertEquals(expectedAvailableToSpendAfterReversal, remainingAvailableToSpendAfterAfterReversal);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterReversal, managedAccountBalanceAfterReversal.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterReversal.getActualBalance());

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, settlementAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - settlementAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);

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

    // TODO - To check test
//    @Test
//    public void CardPurchase_PartialAuthReversalAndSettlementExceedingRemainingAuth_Success() {
//
//        final Long availableToSpend = 1000L;
//        final long authAmount = 100L;
//        final long authReversalAmount = 80L;
//        final Long settlementAmount = 21L;
//        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
//
//        final ManagedCardDetails managedCard =
//                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
//
//        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);
//
//        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
//                new CurrencyAmount(corporateCurrency, authAmount));
//
//        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - authAmount);
//
//        final int expectedManagedAccountBalanceAfterAuth =
//                (int) (managedCard.getInitialManagedAccountBalance() - authAmount);
//
//        final int remainingAvailableToSpendAfterAuth =
//                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterAuth);
//
//        final BalanceModel managedAccountBalanceAfterAuth =
//                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
//                        secretKey, corporateAuthenticationToken);
//
//        Assertions.assertNotNull(authorisationId);
//        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
//        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
//        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());
//
//
//        simulateAuthReversal(managedCard.getManagedCardId(), authorisationId,
//                new CurrencyAmount(corporateCurrency, authReversalAmount));
//
//        final int expectedAvailableToSpendAfterReversal = (int) (remainingAvailableToSpendAfterAuth + authReversalAmount);
//        final int remainingAvailableToSpendAfterAfterReversal =
//                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpendAfterReversal);
//
//        final int expectedManagedAccountBalanceAfterReversal =
//                (int) (managedCard.getInitialManagedAccountBalance() - authAmount + authReversalAmount);
//        final BalanceModel managedAccountBalanceAfterReversal =
//                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
//                        secretKey, corporateAuthenticationToken, expectedManagedAccountBalanceAfterReversal);
//
//        Assertions.assertEquals(expectedAvailableToSpendAfterReversal, remainingAvailableToSpendAfterAfterReversal);
//        Assertions.assertEquals(expectedManagedAccountBalanceAfterReversal, managedAccountBalanceAfterReversal.getAvailableBalance());
//        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterReversal.getActualBalance());
//
//
//        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
//                new CurrencyAmount(corporateCurrency, settlementAmount));
//
//        final int expectedAvailableToSpend = (int) (availableToSpend - settlementAmount - purchaseFee);
//
//        final int managedAccountExpectedBalance =
//                (int) (managedCard.getInitialManagedAccountBalance() - settlementAmount - purchaseFee);
//
//        final int remainingAvailableToSpend =
//                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);
//
//        final BalanceModel managedAccountBalance =
//                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
//                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);
//
//        Assertions.assertNotNull(settlementId);
//        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
//        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
//        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
//    }

    @Test
    public void CardPurchase_AuthAndSettlementTransactionCurrencyMismatch_Success() throws SQLException {

        final long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final String cardCurrency = corporateCurrency;
        final String authorisationCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(cardCurrency)).name();
        final String settlementCurrency =
                Currency.getRandomWithExcludedCurrencies(Arrays.asList(Currency.valueOf(cardCurrency), Currency.valueOf(authorisationCurrency))).name();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(authorisationCurrency, purchaseAmount));

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(settlementCurrency, purchaseAmount));

        final int expectedPurchaseAmount = Math.abs(Integer.parseInt(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final int expectedAvailableToSpend = (int) (availableToSpend - expectedPurchaseAmount - purchaseFee);

        opc.junit.helpers.adminnew.AdminHelper.retrySettlement(RetryType.FORCE_NO_LOSS, adminToken, settlementId);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - expectedPurchaseAmount - purchaseFee);

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

    //TODO check beginning of Month/Week etc for the number of days we're subtracting to check previous auths
    @ParameterizedTest
    @EnumSource(value = LimitInterval.class, names = {"WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY", "ALWAYS"})
    @Execution(ExecutionMode.SAME_THREAD)
    public void CardPurchase_SpendLimitReset_Success(final LimitInterval limitInterval) {

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final List<SpendLimitModel> spendLimits =
                Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 250L), limitInterval),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.DAILY));

        setSpendLimit(managedCard.getManagedCardId(), spendLimits, corporateAuthenticationToken);

        final String firstDailyAuth = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount), Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli(), true);

        final String secondDailyAuth = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount), Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(), true);

        final String deniedAuth = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount), Instant.now().toEpochMilli(), false);

        Assertions.assertNotNull(firstDailyAuth);
        Assertions.assertNotNull(secondDailyAuth);
        Assertions.assertEquals("DENIED_SPEND_CONTROL", deniedAuth);

        final List<SpendLimitModel> updatedSpendLimits =
                Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 50L), limitInterval),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY));

        final List<SpendLimitResponseModel> remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, updatedSpendLimits);

        updatedSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    remainingAvailableToSpend.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);

            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());
        });
    }

    @Disabled("Confirmation required from product")
    public void CardPurchase_SpendLimitResetSubtractingLostFees_Success() {

        final Long purchaseAmount = 100L;
        final Long availableToSpend = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();
        final Long depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final int missingAmount = 5;

        final long depositAmount = purchaseAmount + purchaseFee + depositFee - missingAmount;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency,
                        corporateAuthenticationToken, depositAmount);

        final List<SpendLimitModel> spendLimits =
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.DAILY));

        setSpendLimit(managedCard.getManagedCardId(), spendLimits, corporateAuthenticationToken);

        simulatePurchase(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        TestHelper.simulateManagedAccountDeposit(managedCard.getManagedCardModel().getParentManagedAccountId(),
                corporateCurrency, 1000L, secretKey, corporateAuthenticationToken);

        final String deniedAuth = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount), Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli(),
                false);

        final String auth = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseFee - missingAmount),
                Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli(),
                false);

        Assertions.assertEquals("DENIED_SPEND_CONTROL", deniedAuth);
        Assertions.assertNotNull(auth);

        final List<SpendLimitModel> updatedSpendLimits =
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 0L), LimitInterval.DAILY));

        final List<SpendLimitResponseModel> remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, updatedSpendLimits);


        updatedSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    remainingAvailableToSpend.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);

            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());
        });
    }

    @Test
    public void CardPurchase_SpendLimitExceeded_DeniedSpendControl() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 1001L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String code = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount),
                Instant.now().toEpochMilli(),
                false);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, availableToSpend.intValue());

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedCard.getInitialManagedAccountBalance());

        Assertions.assertEquals("DENIED_SPEND_CONTROL", code);
        Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_Manually_Adjust_AvailableToSpend_Success() {

        final String adminToken = AdminService.loginAdmin();
        final long yesterdayTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final List<SpendLimitModel> spendLimits =
                Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 250L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY));

        setSpendLimit(managedCard.getManagedCardId(), spendLimits, corporateAuthenticationToken);

        final String authPreviousDay = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, 10L), yesterdayTimestamp, true);

        Assertions.assertNotNull(authPreviousDay);

        // Assert Yesterday's auth was deducted from ALWAYS interval but not from today's DAILY limit
        assertUpdatedAvailableToSpendLimits(managedCard.getManagedCardId(), Arrays.asList(
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 240L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        // Now, manually adjust -20 from the card (do not specify adjustment id and timestamp)
        ManagedCardsHelper.manuallyAdjustAvailableToSpend(managedCard.getManagedCardId(), -20L, null, null, adminToken);

        // Assert amount was deducted both from ALWAYS interval and from today's DAILY limit
        assertUpdatedAvailableToSpendLimits(managedCard.getManagedCardId(), Arrays.asList(
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 220L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 80L), LimitInterval.DAILY)));

        // Now, manually adjust -40 from the card (using yesterday's timestamp)
        ManagedCardsHelper.manuallyAdjustAvailableToSpend(managedCard.getManagedCardId(), -40L, null, yesterdayTimestamp, adminToken);

        // Assert amount was deducted from ALWAYS interval but not from today's DAILY limit
        assertUpdatedAvailableToSpendLimits(managedCard.getManagedCardId(), Arrays.asList(
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 180L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 80L), LimitInterval.DAILY)));

        final String authToday = simulateAuthWithTimestamp(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, 10L), System.currentTimeMillis(), true);

        Assertions.assertNotNull(authToday);

        // Assert Today's auth was deducted from ALWAYS interval as well as from today's DAILY limit
        assertUpdatedAvailableToSpendLimits(managedCard.getManagedCardId(), Arrays.asList(
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 170L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 70L), LimitInterval.DAILY)));

        // Now, let's reset today's auth as it was meant to deduct 8 instead of 10 from ATS...
        ManagedCardsHelper.manuallyAdjustAvailableToSpend(managedCard.getManagedCardId(), -8L, Long.valueOf(authToday), null, adminToken);

        // Assert that Today's auth value was reset for both ALWAYS and DAILY limit
        assertUpdatedAvailableToSpendLimits(managedCard.getManagedCardId(), Arrays.asList(
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 172L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 72L), LimitInterval.DAILY)));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitCorporate_AuthExpired_Success(final InstrumentType instrumentType) throws SQLException, InterruptedException {
        final Long timestamp = Instant.now().toEpochMilli();
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }
        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

//      Authorisation
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

//      simulate expiry auth
        GpsDatabaseHelper.expireAuthDate(managedCard.getManagedCardId(), timestamp);
        Thread.sleep(31000);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        final String auth_state = AdminHelper.getAuthorisationById(authorisationId, adminToken).jsonPath().get("state");

        Assertions.assertEquals("CLOSED_MANUAL", auth_state);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitConsumer_AuthExpired_Success(final InstrumentType instrumentType) throws SQLException, InterruptedException {
        final Long timestamp = Instant.now().toEpochMilli();
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);
        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }
        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(consumerCurrency, availableToSpend), consumerAuthenticationToken);

//      Authorisation
        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(consumerCurrency, purchaseAmount));

        final int expectedAvailableToSpendAfterAuth = (int) (availableToSpend - purchaseAmount);
        final int expectedManagedAccountBalanceAfterAuth =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount);

        final int remainingAvailableToSpendAfterAuth =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, expectedAvailableToSpendAfterAuth);
        final BalanceModel managedAccountBalanceAfterAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken);

        Assertions.assertNotNull(authorisationId);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterAuth.getActualBalance());

//      simulate expiry auth
        GpsDatabaseHelper.expireAuthDate(managedCard.getManagedCardId(), timestamp);
        Thread.sleep(31000);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, consumerAuthenticationToken);

        final String auth_state = AdminHelper.getAuthorisationById(authorisationId, adminToken).jsonPath().get("state");

        Assertions.assertEquals("CLOSED_MANUAL", auth_state);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_MultipleAuthExpired_Success() throws SQLException, InterruptedException {
        final Long timestamp = Instant.now().toEpochMilli();
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

//      First authorisation
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

//      simulate expiry auth
        GpsDatabaseHelper.expireAuthDate(managedCard.getManagedCardId(), timestamp);
        Thread.sleep(31000);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        final String auth_state = AdminHelper.getAuthorisationById(authorisationId, adminToken).jsonPath().get("state");

        Assertions.assertEquals("CLOSED_MANUAL", auth_state);
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());

//        Second Auth related to first
        final String authorisationIdSecondAuth = simulateAuth(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final BalanceModel managedAccountBalanceAfterSecondAuth =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken);

        final String auth_stateSecondAuth = AdminHelper.getAuthorisationById(authorisationIdSecondAuth, adminToken).jsonPath().get("state");
        final String auth_stateFirstAuth = AdminHelper.getAuthorisationById(authorisationId, adminToken).jsonPath().get("state");

        Assertions.assertEquals("PENDING_SETTLEMENT", auth_stateSecondAuth);
        Assertions.assertEquals("CLOSED_MANUAL", auth_stateFirstAuth);
        Assertions.assertEquals(expectedAvailableToSpendAfterAuth, remainingAvailableToSpendAfterAuth);
        Assertions.assertEquals(expectedManagedAccountBalanceAfterAuth, managedAccountBalanceAfterSecondAuth.getAvailableBalance());
        Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalanceAfterSecondAuth.getActualBalance());
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitCorporateZeroAuth_Success(final InstrumentType instrumentType) throws SQLException, InterruptedException {
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 0L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }
        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount), false);

        Assertions.assertNotNull(authorisationId);

        final String auth_state = AdminHelper.getAuthorisationById(authorisationId, adminToken).jsonPath().get("state");

        //expect zero authorisation to be immediately closed
        Assertions.assertEquals("CLOSED", auth_state);
    }
    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitConsumerZeroAuth_Success(final InstrumentType instrumentType) throws SQLException, InterruptedException {
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 0L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);
        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
        }
        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(consumerCurrency, availableToSpend), consumerAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(consumerCurrency, purchaseAmount), false);

        Assertions.assertNotNull(authorisationId);

        final String auth_state = AdminHelper.getAuthorisationById(authorisationId, adminToken).jsonPath().get("state");

        //expect zero authorisation to be immediately closed
        Assertions.assertEquals("CLOSED", auth_state);
    }


    private void assertUpdatedAvailableToSpendLimits(final String managedCardId, final List<SpendLimitModel> assertableAvailableToSpendLimits) {
        final List<SpendLimitResponseModel> remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCardId, corporateAuthenticationToken, assertableAvailableToSpendLimits);

        assertableAvailableToSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    remainingAvailableToSpend.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);

            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());
        });
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

        final SpendRulesModel spendRulesModel =
                SpendRulesModel.builder().setAllowedMerchantCategories(new ArrayList<>())
                        .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setAllowContactless(true)
                        .setAllowAtm(true)
                        .setAllowECommerce(true)
                        .setAllowCashback(true)
                        .setAllowCreditAuthorisations(true)
                        .setSpendLimit(spendLimit)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
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

    private String simulateAuthWithTimestamp(final String managedCardId,
                                             final CurrencyAmount purchaseAmount,
                                             final Long timestamp,
                                             final boolean isSuccessfulCall) {
        return SimulatorHelper.simulateAuthorisationWithTimestamp(innovatorId, purchaseAmount, managedCardId, timestamp, isSuccessfulCall);
    }

    private String simulateSettlement(final String managedCardId,
                                      final String relatedAuthorisationId,
                                      final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateSettlement(innovatorId, Long.parseLong(relatedAuthorisationId), purchaseAmount, managedCardId);
    }

    private void simulateAuthReversal(final String managedCardId,
                                      final String relatedAuthorisationId,
                                      final CurrencyAmount purchaseAmount) {
        SimulatorHelper.simulateAuthReversalById(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId);
    }

    private void simulatePurchase(final String managedCardId,
                                  final CurrencyAmount purchaseAmount) {
        SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                purchaseAmount);
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

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
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
}