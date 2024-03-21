package opc.junit.multi.passwords;

import io.vavr.Tuple5;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.PasswordValidationModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.CoreMatchers.equalTo;

public class PasswordsValidateTests extends BasePasswordSetup {

    @Test
    public void ValidatePassword_PasswordValid_Success(){
        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey))), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ValidatePassword_NoPasswordProfile_UnresolvedIdentity(){
        final Tuple5<String, String, String, String, String> innovator = InnovatorHelper.registerLoggedInInnovatorWithProgramme();

        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey))), innovator._4())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_IDENTITY"));
    }

    @Test
    public void ValidatePassword_ShortPassword_PasswordTooShort(){
        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(RandomStringUtils.randomAlphanumeric(7))), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));
    }

    @Test
    public void ValidatePassword_LongPassword_PasswordTooLong(){
        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(RandomStringUtils.randomAlphanumeric(51))), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void ValidatePassword_NoPassword_BadRequest(final String password){
        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(password)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ValidatePassword_NoPasswordModel_BadRequest(){
        PasswordsService.validatePassword(new PasswordValidationModel(null), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
