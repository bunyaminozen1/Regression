package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateAuthUserPushEnrolmentTests extends AbstractPushEnrolmentTests {
    private static String identityTokenScaEnrolApp;

    @BeforeAll
    public static void TestSetup() {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledCorporate(corporateProfileIdScaEnrolApp, secretKeyScaEnrolApp);
        final UsersModel modelUserScaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaApp, secretKeyScaEnrolApp, corporate.getRight());
        identityTokenScaEnrolApp =userScaApp.getRight();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, identityTokenScaEnrolApp);
    }

    @Override
    protected String getIdentityTokenScaEnrolApp() {
        return identityTokenScaEnrolApp;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }

    @Override
    protected String createIdentity(final ProgrammeDetailsModel programme) {

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledCorporate(programme.getCorporatesProfileId(), programme.getSecretKey());

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), corporate.getRight());

        return user.getRight();
    }
}