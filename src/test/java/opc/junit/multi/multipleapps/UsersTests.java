package opc.junit.multi.multipleapps;

import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.ConsumerUserInviteModel;
import opc.models.multi.users.UsersModel;
import opc.models.multi.users.ValidateUserInviteModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.UsersService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.*;

public class UsersTests extends BaseApplicationsSetup {

    @Test
    public void CreateUser_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        UsersService.createUser(UsersModel.DefaultUsersModel().build(), applicationTwo.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdateUser_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createAuthenticatedUser(applicationThree.getSecretKey(), authenticationToken).getLeft();

        UsersService.patchUser(UsersModel.DefaultUsersModel().build(), applicationTwo.getSecretKey(), userId, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeactivateUser_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createAuthenticatedUser(applicationThree.getSecretKey(), authenticationToken).getLeft();

        UsersService.deactivateUser(applicationTwo.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createAuthenticatedUser(applicationThree.getSecretKey(), authenticationToken).getLeft();

        UsersService.deactivateUser(applicationThree.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.activateUser(applicationTwo.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUser_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createAuthenticatedUser(applicationThree.getSecretKey(), authenticationToken).getLeft();

        UsersService.getUser(applicationTwo.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUsers_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        UsersHelper.createAuthenticatedUser(applicationThree.getSecretKey(), authenticationToken).getLeft();

        UsersService.getUsers(applicationTwo.getSecretKey(), Optional.empty(), authenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_OtherApplicationKey_Forbidden() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createUser(applicationThree.getSecretKey(), authenticationToken);

        UsersService.inviteUser(applicationTwo.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ValidateInvite_OtherApplicationKey_NotFound() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createUser(applicationThree.getSecretKey(), authenticationToken);

        UsersService.inviteUser(applicationThree.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.validateUserInvite(new ValidateUserInviteModel("123456"), applicationTwo.getSecretKey(), userId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ConsumeInvite_OtherApplicationKey_NotFound() {

        final String authenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationThree.getConsumersProfileId(),
                        applicationThree.getSecretKey()).getRight();

        final String userId = UsersHelper.createUser(applicationThree.getSecretKey(), authenticationToken);

        UsersService.inviteUser(applicationThree.getSecretKey(), userId, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.validateUserInvite(new ValidateUserInviteModel("123456"), applicationThree.getSecretKey(), userId)
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.consumeUserInvite(new ConsumerUserInviteModel("123456", new PasswordModel("Pass1234")),
                applicationTwo.getSecretKey(), userId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
