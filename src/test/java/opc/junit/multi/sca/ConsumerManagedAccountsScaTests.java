package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerManagedAccountsScaTests extends AbstractManagedAccountsScaTests {
    private static String identityTokenScaApp;
    private static String identityManagedAccountProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityEmailScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityManagedAccountProfileScaMcApp;

    @BeforeAll
    public static void TestSetup() {
        final CreateConsumerModel createConsumerModelScaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaApp).build();
        identityTokenScaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaApp, secretKeyScaApp).getRight();
        identityManagedAccountProfileScaApp = consumerManagedAccountProfileIdScaApp;

        final CreateConsumerModel createConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        identityTokenScaMaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaMaApp, secretKeyScaMaApp).getRight();
        identityManagedAccountProfileScaMaApp = consumerManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = createConsumerModelScaMaApp.getRootUser().getEmail();

        final CreateConsumerModel createConsumerModelScaMcApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMcApp).build();
        identityTokenScaMcApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaMcApp, secretKeyScaMcApp).getRight();
        identityManagedAccountProfileScaMcApp = consumerManagedAccountProfileIdScaMcApp;

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identityTokenScaMaApp);
        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identityTokenScaMcApp);
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
    protected String getIdentityTokenScaMcApp() {
        return identityTokenScaMcApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaMcApp() {
        return identityManagedAccountProfileScaMcApp;
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
