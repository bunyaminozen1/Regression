package opc.junit.multi.sca;

import commons.enums.State;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractScaCreateAuthUserTests extends BaseIdentitiesScaSetup {
    protected abstract IdentityDetails getIdentity(final ProgrammeDetailsModel programme);

    @Test
    public void CreateUser_RootUserWithoutSteppedUpToken_StepUpRequired() {
        final IdentityDetails identity = getIdentity(secondaryScaApp);
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void CreateUser_AuthUserWithoutSteppedUpToken_StepUpRequired() {
        final String verificationCode = TestHelper.OTP_VERIFICATION_CODE;
        final String channel = EnrolmentChannel.SMS.name();

        final IdentityDetails identity = getIdentity(secondaryScaApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(verificationCode, channel, secretKeySecondaryScaApp, identity.getToken());
        AuthenticationHelper.startAndVerifyStepup(verificationCode, channel, secretKeySecondaryScaApp, identity.getToken());

        final Pair<String, String> firstUser = UsersHelper.createAuthenticatedUser(secretKeySecondaryScaApp, identity.getToken());

        final UsersModel user = UsersModel.DefaultUsersModel().build();

        UsersService.createUser(user, secretKeySecondaryScaApp, firstUser.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void CreateUser_AuthUserWithoutHavingMobileNumberBiometricStepUp_Success() {
        final String verificationCode = TestHelper.OTP_VERIFICATION_CODE;
        final String channel = EnrolmentChannel.SMS.name();

        final IdentityDetails identity = getIdentity(secondaryScaApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(verificationCode, channel, secretKeySecondaryScaApp, identity.getToken());
        AuthenticationHelper.startAndVerifyStepup(verificationCode, channel, secretKeySecondaryScaApp, identity.getToken());

        final UsersModel firstUserModel = UsersModel.DefaultUsersModel().setMobile(null).build();
        final Pair<String, String> firstUser = UsersHelper.createAuthenticatedUser(firstUserModel, secretKeySecondaryScaApp, identity.getToken());

        final UsersModel secondUserModel = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(secondUserModel, secretKeySecondaryScaApp, firstUser.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        SecureHelper.enrolAndVerifyBiometric(firstUser.getLeft(), sharedKeySecondaryScaApp, secretKeySecondaryScaApp, firstUser.getRight());
        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), secretKeySecondaryScaApp, firstUser.getRight());
        SimulatorHelper.acceptBiometricStepUp(secretKeySecondaryScaApp, sessionId);

        UsersService.createUser(secondUserModel, secretKeySecondaryScaApp, firstUser.getRight(),Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateUser_StepUpOtp_Success() {
        final String verificationCode = TestHelper.OTP_VERIFICATION_CODE;
        final String channel = EnrolmentChannel.SMS.name();

        final IdentityDetails identity = getIdentity(secondaryScaApp);
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(verificationCode, channel, secretKeySecondaryScaApp, identity.getToken());
        AuthenticationHelper.startAndVerifyStepup(verificationCode, channel, secretKeySecondaryScaApp, identity.getToken());

        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateUser_StepUpAuthy_Success() {

        final IdentityDetails identity = getIdentity(secondaryScaApp);
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(identity.getId(), secretKeySecondaryScaApp, identity.getToken());
        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeySecondaryScaApp, identity.getToken());
        SimulatorHelper.acceptAuthyStepUp(secretKeySecondaryScaApp, sessionId);

        AuthenticationFactorsHelper.checkAuthenticationFactorState(EnrolmentChannel.AUTHY, State.ACTIVE, secretKeySecondaryScaApp, identity.getToken());

        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateUser_StepUpBiometric_Success() {

        final IdentityDetails identity = getIdentity(secondaryScaApp);
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        SecureHelper.enrolAndVerifyBiometric(identity.getId(), sharedKeySecondaryScaApp, secretKeySecondaryScaApp, identity.getToken());
        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.BIOMETRIC.name(), secretKeySecondaryScaApp, identity.getToken());

        SimulatorHelper.acceptBiometricStepUp(secretKeySecondaryScaApp, sessionId);

        UsersService.createUser(user, secretKeySecondaryScaApp, identity.getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }
}
