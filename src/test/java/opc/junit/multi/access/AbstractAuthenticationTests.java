package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.UUID;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;

public abstract class AbstractAuthenticationTests extends BaseAuthenticationSetup {

    protected abstract String getSecretKey();

    protected abstract String getLoginEmail();

    protected abstract String getLoginPassword();

    protected abstract String getIdentityId();

    protected abstract IdentityType getIdentityType();

    protected abstract UserType getUserType();

    protected abstract String createPassword(final String userId);

    @Test
    public void LoginWithPassword_InvalidEmail_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(RandomStringUtils.randomAlphanumeric(10), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_InvalidEmailFormat_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_UnknownPassword_Forbidden() {
        AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(RandomStringUtils.randomAlphanumeric(10))), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_UnknownEmail_Forbidden() {
        AuthenticationService.loginWithPassword(new LoginModel(UUID.randomUUID() + "@weavr.io", new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }


    @ParameterizedTest
    @NullAndEmptySource
    public void LoginWithPassword_NoPassword_BadRequest(final String password) {
        AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(password)), getSecretKey())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_NoPasswordModel_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), null), getSecretKey())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
