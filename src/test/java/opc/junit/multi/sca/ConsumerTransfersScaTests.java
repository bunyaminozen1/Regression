package opc.junit.multi.sca;

import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerTransfersScaTests extends AbstractTransfersScaTests {
    private static String managedCardScaAppId;
    private static String managedCardScaMaAppId;

    @BeforeAll
    public static void TestSetup() {

        final CreateConsumerModel createSendDestinationConsumerModelScaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaApp).build();
        final String sendDestinationIdentityTokenScaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createSendDestinationConsumerModelScaApp, secretKeyScaApp).getRight();

        managedCardScaAppId = ManagedCardsHelper.createManagedCard(consumerPrepaidManagedCardProfileIdScaApp, CURRENCY,
                secretKeyScaApp, sendDestinationIdentityTokenScaApp);

        final CreateConsumerModel createSendDestinationConsumerModelScaMaApp =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaMaApp).build();
        final String sendDestinationIdentityTokenScaMaApp = ConsumersHelper.createEnrolledVerifiedConsumer(createSendDestinationConsumerModelScaMaApp, secretKeyScaMaApp).getRight();
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, sendDestinationIdentityTokenScaMaApp);

        managedCardScaMaAppId = ManagedCardsHelper.createManagedCard(consumerPrepaidManagedCardProfileIdScaMaApp, CURRENCY,
                secretKeyScaMaApp, sendDestinationIdentityTokenScaMaApp);
    }

    @Override
    protected String getDestinationManagedCardScaAppId() {
        return managedCardScaAppId;
    }

    @Override
    protected String getIdentityManagedAccountsProfileScaApp() {
        return scaApp.getConsumerPayneticsEeaManagedAccountsProfileId();
    }

    @Override
    protected String getIdentityManagedAccountsProfileScaMaApp() {
        return scaMaApp.getConsumerPayneticsEeaManagedAccountsProfileId();
    }

    @Override
    protected String getDestinationManagedCardScaMaAppId() {
        return managedCardScaMaAppId;
    }

    @Override
    protected String getIdentityManagedCardsProfileScaApp() {
        return scaApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
    }

    @Override
    protected String getIdentityManagedCardsProfileScaMaApp() {
        return scaMaApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
    }

    @Override
    protected Pair<String, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final String token = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey()).getRight();

        return Pair.of(createConsumerModel.getRootUser().getEmail(), token);
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();
        final String token = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey()).getRight();
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, programme.getSecretKey(), token);

        return Pair.of(createConsumerModel.getRootUser().getEmail(), token);
    }
}
