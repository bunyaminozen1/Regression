package opc.junit.multi.securitysimulated.cards.details;

import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.securitysimulated.BaseSecurityConfigurationTests;
import opc.models.innovator.UnassignedCardResponseModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedcards.*;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CardDetailsSecurityDisabledTests extends BaseSecurityConfigurationTests {

    @BeforeAll
    public static void Setup(){
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), false,
                        SecurityModelConfiguration.CARD_NUMBER.name(), false);

        updateSecurityModel(securityModelConfig);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    @Test
    public void SecurityDisabled_CreateManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_GetManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final JsonPath jsonPath =
                ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_GetManagedCardsDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final JsonPath jsonPath =
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cards[0].cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cards[0].cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_PatchManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final JsonPath jsonPath =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_AssignManagedCardDetailsTokenized_Success(){

        final List<UnassignedCardResponseModel> unassignedCards =
                ManagedCardsHelper.replenishPrepaidCardPool(consumerPrepaidManagedCardsProfileId, consumerCurrency, CardLevelClassification.CONSUMER,
                        InstrumentType.VIRTUAL, innovatorToken);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCards.get(0).getExternalHandle())
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_UpgradeManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        final JsonPath jsonPath =
                ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_ActivateManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel =
                new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final JsonPath jsonPath =
                ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_ReplaceLostStolenManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        final JsonPath jsonPath =
                ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), SecurityModelConfiguration.CARD_NUMBER, consumerAuthenticationToken));
    }
}