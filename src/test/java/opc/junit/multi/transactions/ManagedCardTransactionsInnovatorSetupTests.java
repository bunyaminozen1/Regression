package opc.junit.multi.transactions;

import io.restassured.path.json.JsonPath;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.TransactionAmountModel;
import opc.models.innovator.SpendRulesModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.SpendLimitModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.apache.http.HttpStatus.SC_OK;

@Execution(ExecutionMode.SAME_THREAD)
@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS_INNOVATOR_SETUP)
public class ManagedCardTransactionsInnovatorSetupTests extends BaseTransactionRulesSetup {

    private static String consumerAuthenticationToken;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup(){
        consumerSetup();
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Nested
    @DisplayName("Sequential Tests")
    class SequentialTests {

        @Test
        public void CardPurchase_InnovatorProfileRules_Success(){

            final Long purchaseAmount = 100L;
            final Long availableToSpend = 10000L;
            final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount();

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel profileSpendRulesModel =
                    getDefaultSpendRulesModel().build();

            final SpendRulesModel cardSpendRulesModel =
                    SpendRulesModel.builder()
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS ))).build();

            InnovatorHelper.setProfileSpendRules(profileSpendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);
            InnovatorHelper.addCardSpendRules(cardSpendRulesModel,
                    InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword),
                    managedCard.getManagedCardId(),
                    x -> x.statusCode() == SC_OK &&
                            x.jsonPath().getString("cardLevelSpendRules.spendLimit.limitAmount[0].amount").equals(String.valueOf(availableToSpend)));

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

            final int managedAccountExpectedBalance =
                    (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

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
        public void CardPurchase_SetProfileRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardPurchase_SetProfileRulesPrepaidMerchantCategoryBlocked_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardPurchase_SetProfileRulesDebitMerchantCategoryNotAllowed_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardPurchase_SetProfileRulesPrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardAuthReversal_SetProfileRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardPurchase_SetProfileRulesDebitMerchantCountryBlocked_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCountries(Collections.singletonList("MT")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardPurchase_SetProfileRulesDebitMerchantCountryNotAllowed_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCountries(Collections.singletonList("IT")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardPurchase_SetProfileRulesDebitMerchantCountryBothInAllowedAndBlocked_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCountries(Collections.singletonList("MT"))
                            .setAllowedMerchantCountries(Collections.singletonList("MT")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardPurchase_SetProfileRulesPrepaidMerchantCountryBlocked_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCountries(Collections.singletonList("MT")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardPurchase_SetProfileRulesPrepaidMerchantCountryNotAllowed_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCountries(Collections.singletonList("IT")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardPurchase_SetProfileRulesPrepaidMerchantCountryBothInAllowedAndBlocked_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCountries(Collections.singletonList("MT"))
                            .setAllowedMerchantCountries(Collections.singletonList("MT")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardAuthReversal_SetProfileRulesDebitMerchantCategoryNotAllowed_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardAuthReversal_SetProfileRulesPrepaidMerchantCategoryBlocked_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardAuthReversal_SetProfileRulesPrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardMerchantRefund_SetProfileRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

            final Long purchaseAmount = 10L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardMerchantRefund_SetProfileRulesDebitMerchantCategoryNotAllowed_DeniedSpendControl(){

            final Long purchaseAmount = 10L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            setDefaultCardRules(managedCard.getManagedCardId());

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
        public void CardMerchantRefund_SetProfileRulesPrepaidMerchantCategoryBlocked_DeniedSpendControl(){

            final Long cardBalance = 100L;
            final Long purchaseAmount = 10L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

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
        public void CardMerchantRefund_SetProfileRulesPrepaidMerchantCategoryNotAllowed_DeniedSpendControl(){

            final Long cardBalance = 100L;
            final Long purchaseAmount = 10L;

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCategories(Arrays.asList("9999", "8888")).build();

            InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

            final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final BalanceModel managedCardBalance =
                    ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                            secretKey, consumerAuthenticationToken, cardBalance.intValue());

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
            Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
            Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
        }
    }

    @Execution(ExecutionMode.CONCURRENT)
    @Nested
    @DisplayName("Parallel Tests")
    class ParallelTests {

        @Test
        public void CardPurchase_InnovatorCardRules_Success(){

            final Long purchaseAmount = 100L;
            final Long availableToSpend = 10000L;
            final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount();

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS ))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

            final int managedAccountExpectedBalance =
                    (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

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
        @EnumSource(value = LimitInterval.class)
        public void CardPurchase_DebitCardSpendLimitExceeded_DeniedSpendControl(final LimitInterval interval){

            final Long purchaseAmount = 100L;
            final Long availableToSpend = 90L;

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), interval))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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
        @MethodSource("opc.junit.multi.transactions.ManagedCardTransactionsInnovatorSetupTests#purchaseLimits")
        public void CardPurchase_DebitMaximumMinimumLimitExceeded_DeniedSpendControl(final InstrumentType instrumentType,
                                                                                     final Long purchaseAmount){

            final Long availableToSpend = 1000L;

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            if (instrumentType.equals(InstrumentType.PHYSICAL)){
                ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
            }

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setMinTransactionAmount(TransactionAmountModel.builder().setValue(20L).setHasValue(true).build())
                            .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(99L).setHasValue(true).build())
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final int remainingAvailableToSpend =
                    ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, availableToSpend.intValue());

            final BalanceModel managedAccountBalance =
                    ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                            secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
            Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
            Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
            Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
        }

        @ParameterizedTest
        @MethodSource("opc.junit.multi.transactions.ManagedCardTransactionsInnovatorSetupTests#purchaseLimits")
        public void CardPurchase_PrepaidMaximumMinimumLimitExceeded_DeniedSpendControl(final InstrumentType instrumentType,
                                                                                       final Long purchaseAmount){

            final Long depositAmount = 1000L;

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, depositAmount, 1);

            if (instrumentType.equals(InstrumentType.PHYSICAL)){
                ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);
            }

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setMinTransactionAmount(TransactionAmountModel.builder().setValue(20L).setHasValue(true).build())
                            .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(99L).setHasValue(true).build())
                            .build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final BalanceModel managedAccountBalance =
                    ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                            secretKey, consumerAuthenticationToken, depositAmount.intValue());

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
            Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getAvailableBalance());
            Assertions.assertEquals(depositAmount.intValue(), managedAccountBalance.getActualBalance());
        }

        @ParameterizedTest
        @ValueSource(longs = {20L, 21L, 99L, 100L})
        public void CardPurchase_DebitPurchaseAmountWithinLimits_Success(final Long purchaseAmount){

            final Long availableToSpend = 1000L;
            final Long purchaseFee = TestHelper.getFees(consumerCurrency).get(FeeType.PURCHASE_FEE).getAmount();

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setMinTransactionAmount(TransactionAmountModel.builder().setValue(20L).setHasValue(true).build())
                            .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(100L).setHasValue(true).build())
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final int expectedAvailableToSpend = (int)(availableToSpend - purchaseAmount - purchaseFee);

            final int managedAccountExpectedBalance =
                    (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

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
        public void CardPurchase_DebitMaxMinLimitSetToZero_DeniedSpendControl(){

            final Long availableToSpend = 1000L;
            final Long purchaseAmount = 20L;

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setMinTransactionAmount(TransactionAmountModel.builder().setValue(0L).setHasValue(true).build())
                            .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(0L).setHasValue(true).build())
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, availableToSpend), LimitInterval.ALWAYS))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

            final String purchaseCode = simulatePurchase(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final int remainingAvailableToSpend =
                    ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, availableToSpend.intValue());

            final BalanceModel managedAccountBalance =
                    ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                            secretKey, consumerAuthenticationToken, managedCard.getInitialManagedAccountBalance());

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
            Assertions.assertEquals(availableToSpend, remainingAvailableToSpend);
            Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getAvailableBalance());
            Assertions.assertEquals(managedCard.getInitialManagedAccountBalance(), managedAccountBalance.getActualBalance());
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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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
        public void CardPurchase_SetCardRulesDebitMerchantCountryNotAllowed_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCountries(Collections.singletonList("IT"))
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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
        public void CardPurchase_SetCardRulesDebitMerchantCountryBothBlockedAndAllowed_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCountries(Collections.singletonList("MT"))
                            .setAllowedMerchantCountries(Collections.singletonList("MT"))
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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
        public void CardPurchase_SetCardRulesPrepaidMerchantCountryNotAllowed_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setAllowedMerchantCountries(Collections.singletonList("IT"))
                            .build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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
        public void CardPurchase_SetCardRulesPrepaidMerchantCountryBothInAllowedAndBlocked_DeniedSpendControl(){

            final Long cardBalance = 1000L;
            final Long purchaseAmount = 100L;

            final ManagedCardDetails managedCard =
                    createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                    consumerCurrency, cardBalance, 1);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCountries(Collections.singletonList("MT"))
                            .setAllowedMerchantCountries(Collections.singletonList("MT"))
                            .build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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
        public void CardAuthReversal_SetCardRulesDebitMerchantCategoryBlocked_DeniedSpendControl(){

            final Long purchaseAmount = 100L;

            final ManagedCardDetails managedCard =
                    createFundedManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

            final SpendRulesModel spendRulesModel =
                    getDefaultSpendRulesModel()
                            .setBlockedMerchantCategories(Collections.singletonList("5399"))
                            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.DAILY))).build();

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

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

            InnovatorHelper.addCardSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCard.getManagedCardId());

            final String purchaseCode = simulateMerchantRefund(managedCard.getManagedCardId(),
                    new CurrencyAmount(consumerCurrency, purchaseAmount));

            final BalanceModel managedCardBalance =
                    ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                            secretKey, consumerAuthenticationToken, cardBalance.intValue());

            Assertions.assertEquals("DENIED_SPEND_CONTROL", purchaseCode);
            Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getAvailableBalance());
            Assertions.assertEquals(cardBalance.intValue(), managedCardBalance.getActualBalance());
        }
    }

    @AfterAll
    public static void Reset(){
        final SpendRulesModel spendRulesModel =
                SpendRulesModel
                        .builder()
                        .setAllowedMerchantCategories(new ArrayList<>())
                        .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setAllowContactless("TRUE")
                        .setAllowAtm("TRUE")
                        .setAllowECommerce("TRUE")
                        .setAllowCashback("TRUE")
                        .setAllowCreditAuthorisations("TRUE")
                        .setAllowedMerchantCountries(Collections.singletonList("MT"))
                        .setBlockedMerchantCountries(Collections.singletonList("IT"))
                        .build();

        InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerDebitManagedCardsProfileId);
        InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, consumerPrepaidManagedCardsProfileId);
        InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, corporateDebitManagedCardsProfileId);
        InnovatorHelper.setProfileSpendRules(spendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId, corporatePrepaidManagedCardsProfileId);
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
                .setAllowContactless("TRUE")
                .setAllowAtm("TRUE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("TRUE")
                .setAllowCreditAuthorisations("TRUE")
                .setAllowedMerchantCountries(Collections.singletonList("MT"))
                .setBlockedMerchantCountries(Collections.singletonList("IT"))
                .setSpendLimit(new ArrayList<>());
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

    private void setDefaultCardRules(final String managedCardId) {
        final SpendRulesModel cardSpendRulesModel =
                SpendRulesModel
                        .builder()
                        .setAllowedMerchantCategories(new ArrayList<>())
                        .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setAllowContactless("TRUE")
                        .setAllowAtm("TRUE")
                        .setAllowECommerce("TRUE")
                        .setAllowCashback("TRUE")
                        .setAllowCreditAuthorisations("TRUE")
                        .setAllowedMerchantCountries(new ArrayList<>())
                        .setBlockedMerchantCountries(new ArrayList<>())
                        .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 10000L), LimitInterval.ALWAYS ))).build();

        InnovatorHelper.addCardSpendRules(cardSpendRulesModel, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), managedCardId);
    }

    private static Stream<Arguments> purchaseLimits() {
        return Stream.of(Arguments.of(InstrumentType.PHYSICAL, 100L),
                Arguments.of(InstrumentType.VIRTUAL, 19L));
    }
}
