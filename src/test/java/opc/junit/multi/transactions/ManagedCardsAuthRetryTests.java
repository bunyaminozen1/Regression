package opc.junit.multi.transactions;

import commons.enums.State;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.RetryAuthorisationModel;
import opc.models.admin.RetryAuthorisationsModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardAuthModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

@Tag(MultiTags.MANAGED_CARDS_TRANSACTIONS)
public class ManagedCardsAuthRetryTests extends BaseTransactionsSetup {

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
    public void CardPurchase_DebitAuthorisation_NotEnoughBalance_ForceLoss_success() {

        final Long availableToSpend = 100000L;
        final Long purchaseAmount = 11000L;

        final ManagedCardDetails managedCard = createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final SimulateCardAuthModel simulateCardAuthModel = SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(corporateCurrency, purchaseAmount))
                        .setInvalidAuthCode("NOT_ENOUGH_BALANCE")
                        .build();

        String authorisationId = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "RETRY");

        RetryAuthorisationModel retryForceLoss = new RetryAuthorisationModel()
                .setRetryType("FORCE_LOSS")
                .setNote("Retrying with force loss");

        AdminHelper.retryAuthorisation(retryForceLoss, authorisationId, adminToken);
        assertAuthorisationState(authorisationId, State.PENDING_SETTLEMENT);
    }

    @Test
    public void CardPurchase_DebitAuthorisations_NotEnoughBalance_ForceLoss_success() {

        final Long availableToSpend = 100000L;
        final Long purchaseAmount = 11000L;

        final ManagedCardDetails managedCard = createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final SimulateCardAuthModel simulateCardAuthModel = SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(corporateCurrency, purchaseAmount))
                .setInvalidAuthCode("NOT_ENOUGH_BALANCE")
                .build();

        String authorisationId1 = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "RETRY");
        String authorisationId2 = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "RETRY");

        RetryAuthorisationsModel retryForceLoss = new RetryAuthorisationsModel()
                .setAuthorisationId(List.of(authorisationId1, authorisationId2))
                .setRetryType("FORCE_LOSS")
                .setNote("Retrying with force loss");

        AdminHelper.retryAuthorisations(retryForceLoss, adminToken);

        final State expectedState = State.PENDING_SETTLEMENT;
        assertAuthorisationState(authorisationId1, expectedState);
        assertAuthorisationState(authorisationId2, expectedState);
    }

    @Test
    public void CardPurchase_PrepaidAuthorisation_NotEnoughBalance_ForceLoss_success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 1100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, depositAmount, 1);

        final SimulateCardAuthModel simulateCardAuthModel = SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(consumerCurrency, purchaseAmount))
                .setInvalidAuthCode("NOT_ENOUGH_BALANCE")
                .build();

        String authorisationId = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "RETRY");

        RetryAuthorisationModel retryForceLoss = new RetryAuthorisationModel()
                .setRetryType("FORCE_LOSS")
                .setNote("Retrying with force loss");

        AdminHelper.retryAuthorisation(retryForceLoss, authorisationId, adminToken);

        assertAuthorisationState(authorisationId, State.PENDING_SETTLEMENT);
    }

    @Test
    public void CardPurchase_PrepaidAuthorisations_NotEnoughBalance_ForceLoss_success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 1100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, depositAmount, 1);

        final SimulateCardAuthModel simulateCardAuthModel = SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(consumerCurrency, purchaseAmount))
                .setInvalidAuthCode("NOT_ENOUGH_BALANCE")
                .build();

        String authorisationId1 = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "RETRY");
        String authorisationId2 = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "RETRY");

        RetryAuthorisationsModel retryForceLoss = new RetryAuthorisationsModel()
                .setAuthorisationId(List.of(authorisationId1, authorisationId2))
                .setRetryType("FORCE_LOSS")
                .setNote("Retrying with force loss");

        AdminHelper.retryAuthorisations(retryForceLoss, adminToken);

        final State expectedState = State.PENDING_SETTLEMENT;
        assertAuthorisationState(authorisationId1, expectedState);
        assertAuthorisationState(authorisationId2, expectedState);
    }

    @Test
    public void CardPurchase_DebitAuthorisation_PositiveTxnAmount_RetryWithImprovement_success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);

        transferFundsToCard(consumerAuthenticationToken, IdentityType.CONSUMER, managedCard.getManagedCardId(),
                consumerCurrency, depositAmount, 1);

        final SimulateCardAuthModel simulateCardAuthModel = SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(consumerCurrency, purchaseAmount))
                .setInvalidAuthCode("TRANSACTION_AMOUNT_POSITIVE")
                .build();

        String authorisationId = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "INVALID");

        RetryAuthorisationModel retryWithImprovement = new RetryAuthorisationModel()
                .setRetryType("RETRY_WITH_IMPROVEMENT")
                .setNote("Retrying with improvement")
                .setTransactionAmount(new CurrencyAmount().setAmount(-100L).setCurrency(corporateCurrency));

        AdminHelper.retryAuthorisation(retryWithImprovement, authorisationId, adminToken);

        assertAuthorisationState(authorisationId, State.REVALIDATE);
    }

    @Test
    public void CardPurchase_PrepaidAuthorisation_PositiveTxnAmount_RetryWithImprovement_success() {

        final Long availableToSpend = 100000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard = createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final SimulateCardAuthModel simulateCardAuthModel = SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(corporateCurrency, purchaseAmount))
                .setInvalidAuthCode("TRANSACTION_AMOUNT_POSITIVE")
                .build();

        String authorisationId = SimulatorHelper.simulateAuthorisationWithState(innovatorId, simulateCardAuthModel, managedCard.getManagedCardId(), "INVALID");

        RetryAuthorisationModel retryWithImprovement = new RetryAuthorisationModel()
                .setRetryType("RETRY_WITH_IMPROVEMENT")
                .setNote("Retrying with improvement")
                .setTransactionAmount(new CurrencyAmount().setAmount(-100L).setCurrency(corporateCurrency));

        AdminHelper.retryAuthorisation(retryWithImprovement, authorisationId, adminToken);

        assertAuthorisationState(authorisationId, State.REVALIDATE);
    }

    private void assertAuthorisationState(final String authorisationId,
                                          final State expectedState) {
        TestHelper.ensureAsExpected(30,
                () -> AdminHelper.getAuthorisationById(authorisationId, adminToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equalsIgnoreCase(expectedState.name()),
                Optional.of(String.format("Authorisation with id %s not in state %s as expected.", authorisationId, expectedState)));
    }

    private void setSpendLimit(final String managedCardId,
                               final CurrencyAmount spendLimit,
                               final String authenticationToken) {
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
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
