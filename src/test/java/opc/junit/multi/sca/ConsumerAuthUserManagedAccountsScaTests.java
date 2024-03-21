package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerAuthUserManagedAccountsScaTests extends AbstractManagedAccountsScaTests {
    private static String identityTokenScaApp;
    private static String identityManagedAccountProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityEmailScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityManagedAccountProfileScaMcApp;

    @BeforeAll
    public static void TestSetup() {
        //User for ScaApp
        final CreateConsumerModel createConsumerModelScaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaApp).build();
        final Pair<String, String> authenticatedConsumerScaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaApp, secretKeyScaApp);

        final UsersModel modelUserScaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaApp, secretKeyScaApp, authenticatedConsumerScaApp.getRight());
        identityTokenScaApp = userScaApp.getRight();
        identityManagedAccountProfileScaApp = consumerManagedAccountProfileIdScaApp;

        //User for ScaMaApp
        final CreateConsumerModel createConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        final Pair<String, String> authenticatedConsumerScaMaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaMaApp, secretKeyScaMaApp);

        final UsersModel modelUserScaMaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMaApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaMaApp, secretKeyScaMaApp, authenticatedConsumerScaMaApp.getRight());
        identityTokenScaMaApp = userScaMaApp.getRight();
        identityManagedAccountProfileScaMaApp = consumerManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = modelUserScaMaApp.getEmail();

        //User for ScaMcApp
        final CreateConsumerModel createConsumerModelScaMcApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMcApp).build();
        final Pair<String, String> authenticatedConsumerScaMcApp = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelScaMcApp, secretKeyScaMcApp);

        final UsersModel modelUserScaMcApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMcApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaMcApp, secretKeyScaMcApp, authenticatedConsumerScaMcApp.getRight());
        identityTokenScaMcApp = userScaMcApp.getRight();
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

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(userModel, programme.getSecretKey(), token);

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), programme.getSecretKey(), user.getRight());

        return Pair.of(userModel.getEmail(), user.getRight());
    }

    @Override
    protected Pair<String, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final String token = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey()).getRight();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), token);

        return Pair.of(usersModel.getEmail(), user.getRight());
    }
}
