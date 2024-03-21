package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.admin.ImpersonateIdentityModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;

import java.sql.SQLException;

public class ConsumerPerpetualTokenScaTests extends AbstractPerpetualTokenScaTests {

    private static String initialPerpetualTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityManagedCardsProfileScaMaApp;
    private static String identityEmailScaMaApp;

    private static String initialPerpetualTokenScaMcApp;
    private static String identityEmailScaMcApp;
    private static String identityPrepaidManagedCardProfileScaMcApp;
    private static CardLevelClassification identityCardLevelClassificationScaMcApp;
    private static String sendDestinationInitialPerpetualTokenScaMaApp;

    @BeforeAll
    public static void TestSetup() throws SQLException {

        final CreateConsumerModel createConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        Pair<String, String> consumerScaMaApp = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModelScaMaApp, secretKeyScaMaApp);
        final ImpersonateIdentityModel impersonateIdentityModelScaMaApp = new ImpersonateIdentityModel(programmeIdScaMaApp, consumerScaMaApp.getLeft());
        initialPerpetualTokenScaMaApp = AdminService.impersonateIdentity(impersonateIdentityModelScaMaApp, adminToken);
        identityManagedAccountProfileScaMaApp = consumerManagedAccountProfileIdScaMaApp;
        identityManagedCardsProfileScaMaApp = consumerPrepaidManagedCardProfileIdScaMaApp;
        identityEmailScaMaApp = createConsumerModelScaMaApp.getRootUser().getEmail();

        final CreateConsumerModel createConsumerModelScaMcApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMcApp).build();
        Pair<String, String> consumerScaMcApp = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModelScaMcApp, secretKeyScaMcApp);
        final ImpersonateIdentityModel impersonateIdentityModelScaMcApp = new ImpersonateIdentityModel(programmeIdScaMcApp, consumerScaMcApp.getLeft());
        initialPerpetualTokenScaMcApp = AdminService.impersonateIdentity(impersonateIdentityModelScaMcApp, adminToken);
        identityEmailScaMcApp = createConsumerModelScaMcApp.getRootUser().getEmail();
        identityPrepaidManagedCardProfileScaMcApp = consumerPrepaidManagedCardProfileIdScaMcApp;
        identityCardLevelClassificationScaMcApp = CardLevelClassification.CONSUMER;

        final CreateConsumerModel createSendDestinationConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        final Pair<String, String> consumerSendDestinationScaMaApp = ConsumersHelper.createAuthenticatedVerifiedConsumer(createSendDestinationConsumerModelScaMaApp, secretKeyScaMaApp);
        final ImpersonateIdentityModel impersonateSendDestinationIdentityModelScaMaApp = new ImpersonateIdentityModel(programmeIdScaMaApp, consumerSendDestinationScaMaApp.getLeft());
        sendDestinationInitialPerpetualTokenScaMaApp = AdminService.impersonateIdentity(impersonateSendDestinationIdentityModelScaMaApp, adminToken);
    }

    @Override
    protected String getInitialPerpetualTokenScaMaApp() {
        return initialPerpetualTokenScaMaApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaMaApp() {
        return identityManagedAccountProfileScaMaApp;
    }

    @Override
    protected String getIdentityManagedCardsProfileScaMaApp() {
        return identityManagedCardsProfileScaMaApp;
    }

    @Override
    protected String getIdentityEmailScaMaApp() {
        return identityEmailScaMaApp;
    }

    @Override
    protected String getInitialPerpetualTokenScaMcApp() {
        return initialPerpetualTokenScaMcApp;
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
    protected String getSendDestinationInitialPerpetualTokenScaMaApp() {
        return sendDestinationInitialPerpetualTokenScaMaApp;
    }

    @Override
    protected Triple<String, ImpersonateIdentityModel, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final Pair<String, String> identity = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, programme.getSecretKey());

        final ImpersonateIdentityModel impersonateIdentityModel = new ImpersonateIdentityModel(programme.getProgrammeId(), identity.getLeft());
        final String initialPerpetualToken = AdminService.impersonateIdentity(impersonateIdentityModel, adminToken);

        return Triple.of(createConsumerModel.getRootUser().getEmail(), impersonateIdentityModel, initialPerpetualToken);
    }
}
