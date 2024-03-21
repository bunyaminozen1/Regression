package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerManagedCardsScaTests extends AbstractManagedCardsScaTests{
    private static String identityTokenScaApp;
    private static String identityPrepaidManagedCardProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityPrepaidManagedCardProfileScaMaApp;
    private static CardLevelClassification identityCardLevelClassificationScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityPrepaidManagedCardProfileScaMcApp;
    private static String identityEmailScaMcApp;
    private static CardLevelClassification identityCardLevelClassificationScaMcApp;

    @BeforeAll
    public static void TestSetup() {
        final CreateConsumerModel createConsumerModelScaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaApp).build();
        identityTokenScaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaApp, secretKeyScaApp).getRight();
        identityPrepaidManagedCardProfileScaApp = consumerPrepaidManagedCardProfileIdScaApp;

        final CreateConsumerModel createConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        identityTokenScaMaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaMaApp, secretKeyScaMaApp).getRight();
        identityPrepaidManagedCardProfileScaMaApp = consumerPrepaidManagedCardProfileIdScaMaApp;
        identityCardLevelClassificationScaMaApp = CardLevelClassification.CONSUMER;

        final CreateConsumerModel createConsumerModelScaMcApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMcApp).build();
        identityTokenScaMcApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaMcApp, secretKeyScaMcApp).getRight();
        identityPrepaidManagedCardProfileScaMcApp = consumerPrepaidManagedCardProfileIdScaMcApp;
        identityEmailScaMcApp = createConsumerModelScaMcApp.getRootUser().getEmail();
        identityCardLevelClassificationScaMcApp = CardLevelClassification.CONSUMER;

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identityTokenScaMaApp);
        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identityTokenScaMcApp);
    }

    @Override
    protected String getIdentityTokenScaApp() {
        return identityTokenScaApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaApp() {
        return identityPrepaidManagedCardProfileScaApp;
    }

    @Override
    protected String getIdentityTokenScaMaApp() {
        return identityTokenScaMaApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaMaApp() {
        return identityPrepaidManagedCardProfileScaMaApp;
    }

    @Override
    protected CardLevelClassification getIdentityCardLevelClassificationScaMaApp() {
        return identityCardLevelClassificationScaMaApp;
    }

    @Override
    protected String getIdentityTokenScaMcApp() {
        return identityTokenScaMcApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaMcApp() {
        return identityPrepaidManagedCardProfileScaMcApp;
    }

    @Override
    protected String getIdentityEmailScaMcApp() {
        return identityEmailScaMcApp;
    }

    @Override
    protected CardLevelClassification getIdentityCardLevelClassificationScaMcApp() {
        return identityCardLevelClassificationScaMcApp;
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final String token = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey()).getRight();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), programme.getSecretKey(), token);

        return Pair.of(createConsumerModel.getRootUser().getEmail(), token);
    }

    @Override
    protected Pair<String, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final String token = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey()).getRight();

        return Pair.of(createConsumerModel.getRootUser().getEmail(), token);
    }
}
