package opc.junit.innovator.passwords;

import opc.enums.opc.PasswordConstraint;
import opc.junit.helpers.TestHelper;
import opc.models.innovator.CreatePasswordProfileModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public abstract class AbstractPasscodeLengthTests extends BasePasswordsSetup {

    @AfterEach
    public void setPasswordConstraint(){
        InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PasswordConstraint.PASSWORD), programmeIdAppOne, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
    @Test
    public void PasscodeLength_UpdateProfileWithLengthSixRootUserCreatePassword_Success(){

        final IdentityDetails identity = getWithoutPasswordIdentity(applicationOne);
        final int passcodeLength = 6;
        setPasscodeConstraintWithLength(passcodeLength);

        final CreatePasswordModel firstPasswordModel = createPasswordModel(passcodeLength-1);
        PasswordsService.createPassword(firstPasswordModel, identity.getId(), secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final CreatePasswordModel secondPasswordModel = createPasswordModel(passcodeLength+1);
        PasswordsService.createPassword(secondPasswordModel, identity.getId(), secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final CreatePasswordModel thirdPasswordModel = createPasswordModel(passcodeLength);
        PasswordsService.createPassword(thirdPasswordModel, identity.getId(), secretKeyAppOne)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identity.getIdentityType().name()))
                .body("passwordInfo.identityId.id", equalTo(identity.getId()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void PasscodeLength_UpdateProfileWithLengthSevenRootUserUpdatePassword_Success(){

        final IdentityDetails identity = getPasswordCreatedIdentity(applicationOne);
        final int passcodeLength = 7;
        setPasscodeConstraintWithLength(passcodeLength);

        final UpdatePasswordModel updatePasswordModel = updatePasswordModel(passcodeLength-1, secretKeyAppOne);
        PasswordsService.updatePassword(updatePasswordModel, secretKeyAppOne, identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final UpdatePasswordModel secondUpdatePasswordModel =updatePasswordModel(passcodeLength+1, secretKeyAppOne);
        PasswordsService.updatePassword(secondUpdatePasswordModel, secretKeyAppOne, identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final UpdatePasswordModel thirdUpdatePasswordModel =updatePasswordModel(passcodeLength, secretKeyAppOne);
        PasswordsService.updatePassword(thirdUpdatePasswordModel, secretKeyAppOne, identity.getToken())
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identity.getIdentityType().name()))
                .body("passwordInfo.identityId.id", equalTo(identity.getId()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void PasscodeLength_UpdateProfileWithLengthEightRootUserForgotPassword_Success(){

        final IdentityDetails identity = getPasswordCreatedIdentity(applicationOne);

        final int passcodeLength = 8;
        setPasscodeConstraintWithLength(passcodeLength);

        PasswordsService.startLostPassword(new LostPasswordStartModel(identity.getEmail()), secretKeyAppOne)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel firstPasswordResumeModel = lostPasswordResumeModel(identity.getEmail(), passcodeLength-1);

        PasswordsService.resumeLostPassword(firstPasswordResumeModel, secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final LostPasswordResumeModel secondPasswordResumeModel = lostPasswordResumeModel(identity.getEmail(), passcodeLength+1);

        PasswordsService.resumeLostPassword(secondPasswordResumeModel, secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final LostPasswordResumeModel thirdPasswordResumeModel =lostPasswordResumeModel(identity.getEmail(), passcodeLength);

        PasswordsService.resumeLostPassword(thirdPasswordResumeModel, secretKeyAppOne)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void PasscodeLength_UpdateProfileWithLengthAuthorizedUserCreatePassword_Success(){

        final IdentityDetails invitedUser = getInvitedUser(applicationOne);
        final int passcodeLength = 6;
        setPasscodeConstraintWithLength(passcodeLength);

        final CreatePasswordModel firstPasswordModel = createPasswordModel(passcodeLength-1);
        PasswordsService.createPassword(firstPasswordModel, invitedUser.getId(), secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final CreatePasswordModel secondPasswordModel = createPasswordModel(passcodeLength+1);
        PasswordsService.createPassword(secondPasswordModel, invitedUser.getId(), secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final CreatePasswordModel thirdPasswordModel = createPasswordModel(passcodeLength);
        PasswordsService.createPassword(thirdPasswordModel, invitedUser.getId(), secretKeyAppOne)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(invitedUser.getIdentityType().name()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void PasscodeLength_UpdateProfileWithLengthSevenAuthorizedUserUpdatePassword_Success(){

        final IdentityDetails user = getPasswordCreatedUser(applicationOne);
        final int passcodeLength = 7;
        setPasscodeConstraintWithLength(passcodeLength);

        final UpdatePasswordModel updatePasswordModel = updatePasswordModel(passcodeLength-1, secretKeyAppOne);
        PasswordsService.updatePassword(updatePasswordModel, secretKeyAppOne, user.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final UpdatePasswordModel secondUpdatePasswordModel =updatePasswordModel(passcodeLength+1, secretKeyAppOne);
        PasswordsService.updatePassword(secondUpdatePasswordModel, secretKeyAppOne, user.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final UpdatePasswordModel thirdUpdatePasswordModel =updatePasswordModel(passcodeLength, secretKeyAppOne);
        PasswordsService.updatePassword(thirdUpdatePasswordModel, secretKeyAppOne, user.getToken())
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(user.getIdentityType().name()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void PasscodeLength_UpdateProfileWithLengthEightAuthorizedUserForgotPassword_Success(){

        final IdentityDetails user = getPasswordCreatedUser(applicationOne);

        final int passcodeLength = 8;
        setPasscodeConstraintWithLength(passcodeLength);

        PasswordsService.startLostPassword(new LostPasswordStartModel(user.getEmail()), secretKeyAppOne)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel firstPasswordResumeModel = lostPasswordResumeModel(user.getEmail(), passcodeLength-1);

        PasswordsService.resumeLostPassword(firstPasswordResumeModel, secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final LostPasswordResumeModel secondPasswordResumeModel = lostPasswordResumeModel(user.getEmail(), passcodeLength+1);

        PasswordsService.resumeLostPassword(secondPasswordResumeModel, secretKeyAppOne)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final LostPasswordResumeModel thirdPasswordResumeModel =lostPasswordResumeModel(user.getEmail(), passcodeLength);

        PasswordsService.resumeLostPassword(thirdPasswordResumeModel, secretKeyAppOne)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    private void setPasscodeConstraintWithLength(final int passcodeLength){

        final CreatePasswordProfileModel passwordProfileModel = CreatePasswordProfileModel
                .setRandomPasswordProfileModel(passcodeLength, passcodeLength);

        AdminService.updatePasswordProfile(adminToken, programmeIdAppOne, corporateProfileIdAppOne, passwordProfileModel)
                .then()
                .statusCode(SC_OK);

        InnovatorService.getProfileConstraint(programmeIdAppOne, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("constraint", equalTo("PASSCODE"))
                .body("length", equalTo(passcodeLength));
    }

    private CreatePasswordModel createPasswordModel(final int length){
        return CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(RandomStringUtils.randomNumeric(length))).build();
    }

    private UpdatePasswordModel updatePasswordModel(final int length,
                                                    final String secretKey){
        return new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                new PasswordModel(RandomStringUtils.randomAlphanumeric(length)));
    }

    private LostPasswordResumeModel lostPasswordResumeModel(final String email, final int length){
        return LostPasswordResumeModel
                .newBuilder()
                .setEmail(email)
                .setNewPassword(new PasswordModel(RandomStringUtils.randomNumeric(length)))
                .setNonce(TestHelper.VERIFICATION_CODE)
                .build();
    }
    protected abstract IdentityDetails getPasswordCreatedIdentity(final ProgrammeDetailsModel programme);
    protected abstract IdentityDetails getPasswordCreatedUser(final ProgrammeDetailsModel programme);
    protected abstract IdentityDetails getWithoutPasswordIdentity(final ProgrammeDetailsModel programme);
    protected abstract IdentityDetails getInvitedUser(final ProgrammeDetailsModel programme);
}
