package opc.junit.adminnew.settlementsretry;

import commons.enums.Currency;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.RetryType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.SettlementRetryModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

public class SettlementRetryTests extends BaseSettlementRetrySetup {
    private static String corporateAuthenticationToken;
    private static String corporateCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Test
    public void SettlementRetry_CardChange_HappyPath() {

        final long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final String settlementCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(corporateCurrency)).name();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(settlementCurrency, purchaseAmount));

        AdminService.getSettlement(adminToken, settlementId)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("RETRY"));

        final SettlementRetryModel settlementRetryModel =  SettlementRetryModel.SettlementRetryCardModel("THIS IS A TEST", RetryType.RETRY_WITH_IMPROVEMENT, purchaseAmount.toString(), corporateCurrency);

        AdminService.settlementRetry(settlementRetryModel, adminToken, settlementId)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService.getSettlement(adminToken, settlementId)
                .then()
                .statusCode(SC_OK)
                .body("details.cardAmount.amount", equalTo(purchaseAmount.intValue()))
                .body("details.cardAmount.currency", equalTo(corporateCurrency))
                .body("state", equalTo("REVALIDATE"));
    }

    @Test
    public void SettlementRetry_TransactionChange_HappyPath() {

        final long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final String settlementCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(corporateCurrency)).name();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(settlementCurrency, purchaseAmount));

        AdminService.getSettlement(adminToken, settlementId)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("RETRY"));

        final SettlementRetryModel settlementRetryModel =  SettlementRetryModel.SettlementRetryTransactionModel("THIS IS A TEST", RetryType.RETRY_WITH_IMPROVEMENT, purchaseAmount.toString(), corporateCurrency);

        AdminService.settlementRetry(settlementRetryModel, adminToken, settlementId)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService.getSettlement(adminToken, settlementId)
                .then()
                .statusCode(SC_OK)
                .body("details.transactionAmount.amount", equalTo(purchaseAmount.intValue()))
                .body("details.transactionAmount.currency", equalTo(corporateCurrency))
                .body("state", equalTo("REVALIDATE"));
    }

    private void setSpendLimit(final String managedCardId,
                               final CurrencyAmount spendLimit,
                               final String authenticationToken) {
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private String simulateAuth(final String managedCardId,
                                final String relatedAuthorisationId,
                                final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId);
    }

    private String simulateSettlement(final String managedCardId,
                                      final String relatedAuthorisationId,
                                      final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateSettlement(innovatorId, Long.parseLong(relatedAuthorisationId), purchaseAmount, managedCardId);
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
}
