package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerAuthyStepupScaTests extends AbstractAuthyStepupScaTests {
    private static String identityTokenScaApp;
    private static String identityManagedAccountProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityEmailScaMaApp;
    private static String identityManagedCardsProfileScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityEmailScaMcApp;
    private static String identityPrepaidManagedCardProfileScaMcApp;
    private static CardLevelClassification identityCardLevelClassificationScaMcApp;
    private static String sendDestinationIdentityTokenScaMaApp;

    @BeforeAll
    public static void TestSetup() {
        final CreateConsumerModel createConsumerModelScaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaApp).build();
        identityTokenScaApp = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModelScaApp, secretKeyScaApp).getRight();
        identityManagedAccountProfileScaApp = consumerManagedAccountProfileIdScaApp;

        final CreateConsumerModel createConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        identityTokenScaMaApp = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModelScaMaApp, secretKeyScaMaApp).getRight();
        identityManagedAccountProfileScaMaApp = consumerManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = createConsumerModelScaMaApp.getRootUser().getEmail();
        identityManagedCardsProfileScaMaApp = consumerPrepaidManagedCardProfileIdScaMaApp;

        final CreateConsumerModel createConsumerModelScaMcApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMcApp).build();
        identityTokenScaMcApp = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModelScaMcApp, secretKeyScaMcApp).getRight();
        identityEmailScaMcApp = createConsumerModelScaMcApp.getRootUser().getEmail();
        identityPrepaidManagedCardProfileScaMcApp = consumerPrepaidManagedCardProfileIdScaMcApp;
        identityCardLevelClassificationScaMcApp = CardLevelClassification.CONSUMER;

        final CreateConsumerModel createSendDestinationConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        sendDestinationIdentityTokenScaMaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createSendDestinationConsumerModelScaMaApp, secretKeyScaMaApp).getRight();

        authyStepUpAccepted(identityTokenScaMaApp, secretKeyScaMaApp);
        authyStepUpAccepted(identityTokenScaMcApp, secretKeyScaMcApp);
    }

    @Override
    protected String getIdentityTokenScaApp() {
        return identityTokenScaApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaApp() {
        return identityManagedAccountProfileScaApp;
    }

    @Override
    protected String getIdentityTokenScaMaApp() {
        return identityTokenScaMaApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaMaApp() {
        return identityManagedAccountProfileScaMaApp;
    }

    @Override
    protected String getIdentityEmailScaMaApp() {
        return identityEmailScaMaApp;
    }

    @Override
    protected String getIdentityManagedCardsProfileScaMaApp() {
        return identityManagedCardsProfileScaMaApp;
    }

    @Override
    protected String getIdentityTokenScaMcApp() {
        return identityTokenScaMcApp;
    }

    @Override
    protected String getIdentityEmailScaMcApp() {
        return identityEmailScaMcApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaMcApp() {
        return identityPrepaidManagedCardProfileScaMcApp;
    }

    @Override
    protected CardLevelClassification getIdentityCardLevelClassificationScaMcApp() {
        return identityCardLevelClassificationScaMcApp;
    }

    @Override
    protected String getSendDestinationIdentityTokenScaMaApp() {
        return sendDestinationIdentityTokenScaMaApp;
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final String token = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModel, programme.getSecretKey()).getRight();
        return Pair.of(createConsumerModel.getRootUser().getEmail(), token);
    }
}
