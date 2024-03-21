package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;

public abstract class AbstractActivateUserTests extends AbstractUserTests {

    @Test
    public void ActivateUser_Success() {

        final User newUser = createNewUser();
        final String password = createPassword(newUser.id);

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);

        UsersService.getUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_OK)
                    .body("active", equalTo(false));

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(password)),getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);

        UsersService.activateUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.getUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(password)),getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));
    }

    @Test
    public void ActivateUser_RootUser_NotFound() {

        final User newUser = createNewUser();

        UsersService.activateUser(getSecretKey(), newUser.identityId, getAuthToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateUser_AlreadyActive_Success() {

        final User newUser = createNewUser();

        UsersService.getUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));

        UsersService.activateUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ActivateUser_InvalidUserId_NotFound() {

        UsersService.activateUser(getSecretKey(), RandomStringUtils.randomNumeric(18), getAuthToken())
                    .then()
                    .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateUser_IdentityImpersonator_Forbidden() {

        final User newUser = createNewUser();
        createPassword(newUser.id);

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.getUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(false));

        UsersService.activateUser(getSecretKey(), newUser.id, getBackofficeImpersonateToken(newUser.identityId, newUser.identityType))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_CrossIdentityType_NotFound() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        if (newUser.identityType.equals(IdentityType.CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.activateUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateUser_CrossIdentity_Forbidden() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        if (newUser.identityType.equals(IdentityType.CONSUMER)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.activateUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_OtherProgrammeSecretKey_Forbidden() {
        final User newUser = createNewUser();

        UsersService.activateUser(secretKeyAppTwo, newUser.identityId, getAuthToken())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_NoApiKey_BadRequest() {
        final User newUser = createNewUser();

        UsersService.activateUser("", newUser.identityId, getAuthToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ActivateUser_InvalidKey_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.activateUser(RandomStringUtils.randomAlphanumeric(12), newUser.identityId, getAuthToken())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_NoToken_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.activateUser(secretKeyAppTwo, newUser.identityId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_InvalidToken_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.activateUser(secretKeyAppTwo, newUser.identityId, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
