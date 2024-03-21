package opc.junit.multi.multipleapps;

import commons.enums.Currency;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ManagedCardsTests extends BaseApplicationsSetup {

    private static String consumerProfileId;
    private static String managedCardsProfileId;
    private static String secretKey;
    private static String authenticationToken;

    @BeforeAll
    public static void TestSetup() {
        consumerProfileId = applicationTwo.getConsumersProfileId();
        managedCardsProfileId = applicationTwo.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        secretKey = applicationTwo.getSecretKey();

        consumerSetup();
    }

    @Test
    public void CreateManagedCard_Success() {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardsProfileId, Currency.getRandomCurrency().name())
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(managedCardsProfileId));
    }

    @Test
    public void CreateManagedCard_BusinessPurchasingProfile_Forbidden() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(applicationThree.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(),
                                Currency.getRandomCurrency().name())
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedCard_BusinessPayoutsProfile_Forbidden() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(applicationFour.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(),
                                Currency.getRandomCurrency().name())
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedCard_BusinessPurchasingKey_Forbidden() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardsProfileId, Currency.getRandomCurrency().name())
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, applicationThree.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedCard_BusinessPayoutsKey_Forbidden() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardsProfileId, Currency.getRandomCurrency().name())
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, applicationFour.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCard_CrossApplicationUpdate_NotFound() {

        final String consumerAuthenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationFour.getConsumersProfileId(),
                        applicationFour.getSecretKey()).getRight();
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(applicationFour.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), Currency.getRandomCurrency().name())
                        .build();
        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, applicationFour.getSecretKey(), consumerAuthenticationToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();
        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, authenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        authenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }
}