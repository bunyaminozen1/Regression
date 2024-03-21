package data;

import commons.enums.Currency;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class ManagedCardsTests extends BaseTestSetup {

    private static CreateConsumerModel createConsumerModel;
    private static String consumerAuthenticationToken;

    @BeforeAll
    public static void Setup(){
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_Success(final Currency currency) {

        IntStream.range(0, new RandomDataGenerator().nextInt(1, 5)).forEach(i -> {
            final CreateManagedCardModel createManagedCardModel =
                    CreateManagedCardModel
                            .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, currency.name())
                            .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                            .build();

            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                    .then()
                    .statusCode(SC_OK);
        });
    }

    @Test
    public void CreateManagedCard_Blocked_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateManagedCard_Unblocked_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(createManagedCardModel,
                        secretKey, consumerAuthenticationToken);

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.unblockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateManagedCard_Removed_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(createManagedCardModel,
                        secretKey, consumerAuthenticationToken);

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateManagedCard_UpgradedNotActivated_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(createManagedCardModel,
                        secretKey, consumerAuthenticationToken);

        ManagedCardsService.upgradeManagedCardToPhysical(UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build(),
                        secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_Physical_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .dataCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(createManagedCardModel,
                        secretKey, consumerAuthenticationToken);

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);
    }

    @Test
    public void CreateManagedCard_Lost_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPhysicalPrepaidManagedCards(List.of(createManagedCardModel),
                        secretKey, consumerAuthenticationToken).get(0).getManagedCardId();

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, consumerAuthenticationToken);
    }

    @Test
    public void CreateManagedCard_Stolen_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPhysicalPrepaidManagedCards(List.of(createManagedCardModel),
                        secretKey, consumerAuthenticationToken).get(0).getManagedCardId();

        ManagedCardsHelper.reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken);
    }

    @Test
    public void CreateManagedCard_ReplaceLost_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPhysicalPrepaidManagedCards(List.of(createManagedCardModel),
                        secretKey, consumerAuthenticationToken).get(0).getManagedCardId();

        ManagedCardsHelper.replaceLostCard(secretKey, managedCardId, consumerAuthenticationToken);
    }

    @Test
    public void CreateManagedCard_ReplaceDamaged_Success() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerManagedCardsProfileId, Currency.EUR.name())
                        .setNameOnCard(String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()))
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createPhysicalPrepaidManagedCards(List.of(createManagedCardModel),
                        secretKey, consumerAuthenticationToken).get(0).getManagedCardId();

        ManagedCardsHelper.replaceDamagedCard(secretKey, managedCardId, consumerAuthenticationToken);
    }

    private static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.dataCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                createSteppedUpConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
    }
}