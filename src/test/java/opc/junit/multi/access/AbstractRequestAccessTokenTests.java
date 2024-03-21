package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.shared.Identity;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public abstract class AbstractRequestAccessTokenTests extends BaseAuthenticationSetup {

    protected abstract String getSecretKey();
    protected abstract String getLoginEmail();
    protected abstract String getIdentityId();
    protected abstract IdentityType getIdentityType();
    protected abstract UserType getUserType();
    protected abstract String createPassword(final String userId);

    @Test
    public void RequestAccessToken_SameUser_Success() {

        final Identity identity = new Identity(new IdentityModel(getIdentityId(), getIdentityType()));
        final String authenticationToken = AuthenticationHelper.login(getLoginEmail(), secretKey);

        AuthenticationService.accessToken(identity, getSecretKey(), authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("credentials.id", equalTo(getIdentityId()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("status", equalTo("STANDARD"));
    }

    @Test
    public void RequestAccessToken_SameUserWrongType_Unauthorized() {

        final IdentityType wrongType =
                getIdentityType().equals(IdentityType.CORPORATE) ? IdentityType.CONSUMER : IdentityType.CORPORATE;

        final Identity identity = new Identity(new IdentityModel(getIdentityId(), wrongType));
        final String authenticationToken = AuthenticationHelper.login(getLoginEmail(), getSecretKey());

        AuthenticationService.accessToken(identity, getSecretKey(), authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RequestAccessToken_SameUserWrongId_NotFound() {

        final Identity identity = new Identity(new IdentityModel(RandomStringUtils.randomNumeric(18), getIdentityType()));
        final String authenticationToken = AuthenticationHelper.login(getLoginEmail(), getSecretKey());

        AuthenticationService.accessToken(identity, getSecretKey(), authenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }
}
