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

public abstract class AbstractDeactivateUserTests extends AbstractUserTests {

    @Test
    public void DeDeactivateUser_Success() {

        final User newUser = createNewUser();
        final String password = createPassword(newUser.id);

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(password)),getSecretKey())
                .then()
                .statusCode(SC_OK);

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(password)),getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);

        UsersService.getUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_OK)
                    .body("active", equalTo(false));
    }

    @Test
    public void DeDeactivateUser_RootUser_NotFound() {
        final User newUser = createNewUser();

        UsersService.deactivateUser(getSecretKey(), newUser.identityId, getAuthToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeDeactivateUser_AlreadyInactive_Success() {
        final User newUser = createNewUser();

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.deactivateUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void DeDeactivateUser_NotFound() {
        UsersService.deactivateUser(getSecretKey(), RandomStringUtils.randomNumeric(18), getAuthToken())
                    .then()
                    .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeDeactivateUser_IdentityImpersonator_Forbidden() {

        final User newUser = createNewUser();
        final String password = createPassword(newUser.id);

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(password)),getSecretKey())
                .then()
                .statusCode(SC_OK);

        UsersService.deactivateUser(getSecretKey(), newUser.id, getBackofficeImpersonateToken(newUser.identityId, newUser.identityType))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeactivateUser_CrossIdentityType_NotFound() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.deactivateUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeactivateUser_CrossIdentity_Forbidden() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CONSUMER)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.deactivateUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
