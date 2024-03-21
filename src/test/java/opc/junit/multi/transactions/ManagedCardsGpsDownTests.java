package opc.junit.multi.transactions;

import opc.enums.opc.CannedResponseType;
import opc.enums.opc.IdentityType;
import opc.junit.database.GpsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SetCannedResponseModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.SAME_THREAD)
public class ManagedCardsGpsDownTests extends BaseTransactionsSetup {

    private static String corporateAuthenticationToken;
    private static String corporateCurrency;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void CardPurchase_PrepaidGpsDown_Success() throws SQLException {

        // TODO - Remove profile id when fee issue is fixed
        final String corporatePrepaidManagedCardsProfileId = "106566807206035466";

        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = 0L;

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount, 1);

        setCannedResponse(CannedResponseType.MISSING);

        final String authorisationId = simulateAuth(managedCard.getManagedCardId(),
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        setCannedResponse(CannedResponseType.MISSING);

        final String settlementId = simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int managedCardExpectedBalance =
                (int) (depositAmount - purchaseAmount - purchaseFee);

        final BalanceModel managedCardBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, managedCardExpectedBalance, managedCardExpectedBalance);

        assertNotNull(settlementId);
        assertEquals(managedCardExpectedBalance, managedCardBalance.getAvailableBalance());
        assertEquals(managedCardExpectedBalance, managedCardBalance.getActualBalance());

        final Map<Integer, Map<String, String>> gpsCardDetails = GpsDatabaseHelper.getInstrument(managedCard.getManagedCardId());

        gpsCardDetails.forEach((key, value) -> {
            assertEquals(depositAmount, Integer.parseInt(value.get("balance")));
            assertEquals(0, Integer.parseInt(value.get("balance_synced")));
        });

        setCannedResponse(CannedResponseType.SUCCESS);

        simulatePurchase(managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, purchaseAmount));

        final Map<Integer, Map<String, String>> gpsCardDetailsGpsUp = GpsDatabaseHelper.getInstrument(managedCard.getManagedCardId());

        gpsCardDetailsGpsUp.forEach((key, value) -> {
            assertEquals(depositAmount - purchaseAmount - purchaseAmount, Integer.parseInt(value.get("balance")));
            assertEquals(1, Integer.parseInt(value.get("balance_synced")));
        });
    }

    @AfterEach
    public void ClearCannedResponse(){
        SimulatorService.clearManagedCardCannedResponse(innovatorId);
    }

    private String simulateAuth(final String managedCardId,
                                final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, null, managedCardId);
    }

    private static void setCannedResponse(final CannedResponseType cannedResponse){
        SimulatorService.setManagedCardCannedResponse(new SetCannedResponseModel(cannedResponse), innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.getManagedCardCannedResponse(innovatorId),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("type").equals(cannedResponse.name()),
                Optional.of(String.format("Expecting 200 with a caned response of type %s, check logged payload", cannedResponse.name())));
    }

    private String simulateSettlement(final String managedCardId,
                                      final String relatedAuthorisationId,
                                      final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateSettlement(innovatorId, Long.parseLong(relatedAuthorisationId), purchaseAmount, managedCardId);
    }

    private void simulatePurchase(final String managedCardId,
                                    final CurrencyAmount purchaseAmount){
        SimulatorHelper.simulateCardPurchaseById(secretKey, managedCardId, purchaseAmount);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();

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
