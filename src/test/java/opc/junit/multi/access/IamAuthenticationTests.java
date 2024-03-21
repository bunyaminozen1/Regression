package opc.junit.multi.access;

import opc.junit.helpers.innovator.InnovatorHelper;
import opc.services.multi.AuthenticationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.notNullValue;

public class IamAuthenticationTests extends BaseAuthenticationSetup {

    @BeforeAll
    public static void Setup() {

        InnovatorHelper.enableAndUpdateAuth0(InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), programmeId);
    }

    @Test
    public void LoginRootWithPassword_Success() {
        AuthenticationService.loginWithIam(secretKey)
                .then()
                .statusCode(SC_OK)
                .body("url", notNullValue());
    }

    @Test
    public void LoginRootWithPassword_Auth0NotEnabled_NotFound() {
        AuthenticationService.loginWithIam(applicationThree.getSecretKey())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", ""})
    public void LoginWithPassword_InvalidApiKey_BadRequest(final String secretKey) {
        AuthenticationService.loginWithIam(secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_UnknownApiKey_Unauthorised() {
        AuthenticationService.loginWithIam(RandomStringUtils.randomAlphanumeric(24))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
