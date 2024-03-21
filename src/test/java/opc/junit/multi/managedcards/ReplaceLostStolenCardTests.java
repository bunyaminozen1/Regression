package opc.junit.multi.managedcards;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardBrand;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.DestroyedReason;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedCardMode;
import opc.enums.opc.ManufacturingState;
import commons.enums.State;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendLimitResponseModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class ReplaceLostStolenCardTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static SpendRulesResponseModel corporateDebitProfileRules;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();

        corporateDebitProfileRules =
                InnovatorHelper.getProfileSpendRules(innovatorToken, programmeId, corporateDebitManagedCardsProfileId)
                        .as(SpendRulesResponseModel.class);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_PrepaidCorporate_Success(final DestroyedReason destroyedReason){
        
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporatePrepaidManagedCardsProfileId,
                        managedCard, 0, destroyedReason, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId, corporatePrepaidManagedCardsProfileId, managedCard, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_PrepaidConsumer_Success(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), consumerAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulConsumerCardDestroyedAndReturnReplacement(response, consumerPrepaidManagedCardsProfileId,
                        managedCard, destroyedReason, ManufacturingState.DELIVERED, false, false);

        assertSuccessfulConsumerCardReplacement(replacementCardId, consumerPrepaidManagedCardsProfileId, managedCard, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_DebitCorporate_Success(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporateDebitManagedCardsProfileId,
                        managedCard, 0, destroyedReason, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId,corporateDebitManagedCardsProfileId,  managedCard, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_DebitConsumer_Success(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), consumerAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulConsumerCardDestroyedAndReturnReplacement(response, consumerDebitManagedCardsProfileId,
                        managedCard, destroyedReason, ManufacturingState.DELIVERED, false, false);

        assertSuccessfulConsumerCardReplacement(replacementCardId, consumerDebitManagedCardsProfileId, managedCard, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_CorporateUser_Success(final DestroyedReason destroyedReason){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        reportLostOrStolen(managedCard.getManagedCardId(), user.getRight(), destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getRight())
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporatePrepaidManagedCardsProfileId,
                        managedCard, 0, destroyedReason, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId, corporatePrepaidManagedCardsProfileId, managedCard, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void ReplaceLostStolenCard_LostCardDebitSpendRulesChecks_Success(final LimitInterval interval){

        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();
        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                response
                        .extract()
                        .jsonPath()
                        .get("physicalCardDetails.replacement.replacementId");

        ManagedCardsService.getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("availableToSpend.value.amount[0]", equalTo(100))
                .body("availableToSpend.interval[0]", equalTo(interval.name()));

        ManagedCardsService.getManagedCard(secretKey, replacementCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("availableToSpend.value.amount[0]", equalTo(100))
                .body("availableToSpend.interval[0]", equalTo(interval.name()));

        final ValidatableResponse originalCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final ValidatableResponse replacementCardSpendRules =
                ManagedCardsService
                    .getManagedCardsSpendRules(secretKey, replacementCardId, corporateAuthenticationToken)
                    .then()
                    .statusCode(SC_OK);

        assertEmptySpendRules(originalCardSpendRules, corporateCurrency);
        assertSpendRules(replacementCardSpendRules, spendRulesModel, corporateCurrency, corporateDebitProfileRules, spendRulesModel.getSpendLimit());
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void ReplaceLostStolenCard_StolenCardDebitSpendRulesChecks_Success(final LimitInterval interval){

        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();
        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, DestroyedReason.STOLEN);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                response
                        .extract()
                        .jsonPath()
                        .get("physicalCardDetails.replacement.replacementId");

        ManagedCardsService.getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("availableToSpend.value.amount[0]", equalTo(100))
                .body("availableToSpend.interval[0]", equalTo(interval.name()));

        ManagedCardsService.getManagedCard(secretKey, replacementCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("availableToSpend.value.amount[0]", equalTo(100))
                .body("availableToSpend.interval[0]", equalTo(interval.name()));

        final ValidatableResponse originalCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final ValidatableResponse replacementCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, replacementCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(originalCardSpendRules, corporateCurrency);
        assertSpendRules(replacementCardSpendRules, spendRulesModel, corporateCurrency, corporateDebitProfileRules, spendRulesModel.getSpendLimit());
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_MultipleSpendLimitsChecks_Success(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 5000L), LimitInterval.YEARLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 2500L), LimitInterval.QUARTERLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1250L), LimitInterval.MONTHLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 750L), LimitInterval.WEEKLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), LimitInterval.DAILY)))
                .build();
        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                response
                        .extract()
                        .jsonPath()
                        .get("physicalCardDetails.replacement.replacementId");

        final JsonPath originalCardAvailableToSpend =
                ManagedCardsService.getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        final List<SpendLimitResponseModel> originalCardAvailableToSpendLimits =
                Arrays.asList(new ObjectMapper().convertValue(originalCardAvailableToSpend.get("availableToSpend"), SpendLimitResponseModel[].class
                ));

        final JsonPath replacementCardAvailableToSpend =
                ManagedCardsService.getManagedCard(secretKey, replacementCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        final List<SpendLimitResponseModel> replacementCardAvailableToSpendLimits =
                Arrays.asList(new ObjectMapper().convertValue(replacementCardAvailableToSpend.get("availableToSpend"), SpendLimitResponseModel[].class
                ));

        spendRulesModel.getSpendLimit().forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    originalCardAvailableToSpendLimits.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);
            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());

            final SpendLimitResponseModel actualReplacementLimit =
                    replacementCardAvailableToSpendLimits.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);
            assertEquals(expectedLimit.getValue().getAmount().toString(), actualReplacementLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualReplacementLimit.getInterval());
        });

        final ValidatableResponse originalCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final ValidatableResponse replacementCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, replacementCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final List<SpendLimitResponseModel> replacementCardSpendLimits =
                Arrays.asList(new ObjectMapper().convertValue(replacementCardSpendRules.extract().jsonPath().get("spendLimit"), SpendLimitResponseModel[].class
        ));

        spendRulesModel.getSpendLimit().forEach(expectedLimit -> {
            final SpendLimitResponseModel actualLimit =
                    replacementCardSpendLimits.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);
            assertEquals(expectedLimit.getValue().getAmount().toString(), actualLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualLimit.getInterval());
        });

        assertEmptySpendRules(originalCardSpendRules, corporateCurrency);
        assertSpendRules(replacementCardSpendRules, spendRulesModel, corporateCurrency, corporateDebitProfileRules, spendRulesModel.getSpendLimit());
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_CardPurchaseSpendLimitsChecks_Success(final DestroyedReason destroyedReason){

        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 5000L), LimitInterval.YEARLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 2500L), LimitInterval.QUARTERLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1250L), LimitInterval.MONTHLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 750L), LimitInterval.WEEKLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), LimitInterval.DAILY)))
                .build();
        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

         final String authorisationId = simulateAuth(managedCard.getManagedCardId(),
                 new CurrencyAmount(corporateCurrency, purchaseAmount));

        simulateSettlement(managedCard.getManagedCardId(), authorisationId,
                new CurrencyAmount(corporateCurrency, purchaseAmount));

        final List<SpendLimitModel> updatedSpendLimits = new ArrayList<>();
        spendRulesModel.getSpendLimit().forEach(limit -> {
            final Long amount = limit.getValue().getAmount() - purchaseAmount - purchaseFee;
            updatedSpendLimits
                    .add(new SpendLimitModel(new CurrencyAmount(corporateCurrency, amount),
                            LimitInterval.valueOf(limit.getInterval())));
        });

        final List<SpendLimitResponseModel> originalCardAvailableToSpendLimits =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, updatedSpendLimits);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                response
                        .extract()
                        .jsonPath()
                        .get("physicalCardDetails.replacement.replacementId");

        final List<SpendLimitResponseModel> replacementCardAvailableToSpendLimits =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, replacementCardId, corporateAuthenticationToken, updatedSpendLimits);

        updatedSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    originalCardAvailableToSpendLimits.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);
            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());

            final SpendLimitResponseModel actualReplacementLimit =
                    replacementCardAvailableToSpendLimits.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);
            assertEquals(expectedLimit.getValue().getAmount().toString(), actualReplacementLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualReplacementLimit.getInterval());
        });

        final ValidatableResponse originalCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final ValidatableResponse replacementCardSpendRules =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, replacementCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("allowedMerchantCategories", nullValue())
                        .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                        .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                        .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                        .body("allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                        .body("allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                        .body("allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                        .body("allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                        .body("allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()));

        final List<SpendLimitResponseModel> replacementCardSpendLimits =
                Arrays.asList(new ObjectMapper().convertValue(replacementCardSpendRules.extract().jsonPath().get("spendLimit"), SpendLimitResponseModel[].class
                ));

        updatedSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualLimit =
                    replacementCardSpendLimits.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);
            assertEquals(expectedLimit.getValue().getAmount().toString(), actualLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualLimit.getInterval());
        });

        assertEmptySpendRules(originalCardSpendRules, corporateCurrency);
        assertSpendRules(replacementCardSpendRules, spendRulesModel, corporateCurrency, corporateDebitProfileRules, updatedSpendLimits);
    }

    @Test
    public void ReplaceLostStolenCard_CardWithFunds_Success() {

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final long cardBalance = 50L;
        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, cardBalance, 1);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporatePrepaidManagedCardsProfileId,
                        managedCard, (int) cardBalance,
                        DestroyedReason.LOST, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId, corporatePrepaidManagedCardsProfileId, managedCard, false, true);

        final Map<Integer, Map<String, String>> adjustment =
                TestHelper.ensureDatabaseDataRetrieved(200,
                        () -> ManagedCardsDatabaseHelper.getLostStolenReplacementAdjustment(managedCard.getManagedCardId()),
                        x -> x.size() == 1);

        //TODO - Find a better way to check adjustments
        Assertions.assertEquals(
                Long.valueOf(Math.negateExact(cardBalance)),
                Long.valueOf(adjustment.get(0).get("adjustment_amount")), "Incorrect adjustment");
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_ReplacedDamagedReplaceLostStolen_Success(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        ManagedCardsHelper.activateManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporatePrepaidManagedCardsProfileId,
                        managedCard, 0, destroyedReason, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId, corporatePrepaidManagedCardsProfileId, managedCard, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_ReplaceLostStolenCardReplacedDamagedReplaceLostStolen_Success(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporatePrepaidManagedCardsProfileId,
                        managedCard, 0, destroyedReason, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId, corporatePrepaidManagedCardsProfileId, managedCard, false, true);

        ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                secretKey, replacementCardId, corporateAuthenticationToken);

        ManagedCardsHelper.activateManagedCard(secretKey, replacementCardId, corporateAuthenticationToken);

        reportLostOrStolen(replacementCardId, corporateAuthenticationToken, destroyedReason);

        final ValidatableResponse newCardResponse =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, replacementCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final ManagedCardDetails replacedCard =
                ManagedCardDetails.builder()
                        .setManagedCardId(replacementCardId)
                        .setManagedCardModel(managedCard.getManagedCardModel())
                        .setInstrumentType(managedCard.getInstrumentType())
                        .setPhysicalCardAddressModel(managedCard.getPhysicalCardAddressModel())
                        .setManagedCardMode(managedCard.getManagedCardMode())
                        .build();

        final String secondReplacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(newCardResponse, corporatePrepaidManagedCardsProfileId,
                        replacedCard, 0, destroyedReason, true, false);

        assertSuccessfulCorporateCardReplacement(secondReplacementCardId, corporatePrepaidManagedCardsProfileId, replacedCard, true, true);
        assertNotEquals(replacementCardId, secondReplacementCardId);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = { "LOST", "STOLEN" })
    public void ReplaceLostStolenCard_ParentManagedAccountBlocked_ParentManagedAccountNotActive(final DestroyedReason destroyedReason){

        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();
        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, destroyedReason);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedAccountsHelper.blockManagedAccount(managedCard.getManagedCardModel().getParentManagedAccountId(),
                secretKey, corporateAuthenticationToken);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    public void ReplaceLostStolenCard_ActivePhysicalInstrument_InstrumentNotMarkedLostOrStolen(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_MARKED_LOST_OR_STOLEN"));
    }

    @Test
    public void ReplaceLostStolenCard_CardNotUpgraded_InstrumentNotPhysical(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void ReplaceLostStolenCard_CardUpgradedNotActivated_Success(){

        ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel =
                PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, physicalCardAddressModel);
        managedCard = ManagedCardDetails.setPhysicalAddress(managedCard, physicalCardAddressModel);

        reportLostOrStolen(managedCard.getManagedCardId(), consumerAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulConsumerCardDestroyedAndReturnReplacement(response, consumerPrepaidManagedCardsProfileId,
                        managedCard, DestroyedReason.LOST, ManufacturingState.REQUESTED, false, false);

        assertSuccessfulConsumerCardReplacement(replacementCardId, consumerPrepaidManagedCardsProfileId, managedCard, false, true);
    }

    @Test
    public void ReplaceLostStolenCard_CardAlreadyReplaced_InstrumentAlreadyReplaced(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), consumerAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REPLACED"));
    }

    @Test
    public void ReplaceLostStolenCard_CardBlocked_InstrumentNotMarkedLostOrStolen(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_MARKED_LOST_OR_STOLEN"));
    }

    @Test
    public void ReplaceLostStolenCard_CardDestroyed_InstrumentNotMarkedLostOrStolen(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_MARKED_LOST_OR_STOLEN"));
    }

    @Test
    public void ReplaceLostStolenCard_DamagedCard_InstrumentNotMarkedLostOrStolen(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_MARKED_LOST_OR_STOLEN"));
    }

    @Test
    public void ReplaceLostStolenCard_CorporateReplaceUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);


        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        reportLostOrStolen(managedCard.getManagedCardId(), user.getRight(), DestroyedReason.STOLEN);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final ValidatableResponse response =
                ManagedCardsService
                        .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getRight())
                        .then()
                        .statusCode(SC_OK);

        final String replacementCardId =
                assertSuccessfulCorporateCardDestroyedAndReturnReplacement(response, corporatePrepaidManagedCardsProfileId,
                        managedCard, 0, DestroyedReason.STOLEN, false, false);

        assertSuccessfulCorporateCardReplacement(replacementCardId, corporatePrepaidManagedCardsProfileId, managedCard, false, true);
    }

    @Test
    public void ReplaceLostStolenCard_UserReplaceCorporateCard_Unauthorized(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, DestroyedReason.STOLEN);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReplaceLostStolenCard_InvalidCode_BadRequest(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), consumerAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel =
                new ReplacePhysicalCardModel(RandomStringUtils.randomNumeric(10));

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ReplaceLostStolenCard_UnknownManagedCardId_NotFound() {
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReplaceLostStolenCard_NoManagedCardId_NotFound() {
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReplaceLostStolenCard_CrossIdentityCheck_NotFound(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReplaceLostStolenCard_InvalidApiKey_Unauthorised(){
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, "abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReplaceLostStolenCard_NoApiKey_BadRequest(){
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, "", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ReplaceLostStolenCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReplaceLostStolenCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReplaceLostStolenCard_BackofficeCorporateImpersonator_Forbidden(){

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), corporateAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReplaceLostStolenCard_BackofficeConsumerImpersonator_Forbidden(){

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        reportLostOrStolen(managedCard.getManagedCardId(), consumerAuthenticationToken, DestroyedReason.LOST);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void reportLostOrStolen(final String managedCardId, final String token, final DestroyedReason destroyedReason){

        final Response response = destroyedReason.equals(DestroyedReason.LOST) ?
                ManagedCardsService.reportLostCard(secretKey, managedCardId, token) :
                ManagedCardsService.reportStolenCard(secretKey, managedCardId, token);

        response
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String assertSuccessfulCorporateCardDestroyedAndReturnReplacement(final ValidatableResponse response,
                                                                              final String managedCardsProfileId,
                                                                              final ManagedCardDetails managedCard,
                                                                              final int cardBalance,
                                                                              final DestroyedReason destroyedReason,
                                                                              final boolean alreadyReplaced,
                                                                              final boolean isReplacementCard){
        response
                .body("profileId", equalTo(managedCardsProfileId))
                .body("currency", equalTo(corporateCurrency))
                .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                .body("friendlyName", equalTo(alreadyReplaced ? String.format("%s Replacement", managedCard.getManagedCardModel().getFriendlyName()) :
                        managedCard.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo(State.DESTROYED.name()))
                .body("state.destroyedReason", equalTo(destroyedReason.name()))
                .body("physicalCardDetails.replacement.replacementId", not(equalTo(managedCard.getManagedCardId())))
                .body("physicalCardDetails.replacement.replacementReason", equalTo("LOST_STOLEN"))
                .body("physicalCardDetails.pendingActivation", equalTo(false));

        assertCommonDetails(response, managedCard.getManagedCardId(), managedCard, cardBalance, ManufacturingState.DELIVERED, isReplacementCard);

        return response
                .extract()
                .jsonPath()
                .get("physicalCardDetails.replacement.replacementId");
    }

    private String assertSuccessfulConsumerCardDestroyedAndReturnReplacement(final ValidatableResponse response,
                                                                             final String managedCardsProfileId,
                                                                             final ManagedCardDetails managedCard,
                                                                             final DestroyedReason destroyedReason,
                                                                             final ManufacturingState manufacturingState,
                                                                             final boolean alreadyReplaced,
                                                                             final boolean isReplacementCard){
        response
                .body("profileId", equalTo(managedCardsProfileId))
                .body("currency", equalTo(consumerCurrency))
                .body("cardLevelClassification", equalTo(CardLevelClassification.CONSUMER.name()))
                .body("friendlyName", equalTo(alreadyReplaced ? String.format("%s Replacement", managedCard.getManagedCardModel().getFriendlyName()) :
                        managedCard.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo(State.DESTROYED.name()))
                .body("state.destroyedReason", equalTo(destroyedReason.name()))
                .body("physicalCardDetails.replacement.replacementId", not(equalTo(managedCard.getManagedCardId())))
                .body("physicalCardDetails.replacement.replacementReason", equalTo("LOST_STOLEN"))
                .body("physicalCardDetails.pendingActivation", equalTo(false));

        assertCommonDetails(response, managedCard.getManagedCardId(), managedCard, 0, manufacturingState, isReplacementCard);

        return response
                .extract()
                .jsonPath()
                .get("physicalCardDetails.replacement.replacementId");
    }

    private void assertSuccessfulCorporateCardReplacement(final String managedCardId,
                                                          final String managedCardsProfileId,
                                                          final ManagedCardDetails managedCard,
                                                          final boolean alreadyReplaced,
                                                          final boolean isReplacementCard){
        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(managedCardsProfileId))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                        .body("friendlyName", equalTo(alreadyReplaced ? String.format("%s Replacement Replacement", managedCard.getManagedCardModel().getFriendlyName()) :
                                String.format("%s Replacement", managedCard.getManagedCardModel().getFriendlyName())))
                        .body("state.state", equalTo(State.ACTIVE.name()))
                        .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                        .body("physicalCardDetails.pendingActivation", equalTo(true));

        if(managedCard.getManagedCardMode().equals(ManagedCardMode.DEBIT_MODE)){
            response
                    .body("availableToSpend.value.amount[0]", equalTo(0))
                    .body("availableToSpend.interval[0]", equalTo(LimitInterval.ALWAYS.name()));
        }

        assertCommonDetails(response, managedCardId, managedCard, 0, ManufacturingState.REQUESTED, isReplacementCard);
    }

    private void assertSuccessfulConsumerCardReplacement(final String managedCardId,
                                                         final String managedCardsProfileId,
                                                         final ManagedCardDetails managedCard,
                                                         final boolean alreadyReplaced,
                                                         final boolean isReplacementCard){
        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(managedCardsProfileId))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CONSUMER.name()))
                        .body("friendlyName", equalTo(alreadyReplaced ? String.format("%s Replacement Replacement", managedCard.getManagedCardModel().getFriendlyName()) :
                                String.format("%s Replacement", managedCard.getManagedCardModel().getFriendlyName())))
                        .body("state.state", equalTo(State.ACTIVE.name()))
                        .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                        .body("physicalCardDetails.pendingActivation", equalTo(true));

        if(managedCard.getManagedCardMode().equals(ManagedCardMode.DEBIT_MODE)){
            response
                    .body("availableToSpend.value.amount[0]", equalTo(0))
                    .body("availableToSpend.interval[0]", equalTo(LimitInterval.ALWAYS.name()));
        }

        assertCommonDetails(response, managedCardId, managedCard, 0, ManufacturingState.REQUESTED, isReplacementCard);
    }

    private void assertCommonDetails(final ValidatableResponse response,
                                     final String managedCardId,
                                     final ManagedCardDetails managedCard,
                                     final int cardBalance,
                                     final ManufacturingState manufacturingState,
                                     final boolean isReplacementCard){
        response
                .body("id", equalTo(managedCardId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(managedCard.getManagedCardModel().getTag()))
                .body("type", equalTo(managedCard.getInstrumentType().name()))
                .body("cardBrand", equalTo(CardBrand.MASTERCARD.name()))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", notNullValue())
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(managedCard.getManagedCardModel().getNameOnCard()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(isReplacementCard ? LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryPeriodMonths", equalTo(36))
                .body("renewalType", equalTo("NO_RENEW"))
                .body("creationTimestamp", notNullValue())
                .body("cardholderMobileNumber", equalTo(managedCard.getManagedCardModel().getCardholderMobileNumber()))
                .body("billingAddress.addressLine1", equalTo(managedCard.getManagedCardModel().getBillingAddress().getAddressLine1()))
                .body("billingAddress.addressLine2", equalTo(managedCard.getManagedCardModel().getBillingAddress().getAddressLine2()))
                .body("billingAddress.city", equalTo(managedCard.getManagedCardModel().getBillingAddress().getCity()))
                .body("billingAddress.postCode", equalTo(managedCard.getManagedCardModel().getBillingAddress().getPostCode()))
                .body("billingAddress.state", equalTo(managedCard.getManagedCardModel().getBillingAddress().getState()))
                .body("billingAddress.country", equalTo(managedCard.getManagedCardModel().getBillingAddress().getCountry()))
                .body("physicalCardDetails.productReference", notNullValue())
                .body("physicalCardDetails.carrierType", notNullValue())
                .body("physicalCardDetails.pinBlocked", equalTo(false))
                .body("physicalCardDetails.deliveryAddress.name", equalTo(managedCard.getPhysicalCardAddressModel().getName()))
                .body("physicalCardDetails.deliveryAddress.surname", equalTo(managedCard.getPhysicalCardAddressModel().getSurname()))
                .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(managedCard.getPhysicalCardAddressModel().getAddressLine1()))
                .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(managedCard.getPhysicalCardAddressModel().getAddressLine2()))
                .body("physicalCardDetails.deliveryAddress.city", equalTo(managedCard.getPhysicalCardAddressModel().getCity()))
                .body("physicalCardDetails.deliveryAddress.postCode", equalTo(managedCard.getPhysicalCardAddressModel().getPostCode()))
                .body("physicalCardDetails.deliveryAddress.state", equalTo(managedCard.getPhysicalCardAddressModel().getState()))
                .body("physicalCardDetails.deliveryAddress.country", equalTo(managedCard.getPhysicalCardAddressModel().getCountry()))
                .body("physicalCardDetails.deliveryMethod", equalTo("STANDARD_DELIVERY"))
                .body("physicalCardDetails.manufacturingState", equalTo(manufacturingState.name()))
                .body("mode", equalTo(managedCard.getManagedCardModel().getMode()));

        if (managedCard.getManagedCardMode() == ManagedCardMode.PREPAID_MODE) {
            response
                    .body("currency", equalTo(managedCard.getManagedCardModel().getCurrency()))
                    .body("balances.availableBalance", equalTo(cardBalance))
                    .body("balances.actualBalance", equalTo(cardBalance));
        } else {
            response
                    .body("parentManagedAccountId", equalTo(managedCard.getManagedCardModel().getParentManagedAccountId()));
        }
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private String simulateAuth(final String managedCardId,
                                final CurrencyAmount purchaseAmount){
        return SimulatorHelper.simulateAuthorisation(innovatorId, purchaseAmount, null, managedCardId);
    }

    private void simulateSettlement(final String managedCardId,
                                    final String relatedAuthorisationId,
                                    final CurrencyAmount purchaseAmount){
        SimulatorHelper.simulateSettlement(innovatorId, Long.parseLong(relatedAuthorisationId), purchaseAmount, managedCardId);
    }

    private void assertSpendRules(final ValidatableResponse response,
                                  final SpendRulesModel spendRulesModel,
                                  final String currency,
                                  final SpendRulesResponseModel spendRulesResponseModel,
                                  final List<SpendLimitModel> updatedSpendLimits) {
        response
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()
                        .size() == 0 ? null : spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()
                        .size() == 0 ? null : spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()
                        .size() == 0 ? null : spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()
                        .size() == 0 ? null : spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount", equalTo(spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount().intValue()))
                .body("cardLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()
                        .size() == 0 ? null : spendRulesModel.getAllowedMerchantCategories()))
                .body("cardLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()
                        .size() == 0 ? null : spendRulesModel.getBlockedMerchantCategories()))
                .body("cardLevelSpendRules.allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()
                        .size() == 0 ? null : spendRulesModel.getAllowedMerchantIds()))
                .body("cardLevelSpendRules.blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()
                        .size() == 0 ? null : spendRulesModel.getBlockedMerchantIds()))
                .body("cardLevelSpendRules.allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                .body("cardLevelSpendRules.allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                .body("cardLevelSpendRules.allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                .body("cardLevelSpendRules.allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                .body("cardLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()))
                .body("cardLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("cardLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("cardLevelSpendRules.maxTransactionAmount", equalTo(spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("cardLevelSpendRules.minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount().intValue()))
                .body("profileLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesResponseModel.getAllowedMerchantCategories()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantCategories()))
                .body("profileLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesResponseModel.getBlockedMerchantCategories()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantCategories()))
                .body("profileLevelSpendRules.allowedMerchantIds", equalTo(spendRulesResponseModel.getAllowedMerchantIds()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantIds()))
                .body("profileLevelSpendRules.blockedMerchantIds", equalTo(spendRulesResponseModel.getBlockedMerchantIds()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantIds()))
                .body("profileLevelSpendRules.allowContactless", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowContactless())))
                .body("profileLevelSpendRules.allowAtm", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowAtm())))
                .body("profileLevelSpendRules.allowECommerce", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowECommerce())))
                .body("profileLevelSpendRules.allowCashback", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowCashback())))
                .body("profileLevelSpendRules.allowCreditAuthorisations", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowCreditAuthorisations())))
                .body("profileLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesResponseModel.getAllowedMerchantCountries()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantCountries()))
                .body("profileLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesResponseModel.getBlockedMerchantCountries()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantCountries()))
                .body("profileLevelSpendRules.maxTransactionAmount", equalTo(spendRulesResponseModel.getMaxTransactionAmount() == null ?
                        null :  spendRulesResponseModel.getMaxTransactionAmount().intValue()))
                .body("profileLevelSpendRules.minTransactionAmount", equalTo(spendRulesResponseModel.getMinTransactionAmount() == null ?
                        null :  spendRulesResponseModel.getMinTransactionAmount().intValue()));

        if(spendRulesModel.getSpendLimit() != null){

            final List<SpendLimitResponseModel> actualSpendLimits =
                    Arrays.asList(new ObjectMapper().convertValue(response.extract().jsonPath().get("spendLimit"),
                            SpendLimitResponseModel[].class));

            final List<SpendLimitResponseModel> actualCardLevelSpendLimits =
                    Arrays.asList(new ObjectMapper().convertValue(response.extract().jsonPath().get("cardLevelSpendRules.spendLimit"),
                            SpendLimitResponseModel[].class));

            updatedSpendLimits.forEach(spendLimit -> {

                final SpendLimitResponseModel actualLimit =
                        actualSpendLimits.stream().filter(x -> x.getInterval().equals(spendLimit.getInterval())).findFirst().orElseThrow();

                final SpendLimitResponseModel actualCardLevelLimit =
                        actualCardLevelSpendLimits.stream().filter(x -> x.getInterval().equals(spendLimit.getInterval())).findFirst().orElseThrow();

                assertEquals(spendLimit.getValue().getCurrency(), actualLimit.getValue().get("currency"));
                assertEquals(spendLimit.getValue().getAmount().toString(), actualLimit.getValue().get("amount"));
                assertEquals(spendLimit.getInterval(), actualLimit.getInterval());
                assertEquals(spendLimit.getValue().getCurrency(), actualCardLevelLimit.getValue().get("currency"));
                assertEquals(spendLimit.getValue().getAmount().toString(), actualCardLevelLimit.getValue().get("amount"));
                assertEquals(spendLimit.getInterval(), actualCardLevelLimit.getInterval());
            });

            response.body("profileLevelSpendRules.spendLimit", nullValue());

        } else {
            response
                    .body("spendLimit[0].value.currency", equalTo(currency))
                    .body("spendLimit[0].value.amount", equalTo(0))
                    .body("spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                    .body("cardLevelSpendRules.spendLimit[0].value.currency", equalTo(currency))
                    .body("cardLevelSpendRules.spendLimit[0].value.amount", equalTo(0))
                    .body("cardLevelSpendRules.spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        }

        assertEquals("{}", response.extract().jsonPath().get("identityLevelSpendRules").toString());
    }

    private void assertEmptySpendRules(final ValidatableResponse response,
                                       final String currency) {

        response
                .body("spendLimit[0].value.currency", equalTo(currency))
                .body("spendLimit[0].value.amount", equalTo(0))
                .body("spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                .body("cardLevelSpendRules.spendLimit[0].value.currency", equalTo(currency))
                .body("cardLevelSpendRules.spendLimit[0].value.amount", equalTo(0))
                .body("cardLevelSpendRules.spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()));

        assertEquals("{}", response.extract().jsonPath().get("profileLevelSpendRules").toString());
        assertEquals("{}", response.extract().jsonPath().get("identityLevelSpendRules").toString());
    }
}