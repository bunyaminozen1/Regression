package opc.junit.multi.security.cards.details;

import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.security.BaseSecurityConfigurationTests;
import opc.models.innovator.UnassignedCardResponseModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.secure.DetokenizeModel;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CardDetailsSecurityEnabledTests extends BaseSecurityConfigurationTests {

    @BeforeAll
    public static void Setup(){
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        consumerAssociateRandom = associate(consumerAuthenticationToken);
    }

    @Test
    public void SecurityEnabled_CreateManagedCardDetailsTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_GetManagedCardDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_GetManagedCardsDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cards[0].cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cards[0].cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_PatchManagedCardDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_AssignManagedCardDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_UpgradeManagedCardDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_ActivateManagedCardDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_ReplaceLostStolenManagedCardDetailsTokenized_Success(){

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

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_DetokenizeScaEnabledCardDetails_Success() throws InterruptedException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(scaMcApp.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, scaMcApp.getSecretKey());

        final String consumerAssociateRandom = associate(consumer.getRight(), scaMcApp.getSharedKey());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(scaMcApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(),
                        createConsumerModel.getBaseCurrency())
                        .build();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                scaMcApp.getSecretKey(), consumer.getRight());

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, scaMcApp.getSecretKey(), consumer.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel.Builder cardNumberDetokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cardNumber.value"))
                        .setRandom(consumerAssociateRandom);

        final DetokenizeModel.Builder cvvDetokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cvv.value"))
                        .setRandom(consumerAssociateRandom);

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumer.getRight(), scaMcApp.getSharedKey()));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumer.getRight(), scaMcApp.getSharedKey()));

        AuthenticationHelper.logout(consumer.getRight(), scaMcApp.getSecretKey());
        TimeUnit.SECONDS.sleep(31);
        final String newToken = AuthenticationHelper.login(createConsumerModel.getRootUser().getEmail(), scaMcApp.getSecretKey());
        final String newAssociateRandom = associate(newToken, scaMcApp.getSharedKey());

        SecureService.detokenize(scaMcApp.getSharedKey(), newToken, cardNumberDetokenizeModel.setRandom(newAssociateRandom).build())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        SecureService.detokenize(scaMcApp.getSharedKey(), newToken, cvvDetokenizeModel.setRandom(newAssociateRandom).build())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }
}
