package opc.junit.secure;

import opc.junit.helpers.innovator.InnovatorHelper;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetAuthenticationTypeTests extends BaseSecureSetup{

    @Test
    public void GetAuthenticationType_Password_Success(){

        SecureService.getAuthenticationType(sharedKey)
                .then()
                .statusCode(SC_OK)
                .body("type", equalTo("PASSWORD"))
                .body("length", equalTo(8));
    }

    @Test
    public void GetAuthenticationType_Passcode_Success(){

        SecureService.getAuthenticationType(passcodeApp.getSharedKey())
                .then()
                .statusCode(SC_OK)
                .body("type", equalTo("PASSCODE"))
                .body("length", equalTo(4));
    }

    @Test
    public void GetAuthenticationType_WithoutSharedKey_BadRequest(){

        SecureService.getAuthenticationType("")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("programme-key"));
    }

    @Test
    public void GetAuthenticationType_InvalidSharedKey_(){

        SecureService.getAuthenticationType(RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body( "message", equalTo("Invalid programme key"));
    }
}
