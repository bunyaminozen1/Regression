package opc.junit.helpers.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.enums.Currency;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import opc.enums.opc.AcceptedResponse;
import opc.enums.opc.CardBureau;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.AvailableToSpendManualAdjustModel;
import opc.models.admin.AvailableToSpendManualAdjustModel.Builder;
import opc.models.innovator.CreateUnassignedCardBatchModel;
import opc.models.innovator.UnassignedCardResponseModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendLimitResponseModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.models.testmodels.UnassignedManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;

public class ManagedCardsHelper {

    public static void upgradeAndActivateManagedCardToPhysical(final String secretKey,
                                                               final String managedCardId,
                                                               final String authenticationToken) {

        upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, authenticationToken,
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build());
    }

    public static void upgradeAndActivateManagedCardToPhysical(final String secretKey,
                                                               final String managedCardId,
                                                               final String authenticationToken,
                                                               final PhysicalCardAddressModel physicalCardAddressModel) {

        upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, authenticationToken, physicalCardAddressModel, CardBureau.NITECREST);
    }

    public static void upgradeAndActivateManagedCardToPhysical(final String secretKey,
                                                               final String managedCardId,
                                                               final String authenticationToken,
                                                               final PhysicalCardAddressModel physicalCardAddressModel,
                                                               final CardBureau cardBureau) {

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setDeliveryAddress(physicalCardAddressModel)
                        .setProductReference(cardBureau.getProductReference())
                        .setCarrierType(cardBureau.getCarrierType())
                        .build();

        TestHelper.ensureAsExpected(200,
                () -> ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, authenticationToken),
                SC_OK);

        TestHelper.ensureAsExpected(60,
                () -> ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, authenticationToken),
                SC_OK);
    }

    public static String createManagedCard(final CreateManagedCardModel createManagedCardModel,
                                           final String secretKey,
                                           final String authenticationToken) {
        return
                TestHelper.ensureAsExpected(60,
                        () -> ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                        .jsonPath()
                        .get("id");
    }

    public static String createManagedCard(final String profile,
                                           final String currency,
                                           final String secretKey,
                                           final String authenticationToken) {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(profile, currency).build();
        return
                TestHelper.ensureAsExpected(15,
                        () -> ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                        .jsonPath()
                        .get("id");
    }

    public static String createDebitManagedCard(final String profile,
                                                final String managedAccountId,
                                                final String secretKey,
                                                final String authenticationToken) {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreateDebitManagedCardModel(profile, managedAccountId).build();

        return createDebitManagedCard(createManagedCardModel, secretKey, authenticationToken);
    }

    public static String createDebitManagedCard(final CreateManagedCardModel createManagedCardModel,
                                                final String secretKey,
                                                final String authenticationToken) {

        return
                TestHelper.ensureAsExpected(15,
                        () -> ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                        .jsonPath()
                        .get("id");
    }

    public static String createPrepaidManagedCard(final String profile,
                                                  final String currency,
                                                  final String secretKey,
                                                  final String authenticationToken){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(profile, currency).build();
        return createPrepaidManagedCard(createManagedCardModel, secretKey, authenticationToken);
    }

    public static String createPrepaidManagedCard(final CreateManagedCardModel createManagedCardModel,
                                                  final String secretKey,
                                                  final String authenticationToken){
        return
                TestHelper.ensureAsExpected(15,
                        () -> ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                        .jsonPath()
                        .get("id");
    }

    public static void blockManagedCard(final String secretKey,
                                        final String managedCardId,
                                        final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.blockManagedCard(secretKey, managedCardId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void removeManagedCard(final String secretKey,
                                          final String managedCardId,
                                          final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.removeManagedCard(secretKey, managedCardId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void activateManagedCard(final String secretKey,
                                           final String managedCardId,
                                           final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService
                        .activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                                secretKey, managedCardId, authenticationToken),
                SC_OK);
    }

    public static void upgradeManagedCardToPhysical(final String secretKey,
                                                    final String managedCardId,
                                                    final String authenticationToken) {

        upgradeManagedCardToPhysical(secretKey, managedCardId, authenticationToken,
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build());
    }

    public static UpgradeToPhysicalCardModel upgradeManagedCardToPhysical(final String secretKey,
                                                    final String managedCardId,
                                                    final String authenticationToken,
                                                    final PhysicalCardAddressModel physicalCardAddressModel) {

        return upgradeManagedCardToPhysical(secretKey, managedCardId, authenticationToken, physicalCardAddressModel, CardBureau.NITECREST);
    }

    public static UpgradeToPhysicalCardModel upgradeManagedCardToPhysical(final String secretKey,
                                                    final String managedCardId,
                                                    final String authenticationToken,
                                                    final PhysicalCardAddressModel physicalCardAddressModel,
                                                    final CardBureau cardBureau) {

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setProductReference(cardBureau.getProductReference())
                        .setCarrierType(cardBureau.getCarrierType())
                        .setDeliveryAddress(physicalCardAddressModel).build();

        TestHelper.ensureAsExpected(200,
                () -> ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, authenticationToken),
                SC_OK);

        return upgradeToPhysicalCardModel;
    }

    public static void reportLostCard(final String secretKey,
                                      final String managedCardId,
                                      final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.reportLostCard(secretKey, managedCardId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void reportStolenCard(final String secretKey,
                                        final String managedCardId,
                                        final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.reportStolenCard(secretKey, managedCardId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void replaceLostCard(final String secretKey,
                                       final String managedCardId,
                                       final String authenticationToken) {

        reportLostCard(secretKey, managedCardId, authenticationToken);

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                        secretKey, managedCardId, authenticationToken),
                SC_OK);
    }

    public static void replaceDamagedCard(final String secretKey,
                                          final String managedCardId,
                                          final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                        secretKey, managedCardId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void setSpendLimit(final SpendRulesModel spendRulesModel,
                                     final String secretKey,
                                     final String managedCardId,
                                     final String authenticationToken){
        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, authenticationToken, Optional.empty()),
                SC_NO_CONTENT);
    }

    public static void setDefaultDebitSpendLimit(final CurrencyAmount availableToSpend,
                                                 final String secretKey,
                                                 final String managedCardId,
                                                 final String authenticationToken){

        final SpendRulesModel spendRulesModel = SpendRulesModel
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
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(availableToSpend, LimitInterval.ALWAYS)))
                .build();

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, authenticationToken, Optional.empty()),
                SC_NO_CONTENT);
    }

    public static void setDefaultPrepaidSpendLimit(final String secretKey,
                                                   final String managedCardId,
                                                   final String authenticationToken){

        final SpendRulesModel spendRulesModel = SpendRulesModel
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
                .setSpendLimit(new ArrayList<>())
                .build();

        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, authenticationToken, Optional.empty()),
                SC_NO_CONTENT);
    }

    @Deprecated
    public static int getAvailableToSpend(final String secretKey,
                                                             final String managedCardId,
                                                             final String authenticationToken){
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken),
                SC_OK)
                .jsonPath().get("availableToSpend[0].value.amount");
    }

    public static int getAvailableToSpend(final String secretKey,
                                          final String managedCardId,
                                          final String authenticationToken,
                                          final int expectedAvailableToSpend){

        return TestHelper.ensureAsExpected(60,
                        () -> ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken),
                        x -> x.statusCode() == SC_OK && x.jsonPath().get("availableToSpend[0].value.amount")
                        .equals(expectedAvailableToSpend),
                Optional.of(String.format("Expecting 200 with an ATS amount of %s for card id %s, check logged payload", expectedAvailableToSpend, managedCardId)))
                .jsonPath().get("availableToSpend[0].value.amount");
    }

    public static List<SpendLimitResponseModel> getAvailableToSpendList(final String secretKey,
                                                                        final String managedCardId,
                                                                        final String authenticationToken,
                                                                        final List<SpendLimitModel> spendLimits){

        final List<SpendLimitResponseModel>[] spendLimitsResponse = new List[]{new ArrayList<>()};

        try {
            await().atMost(30, TimeUnit.SECONDS).with().pollInterval(Duration.ONE_SECOND).until(() ->{
                final Response response =
                        ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken);

                spendLimitsResponse[0] =
                        Arrays.asList(new ObjectMapper()
                                .convertValue(response.jsonPath().get("availableToSpend"), SpendLimitResponseModel[].class
                                ));

                final Map<LimitInterval, Long> expectedLimits = new HashMap<>();
                spendLimits.forEach(spendLimit ->
                    expectedLimits.put(LimitInterval.valueOf(spendLimit.getInterval()), spendLimit.getValue().getAmount())
                );

                final Map<LimitInterval, Long> actualLimits = new HashMap<>();
                spendLimitsResponse[0].forEach(spendLimit ->
                        actualLimits.put(LimitInterval.valueOf(spendLimit.getInterval()), Long.parseLong(spendLimit.getValue().get("amount")))
                );

                return (response.statusCode() == SC_OK && new TreeMap<>(expectedLimits).equals(new TreeMap<>(actualLimits)));
            });
        } catch (Exception e) {
            // We don't need to throw the exception so that the available to spend is returned even if it does not match.
        }

        return spendLimitsResponse[0];
    }

    public static void manuallyAdjustAvailableToSpend(final String managedCardId,
                                                      final long adjustmentAmount,
                                                      final Long adjustmentId,
                                                      final Long adjustmentTimestamp,
                                                      final String authenticationToken) {

        final Builder builder = AvailableToSpendManualAdjustModel.builder();

        builder.setAdjustmentAmount(adjustmentAmount);
        if (adjustmentId != null) {
            builder.setAdjustmentId(adjustmentId);
        }
        if (adjustmentTimestamp != null) {
            builder.setAdjustmentTimestamp(adjustmentTimestamp);
        }

        TestHelper.ensureAsExpected(15,
            () -> AdminService.manuallyAdjustAvailableToSpend(authenticationToken, builder.build(), managedCardId),
            SC_NO_CONTENT);
    }

    public static Response getManagedCardStatement(final String managedCardId,
                                                   final String secretKey,
                                                   final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.getManagedCardStatement(secretKey, managedCardId, authenticationToken,
                        Optional.empty(), AcceptedResponse.JSON),
                SC_OK);
    }

    public static void getManagedCardStatementForbidden(final String managedCardId,
                                                   final String secretKey,
                                                   final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.getManagedCardStatement(secretKey, managedCardId, authenticationToken,
                        Optional.empty(), AcceptedResponse.JSON),
                SC_FORBIDDEN);
    }

    public static Response getManagedCardStatement(final String managedCardId,
                                                   final String secretKey,
                                                   final String authenticationToken,
                                                   final int expectedStatementCount) {
        return TestHelper.ensureAsExpected(300,
                () -> ManagedCardsService.getManagedCardStatement(secretKey, managedCardId, authenticationToken,
                        Optional.empty(), AcceptedResponse.JSON),
                x-> x.statusCode() == SC_OK && x.jsonPath().get("count").equals(expectedStatementCount),
                Optional.of(String.format("Expecting 200 with a statement count of %s, check logged payload", expectedStatementCount)));
    }

    public static Response getManagedCard(final String secretKey,
                                          final String managedCardId,
                                          final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken), SC_OK);
    }

    public static String simulatePurchase(final String secretKey,
                                          final String managedCardId,
                                          final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                purchaseAmount);
    }

    public static BalanceModel getManagedCardBalance(final String managedCardId,
                                                     final String secretKey,
                                                     final String authenticationToken,
                                                     final int expectedManagedCardBalance) {

        final Response response = TestHelper.ensureAsExpected(30,
                () -> ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken),
                x-> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance").equals(expectedManagedCardBalance),
                Optional.of(String.format("Expecting 200 with a balance of %s for card id %s, check logged payload", expectedManagedCardBalance, managedCardId)));

        return new BalanceModel(response.jsonPath().get("balances.availableBalance"),
                response.jsonPath().get("balances.actualBalance"));
    }

    public static BalanceModel getManagedCardBalance(final String managedCardId,
                                                     final String secretKey,
                                                     final String authenticationToken) {

        final JsonPath response = ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken).jsonPath();

        return new BalanceModel(response.get("balances.availableBalance"),
                response.get("balances.actualBalance"));
    }

    public static BalanceModel getManagedCardBalance(final String managedCardId,
                                                     final String secretKey,
                                                     final String authenticationToken,
                                                     final int expectedManagedCardAvailableBalance,
                                                     final int expectedManagedCardActualBalance) {

        return getManagedCardBalance(managedCardId, secretKey, authenticationToken, expectedManagedCardAvailableBalance, expectedManagedCardActualBalance, 30);
    }

    public static BalanceModel getManagedCardBalance(final String managedCardId,
                                                     final String secretKey,
                                                     final String authenticationToken,
                                                     final int expectedManagedCardAvailableBalance,
                                                     final int expectedManagedCardActualBalance,
                                                     final int timeout) {

        final Response response = TestHelper.ensureAsExpected(30,
                () -> ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken),
                x-> x.statusCode() == SC_OK &&
                        x.jsonPath().get("balances.availableBalance").equals(expectedManagedCardAvailableBalance) &&
                        x.jsonPath().get("balances.actualBalance").equals(expectedManagedCardActualBalance),
                Optional.of(String.format("Expecting 200 with available balance %s and actual balance %s, check logged payload",
                        expectedManagedCardAvailableBalance, expectedManagedCardActualBalance)));

        return new BalanceModel(response.jsonPath().get("balances.availableBalance"),
                response.jsonPath().get("balances.actualBalance"));
    }

    public static String getPhysicalCardPin(final String secretKey,
                                            final String managedCardId,
                                            final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.getPhysicalCardPin(secretKey, managedCardId, authenticationToken), SC_OK)
                .jsonPath().getString("pin.value");
    }

    public static List<ManagedCardDetails> createPrepaidManagedCards(final String profile,
                                                                     final String currency,
                                                                     final String secretKey,
                                                                     final String authenticationToken,
                                                                     final int noOfCards){

        final List<ManagedCardDetails> cards = new ArrayList<>();

        IntStream.range(0, noOfCards).forEach(x -> {
            final CreateManagedCardModel createManagedCardModel =
                    CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(profile, currency).build();

            final String cardId = createPrepaidManagedCard(createManagedCardModel, secretKey, authenticationToken);

            cards.add(ManagedCardDetails.builder()
                    .setManagedCardId(cardId)
                    .setManagedCardModel(createManagedCardModel)
                    .setManagedCardMode(PREPAID_MODE)
                    .setInstrumentType(VIRTUAL)
                    .build());
        });

        return cards;
    }

    public static List<ManagedCardDetails> createDebitManagedCards(final String profile,
                                                                   final String managedAccountId,
                                                                   final String secretKey,
                                                                   final String authenticationToken,
                                                                   final int noOfCards){

        final List<ManagedCardDetails> cards = new ArrayList<>();

        IntStream.range(0, noOfCards).forEach(x -> {
            final CreateManagedCardModel createManagedCardModel =
                    CreateManagedCardModel.DefaultCreateDebitManagedCardModel(profile, managedAccountId).build();

            final String cardId = createDebitManagedCard(createManagedCardModel, secretKey, authenticationToken);

            cards.add(ManagedCardDetails.builder()
                    .setManagedCardId(cardId)
                    .setManagedCardModel(createManagedCardModel)
                    .setManagedCardMode(DEBIT_MODE)
                    .setInstrumentType(VIRTUAL)
                    .build());
        });

        return cards;
    }

    public static List<ManagedCardDetails> createPhysicalPrepaidManagedCards(final String profile,
                                                                             final String currency,
                                                                             final String secretKey,
                                                                             final String authenticationToken,
                                                                             final int noOfCards){

        final List<ManagedCardDetails> cards = new ArrayList<>();

        IntStream.range(0, noOfCards).forEach(x -> {
            final CreateManagedCardModel createManagedCardModel =
                    CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(profile, currency).build();

            final String cardId = createPrepaidManagedCard(createManagedCardModel, secretKey, authenticationToken);
            upgradeAndActivateManagedCardToPhysical(secretKey, cardId, authenticationToken);

            cards.add(ManagedCardDetails.builder()
                    .setManagedCardId(cardId)
                    .setManagedCardModel(createManagedCardModel)
                    .setManagedCardMode(PREPAID_MODE)
                    .setInstrumentType(PHYSICAL)
                    .build());
        });

        return cards;
    }

    public static List<ManagedCardDetails> createPhysicalPrepaidManagedCards(final List<CreateManagedCardModel> createManagedCardModels,
                                                                             final String secretKey,
                                                                             final String authenticationToken){

        final List<ManagedCardDetails> cards = new ArrayList<>();

        createManagedCardModels.forEach(createManagedCardModel -> {

            final String cardId = createPrepaidManagedCard(createManagedCardModel, secretKey, authenticationToken);
            upgradeAndActivateManagedCardToPhysical(secretKey, cardId, authenticationToken);

            cards.add(ManagedCardDetails.builder()
                    .setManagedCardId(cardId)
                    .setManagedCardModel(createManagedCardModel)
                    .setManagedCardMode(PREPAID_MODE)
                    .setInstrumentType(PHYSICAL)
                    .build());
        });

        return cards;
    }

    public static List<ManagedCardDetails> createPhysicalDebitManagedCards(final String profile,
                                                                           final String managedAccountId,
                                                                           final String secretKey,
                                                                           final String authenticationToken,
                                                                           final int noOfCards){

        final List<ManagedCardDetails> cards = new ArrayList<>();

        IntStream.range(0, noOfCards).forEach(x -> {
            final CreateManagedCardModel createManagedCardModel =
                    CreateManagedCardModel.DefaultCreateDebitManagedCardModel(profile, managedAccountId).build();

            final String cardId = createDebitManagedCard(createManagedCardModel, secretKey, authenticationToken);
            upgradeAndActivateManagedCardToPhysical(secretKey, cardId, authenticationToken);

            cards.add(ManagedCardDetails.builder()
                    .setManagedCardId(cardId)
                    .setManagedCardModel(createManagedCardModel)
                    .setManagedCardMode(DEBIT_MODE)
                    .setInstrumentType(PHYSICAL)
                    .build());
        });

        return cards;
    }

    public static List<ManagedCardDetails> createPhysicalInactivePrepaidManagedCards(final String profile,
                                                                                     final String currency,
                                                                                     final String secretKey,
                                                                                     final String authenticationToken,
                                                                                     final int noOfCards){

        final List<ManagedCardDetails> cards = new ArrayList<>();

        IntStream.range(0, noOfCards).forEach(x -> {
            final CreateManagedCardModel createManagedCardModel =
                    CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(profile, currency).build();

            final String cardId = createPrepaidManagedCard(createManagedCardModel, secretKey, authenticationToken);
            upgradeManagedCardToPhysical(secretKey, cardId, authenticationToken);

            cards.add(ManagedCardDetails.builder()
                    .setManagedCardId(cardId)
                    .setManagedCardModel(createManagedCardModel)
                    .setManagedCardMode(PREPAID_MODE)
                    .setInstrumentType(VIRTUAL)
                    .build());
        });

        return cards;
    }

    public static List<ManagedCardDetails> createVirtualAndPhysicalCards(final String prepaidManagedCardProfileId,
                                                                         final String debitManagedCardProfileId,
                                                                         final String currency,
                                                                         final String parentManagedAccountId,
                                                                         final String authenticationToken,
                                                                         final String secretKey){

        final List<ManagedCardDetails> managedCards = new ArrayList<>();
        managedCards.add(createPrepaidManagedCards(prepaidManagedCardProfileId, currency, secretKey, authenticationToken, 1).get(0));
        managedCards.add(createPhysicalPrepaidManagedCards(prepaidManagedCardProfileId, currency, secretKey, authenticationToken, 1).get(0));
        managedCards.add(createDebitManagedCards(debitManagedCardProfileId, parentManagedAccountId, secretKey, authenticationToken, 1).get(0));
        managedCards.add(createPhysicalDebitManagedCards(debitManagedCardProfileId, parentManagedAccountId, secretKey, authenticationToken, 1).get(0));
        managedCards.add(createPhysicalInactivePrepaidManagedCards(prepaidManagedCardProfileId, currency, secretKey, authenticationToken, 1).get(0));

        return managedCards;
    }

    public static String getProcessorCardProduct(IdentityType identityType, Currency currency) {
        Map<Pair<IdentityType, Currency>, String> processorCardProducts = new HashMap<>();
        // Current GPS card product combinations
        processorCardProducts.put(Pair.of(IdentityType.CORPORATE, Currency.USD), "5164");
        processorCardProducts.put(Pair.of(IdentityType.CORPORATE, Currency.GBP), "5163");
        processorCardProducts.put(Pair.of(IdentityType.CORPORATE, Currency.EUR), "5162");
        processorCardProducts.put(Pair.of(IdentityType.CONSUMER, Currency.USD), "5161");
        processorCardProducts.put(Pair.of(IdentityType.CONSUMER, Currency.GBP), "5160");
        processorCardProducts.put(Pair.of(IdentityType.CONSUMER, Currency.EUR), "5159");
        if (!processorCardProducts.containsKey(Pair.of(identityType, currency)))
            throw new IllegalArgumentException(String.format("Unsupported currency type %s and identity type %s", currency, identityType));

        return processorCardProducts.get(Pair.of(identityType, currency));
    }

    public static String getCardNumberLastFour(final String secretKey,
                                               final String managedCardId,
                                               final String authenticationToken) {
        return getManagedCard(secretKey, managedCardId, authenticationToken).jsonPath().getString("cardNumberLastFour");
    }

    public static String createDebitManagedCardWithDefaultSpendRules(final String managedCardProfileId,
                                                                     final String managedAccountId,
                                                                     final String currency,
                                                                     final String secretKey,
                                                                     final String authenticationToken) {

        final String managedCardId =
                createDebitManagedCard(managedCardProfileId, managedAccountId, secretKey, authenticationToken);

        setDefaultDebitSpendLimit(new CurrencyAmount(currency, 1000L), secretKey,
                managedCardId, authenticationToken);

        return managedCardId;
    }

    public static String createPrepaidManagedCardWithDefaultSpendRules(final String managedCardProfileId,
                                                                       final String currency,
                                                                       final String secretKey,
                                                                       final String authenticationToken) {

        final String managedCardId =
                createPrepaidManagedCard(managedCardProfileId, currency, secretKey, authenticationToken);

        setDefaultPrepaidSpendLimit(secretKey, managedCardId, authenticationToken);

        return managedCardId;
    }

    public static String assignManagedCard(final String reference,
                                           final String secretKey,
                                           final String identityToken) {
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(reference)
                        .build();

        return TestHelper.ensureAsExpected(15,
                () -> ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, identityToken),
                SC_OK).jsonPath().getString("id");
    }

    public static Response replenishCardPool(final CreateUnassignedCardBatchModel createUnassignedCardBatchModel,
                                                                      final String innovatorToken){

        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.replenishCardPool(createUnassignedCardBatchModel, innovatorToken),
                        SC_OK);
    }

    public static List<UnassignedCardResponseModel> replenishPrepaidCardPool(final String managedCardsProfileId,
                                                                             final String currency,
                                                                             final CardLevelClassification cardLevelClassification,
                                                                             final InstrumentType instrumentType,
                                                                             final String innovatorToken){

        final CreateUnassignedCardBatchModel createUnassignedCardBatchModel =
                CreateUnassignedCardBatchModel
                        .DefaultCreatePrepaidUnassignedCardBatchModel(managedCardsProfileId, currency, cardLevelClassification, instrumentType, TestHelper.VERIFICATION_CODE, Optional.empty())
                        .build();

        return replenishCardPool(createUnassignedCardBatchModel, innovatorToken)
                .jsonPath().getList("unassignedManagedCards.prepaidUnassignedCard.managedCard",
                        UnassignedCardResponseModel.class);
    }

    public static List<UnassignedManagedCardDetails> createUnassignedPrepaidCards(final String managedCardsProfileId,
                                                                                  final String currency,
                                                                                  final CardLevelClassification cardLevelClassification,
                                                                                  final InstrumentType instrumentType,
                                                                                  final String innovatorToken,
                                                                                  final Optional<Integer> batchSize){

        final CreateUnassignedCardBatchModel createUnassignedCardBatchModel =
                CreateUnassignedCardBatchModel
                        .DefaultCreatePrepaidUnassignedCardBatchModel(managedCardsProfileId, currency, cardLevelClassification, instrumentType, TestHelper.VERIFICATION_CODE, batchSize)
                        .build();

        final List<UnassignedCardResponseModel> unassignedCards =
                replenishCardPool(createUnassignedCardBatchModel, innovatorToken)
                        .jsonPath().getList("unassignedManagedCards.prepaidUnassignedCard.managedCard",
                                UnassignedCardResponseModel.class);

        final List<UnassignedManagedCardDetails> unassignedManagedCards = new ArrayList<>();

        unassignedCards.forEach(card -> unassignedManagedCards.add(UnassignedManagedCardDetails.builder()
                .setCreateUnassignedCardBatchModel(createUnassignedCardBatchModel)
                .setManagedCardMode(PREPAID_MODE)
                .setUnassignedCardResponseModel(card)
                .setInstrumentType(instrumentType)
                .build()));

        return unassignedManagedCards;
    }

    public static List<UnassignedManagedCardDetails> createUnassignedDebitCards(final String managedCardsProfileId,
                                                                                final String managedAccountId,
                                                                                final CardLevelClassification cardLevelClassification,
                                                                                final InstrumentType instrumentType,
                                                                                final String innovatorToken,
                                                                                final Optional<Integer> batchSize){

        final CreateUnassignedCardBatchModel createUnassignedCardBatchModel =
                CreateUnassignedCardBatchModel
                        .DefaultCreateDebitUnassignedCardBatchModel(managedCardsProfileId, managedAccountId, cardLevelClassification, instrumentType, TestHelper.VERIFICATION_CODE, batchSize)
                        .build();

        final List<UnassignedCardResponseModel> unassignedCards =
                replenishCardPool(createUnassignedCardBatchModel, innovatorToken)
                        .jsonPath().getList("unassignedManagedCards.debitUnassignedCard.managedCard",
                                UnassignedCardResponseModel.class);

        final List<UnassignedManagedCardDetails> unassignedManagedCards = new ArrayList<>();

        unassignedCards.forEach(card -> {
            card.setParentManagedAccountId(managedAccountId);
            unassignedManagedCards.add(UnassignedManagedCardDetails.builder()
                    .setCreateUnassignedCardBatchModel(createUnassignedCardBatchModel)
                    .setManagedCardMode(DEBIT_MODE)
                    .setUnassignedCardResponseModel(card)
                    .setInstrumentType(instrumentType)
                    .build());
        });

        return unassignedManagedCards;
    }

    public static List<UnassignedManagedCardDetails> replenishCardPool(final String managedAccountId,
                                                                       final String prepaidManagedCardsProfileId,
                                                                       final String debitManagedCardsProfileId,
                                                                       final String currency,
                                                                       final CardLevelClassification cardLevelClassification,
                                                                       final String innovatorToken){
        final List<UnassignedManagedCardDetails> unassignedCards = new ArrayList<>();
        unassignedCards.addAll(createUnassignedPrepaidCards(prepaidManagedCardsProfileId, currency, cardLevelClassification, VIRTUAL, innovatorToken, Optional.empty()));
        unassignedCards.addAll(createUnassignedPrepaidCards(prepaidManagedCardsProfileId, currency, cardLevelClassification, PHYSICAL, innovatorToken, Optional.empty()));
        unassignedCards.addAll(createUnassignedDebitCards(debitManagedCardsProfileId, managedAccountId, cardLevelClassification, VIRTUAL, innovatorToken, Optional.empty()));
        unassignedCards.addAll(createUnassignedDebitCards(debitManagedCardsProfileId, managedAccountId, cardLevelClassification, PHYSICAL, innovatorToken, Optional.empty()));

        return unassignedCards;
    }

    public static List<String> cardBatchWithSpendRules(final String managedAccountId,
                                                       final String prepaidManagedCardsProfileId,
                                                       final String debitManagedCardsProfileId,
                                                       final String currency,
                                                       final CardLevelClassification cardLevelClassification,
                                                       final String innovatorToken,
                                                       final int batchSize,
                                                       final String secretKey,
                                                       final String identityToken){
        final List<UnassignedManagedCardDetails> unassignedCards = new ArrayList<>();
        final List<String> managedCards = new ArrayList<>();
        unassignedCards.addAll(createUnassignedPrepaidCards(prepaidManagedCardsProfileId, currency, cardLevelClassification, VIRTUAL, innovatorToken, Optional.of(batchSize)));
        unassignedCards.addAll(createUnassignedDebitCards(debitManagedCardsProfileId, managedAccountId, cardLevelClassification, VIRTUAL, innovatorToken, Optional.of(batchSize)));

        unassignedCards.stream().parallel().forEach(card -> {
            final String managedCardId =
                    assignManagedCard(card.getUnassignedCardResponseModel().getExternalHandle(), secretKey, identityToken);

            if (card.getManagedCardMode().equals(DEBIT_MODE)) {
                setDefaultDebitSpendLimit(new CurrencyAmount(currency, 1000L), secretKey, managedCardId, identityToken);
            } else {
                setDefaultPrepaidSpendLimit(secretKey, managedCardId, identityToken);
            }

            managedCards.add(managedCardId);
        });

        return managedCards;
    }

    public static List<String> cardBatchDebitWithoutSpendRules(final String managedAccountId,
                                                       final String debitManagedCardsProfileId,
                                                       final CardLevelClassification cardLevelClassification,
                                                       final String innovatorToken,
                                                       final int batchSize,
                                                       final String secretKey,
                                                       final String identityToken){
        final List<UnassignedManagedCardDetails> unassignedCards = new ArrayList<>();
        final List<String> managedCards = new ArrayList<>();
        unassignedCards.addAll(createUnassignedDebitCards(debitManagedCardsProfileId, managedAccountId, cardLevelClassification, VIRTUAL, innovatorToken, Optional.of(batchSize)));

        unassignedCards.stream().parallel().forEach(card -> {
            final String managedCardId =
                    assignManagedCard(card.getUnassignedCardResponseModel().getExternalHandle(), secretKey, identityToken);

            managedCards.add(managedCardId);
        });

        return managedCards;
    }

    public static List<String> cardBatchPrepaidWithoutSpendRules(final String prepaidManagedCardsProfileId,
                                                               final String currency,
                                                               final CardLevelClassification cardLevelClassification,
                                                               final String innovatorToken,
                                                               final int batchSize,
                                                               final String secretKey,
                                                               final String identityToken){
        final List<UnassignedManagedCardDetails> unassignedCards = new ArrayList<>();
        final List<String> managedCards = new ArrayList<>();
        unassignedCards.addAll(createUnassignedPrepaidCards(prepaidManagedCardsProfileId, currency, cardLevelClassification, VIRTUAL, innovatorToken, Optional.of(batchSize)));

        unassignedCards.stream().parallel().forEach(card -> {
            final String managedCardId =
                    assignManagedCard(card.getUnassignedCardResponseModel().getExternalHandle(), secretKey, identityToken);

            managedCards.add(managedCardId);
        });

        return managedCards;
    }

    public static void deleteSpendRules(final String secretKey,
                                        final String managedCardId,
                                        final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                        () -> ManagedCardsService.deleteManagedCardsSpendRules(secretKey, managedCardId, authenticationToken), SC_NO_CONTENT);
    }
}