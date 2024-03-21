package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerAuthUserPushEnrolmentTests extends AbstractPushEnrolmentTests{
  private static String identityTokenScaEnrolApp;
  private final IdentityType identityType=IdentityType.CONSUMER;

  @BeforeAll
  public static void TestSetup() {
    final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileIdScaEnrolApp, secretKeyScaEnrolApp);

    final UsersModel modelUserScaApp = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> userScaApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaApp, secretKeyScaEnrolApp, consumer.getRight());
    identityTokenScaEnrolApp = userScaApp.getRight();

    AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, identityTokenScaEnrolApp);
  }
  @Override
  protected String getIdentityTokenScaEnrolApp() {
    return identityTokenScaEnrolApp;
  }

    @Override
    protected IdentityType getIdentityType() {
        return identityType;
    }

  @Override
  protected String createIdentity(final ProgrammeDetailsModel programme) {

    final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(programme.getConsumersProfileId(), programme.getSecretKey());

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), consumer.getRight());

    return user.getRight();
  }
}


