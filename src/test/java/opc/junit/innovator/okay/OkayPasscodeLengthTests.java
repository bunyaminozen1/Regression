package opc.junit.innovator.okay;

import opc.enums.opc.PasswordConstraint;
import opc.junit.database.OkayDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.innovator.passwords.BasePasswordsSetup;
import opc.models.innovator.CreatePasswordProfileModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

class OkayPasscodeLengthTests extends BasePasswordsSetup {

    @BeforeAll
    public static void enableBiometrics() {
        InnovatorService.enableOkay(programmeIdAppOne, innovatorToken);
        InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PasswordConstraint.PASSCODE), programmeIdAppOne, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @AfterEach
    public void setPasswordConstraint(){
        InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(PasswordConstraint.PASSWORD), programmeIdAppOne, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    void PasscodeLength_UpdateProfilesWithLength_Success(){

        final int passcodeLength = 6;
        setPasscodeProfileWithLength(corporateProfileIdAppOne, passcodeLength);
        setPasscodeProfileWithLength(consumerProfileIdAppOne, passcodeLength);

        InnovatorService.getProfileConstraint(programmeIdAppOne, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("constraint", equalTo("PASSCODE"))
                .body("length", equalTo(passcodeLength));

        validateOkayPasscodeLength(passcodeLength);
    }

    @Test
    void PasscodeLength_UpdateDifferentProfilesWithDifferentLength_LastWins_Success() {

        setPasscodeProfileWithLength(corporateProfileIdAppOne, 5);
        validateOkayPasscodeLength(5);
        setPasscodeProfileWithLength(consumerProfileIdAppOne, 6);
        validateOkayPasscodeLength(6);

        setPasscodeProfileWithLength(consumerProfileIdAppOne, 5);
        setPasscodeProfileWithLength(corporateProfileIdAppOne, 6);
        validateOkayPasscodeLength(6);

        setPasscodeProfileWithLength(corporateProfileIdAppOne, 5);
        setPasscodeProfileWithLength(consumerProfileIdAppOne, 6);
        validateOkayPasscodeLength(6);
    }

    private void setPasscodeProfileWithLength(final String profileId, final int passcodeLength){

        final CreatePasswordProfileModel passwordProfileModel = CreatePasswordProfileModel
                .setRandomPasswordProfileModel(passcodeLength, passcodeLength);

        AdminService.updatePasswordProfile(adminToken, programmeIdAppOne, profileId, passwordProfileModel)
                .then()
                .statusCode(SC_OK);
    }

    private static void validateOkayPasscodeLength(final int passcodeLength) {
        TestHelper.ensureDatabaseResultAsExpected(10,
                () -> OkayDatabaseHelper.getApp(programmeIdAppOne),
                x -> !x.isEmpty() && x.get(0).get("passcode_length").equals(String.valueOf(passcodeLength)),
                Optional.of(String.format("Passcode length for programme %s does not have value %d as expected",
                        programmeIdAppOne, passcodeLength)));
    }
}
