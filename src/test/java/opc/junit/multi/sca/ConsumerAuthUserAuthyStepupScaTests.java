package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerAuthUserAuthyStepupScaTests extends AbstractAuthyStepupScaTests {
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
        //User for ScaApp
        final CreateConsumerModel createConsumerModelScaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaApp).build();
        final Pair<String, String> authenticatedConsumerScaApp = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModelScaApp, secretKeyScaApp);

        final UsersModel modelUserScaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaApp = UsersHelper.createAuthyEnrolledUser(modelUserScaApp, secretKeyScaApp, authenticatedConsumerScaApp.getRight());
        identityTokenScaApp = userScaApp.getRight();
        identityManagedAccountProfileScaApp = consumerManagedAccountProfileIdScaApp;

        //User for ScaMaApp
        final CreateConsumerModel createConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        final Pair<String, String> authenticatedConsumerScaMaApp = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModelScaMaApp, secretKeyScaMaApp);

        final UsersModel modelUserScaMaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMaApp = UsersHelper.createAuthyEnrolledUser(modelUserScaMaApp, secretKeyScaMaApp, authenticatedConsumerScaMaApp.getRight());
        identityTokenScaMaApp = userScaMaApp.getRight();
        identityManagedAccountProfileScaMaApp = consumerManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = modelUserScaMaApp.getEmail();
        identityManagedCardsProfileScaMaApp = consumerPrepaidManagedCardProfileIdScaMaApp;

        //User for ScaMcApp
        final CreateConsumerModel createConsumerModelScaMcApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMcApp).build();
        final Pair<String, String> authenticatedConsumerScaMcApp = ConsumersHelper.createStepupAuthenticatedVerifiedConsumer(createConsumerModelScaMcApp, secretKeyScaMcApp);

        final UsersModel modelUserScaMcApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMcApp = UsersHelper.createAuthyEnrolledUser(modelUserScaMcApp, secretKeyScaMcApp, authenticatedConsumerScaMcApp.getRight());
        identityTokenScaMcApp = userScaMcApp.getRight();
        identityEmailScaMcApp = modelUserScaMcApp.getEmail();
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

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthyEnrolledUser(usersModel, programme.getSecretKey(), token);

        return Pair.of(usersModel.getEmail(), user.getRight());
    }
}
