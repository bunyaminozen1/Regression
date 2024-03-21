package opc.junit.multi.multipleapps;

import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class PasswordsTests extends BaseApplicationsSetup {

    @Test
    public void CreatePassword_OtherApplicationKey_Forbidden() {

        final String identityId = CorporatesHelper.createCorporate(applicationTwo.getCorporatesProfileId(), applicationTwo.getSecretKey());

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(applicationTwo.getSecretKey()))).build();

        PasswordsService.createPassword(createPasswordModel, identityId, applicationThree.getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdatePassword_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                CorporatesHelper.createAuthenticatedCorporate(applicationTwo.getCorporatesProfileId(), applicationTwo.getSecretKey()).getRight();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(applicationThree.getSecretKey())),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, applicationThree.getSecretKey(), authenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartLostPassword_OtherApplicationKey_NotFound() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationTwo.getCorporatesProfileId()).build();
        CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationTwo.getSecretKey()).getRight();

        PasswordsService.startLostPassword(new LostPasswordStartModel(createCorporateModel.getRootUser().getEmail()), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void ResumeLostPassword_OtherApplicationKey_Forbidden() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationTwo.getCorporatesProfileId()).build();
        CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationTwo.getSecretKey()).getRight();

        PasswordsService.startLostPassword(new LostPasswordStartModel(createCorporateModel.getRootUser().getEmail()), applicationTwo.getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, applicationThree.getSecretKey())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }
}
