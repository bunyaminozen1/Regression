package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerPushEnrolmentTests extends AbstractPushEnrolmentTests {
    private static String identityTokenScaEnrolApp;

    @BeforeAll
    public static void TestSetup() {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileIdScaEnrolApp, secretKeyScaEnrolApp);
        identityTokenScaEnrolApp =consumer.getRight();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, identityTokenScaEnrolApp);
    }
    @Override
    protected String getIdentityTokenScaEnrolApp() {
        return identityTokenScaEnrolApp;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }

    @Override
    protected String createIdentity(final ProgrammeDetailsModel programme) {

        return ConsumersHelper.createEnrolledConsumer(programme.getConsumersProfileId(), programme.getSecretKey()).getRight();
    }
}