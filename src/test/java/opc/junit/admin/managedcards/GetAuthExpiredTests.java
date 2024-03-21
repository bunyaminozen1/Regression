package opc.junit.admin.managedcards;

import opc.enums.opc.LimitInterval;
import opc.junit.database.GpsDatabaseHelper;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.AuthorisationsRequestModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.PagingModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Execution(ExecutionMode.SAME_THREAD)
public class GetAuthExpiredTests extends BaseManagedCardsSetup {
    private static String corporateAuthenticationToken;
    private static String corporateCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Test
    public void GetExpiredAuthByProviderLinkId_DebitCorporate_AuthExpired_Success() throws SQLException {
        final Long timestamp = Instant.now().toEpochMilli();
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

//      Authorisation
        simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

//      simulate expiry auth
        GpsDatabaseHelper.expireAuthDate(managedCard.getManagedCardId(), timestamp);

        final AuthorisationsRequestModel authorisationsRequestModel = AuthorisationsRequestModel.defaultAuthorisationsRequestModel(new PagingModel(0, 1));
        AdminService.authorisationExpiryFind(authorisationsRequestModel, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("systemExpiredAuthorisationResponseEntries[0].cardId", equalTo(managedCard.getManagedCardId()))
                .body("systemExpiredAuthorisationResponseEntries[0].providerLinkId", notNullValue());

        //get the providerLinkId
        final String providerLinkId = AdminHelper.getProviderLinkId(authorisationsRequestModel, adminToken);
        final AuthorisationsRequestModel authorisationsRequestModelProviderLinkId = AuthorisationsRequestModel.defaultAuthorisationsRequestModel(new PagingModel(0, 1), providerLinkId);

        AdminService.authorisationExpiryFind(authorisationsRequestModelProviderLinkId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("systemExpiredAuthorisationResponseEntries[0].cardId", equalTo(managedCard.getManagedCardId()))
                .body("systemExpiredAuthorisationResponseEntries[0].providerLinkId", equalTo(providerLinkId));

    }

    @Test
    public void GetExpiredAuthByProviderLinkId_DebitCorporate_JobClosed_Success() throws SQLException, InterruptedException {
        final Long timestamp = Instant.now().toEpochMilli();
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

//      Authorisation
        simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

//      simulate expiry auth
        GpsDatabaseHelper.expireAuthDate(managedCard.getManagedCardId(), timestamp);
        Thread.sleep(31000);

        final AuthorisationsRequestModel authorisationsRequestModel = AuthorisationsRequestModel.defaultAuthorisationsRequestModel(new PagingModel(0, 1));
        AdminService.authorisationExpiryFind(authorisationsRequestModel, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0));

    }

    @Test
    public void GetExpiredAuthByProviderLinkId_DebitCorporate_NoAuthExpired_Success() {
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        setSpendLimit(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, availableToSpend), corporateAuthenticationToken);

//      Authorisation
        simulateAuth(managedCard.getManagedCardId(), null,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final AuthorisationsRequestModel authorisationsRequestModel = AuthorisationsRequestModel.defaultAuthorisationsRequestModel(new PagingModel(0, 1));
        AdminService.authorisationExpiryFind(authorisationsRequestModel, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0));

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

    private String simulateAuth(final String managedCardId,
                                final String relatedAuthorisationId,
                                final CurrencyAmount purchaseAmount) {
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, relatedAuthorisationId, managedCardId);
    }

}
