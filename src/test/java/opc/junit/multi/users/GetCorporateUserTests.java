package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.*;

public class GetCorporateUserTests extends AbstractGetUserTests{

    private String corporateId;
    private String authenticationToken;
    private String corporateEmail;

    @BeforeEach
    public void BeforeEach() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> authenticatedConsumer = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
        corporateEmail = createCorporateModel.getRootUser().getEmail();
    }

    @Override
    protected String getSecretKey() {
        return secretKey;
    }

    @Override
    protected String getAuthToken() {
        return authenticationToken;
    }

    @Override
    protected User createNewUser() {
        final UsersModel model = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(model, getSecretKey(), getAuthToken());
        return new User(userId, model, corporateId, IdentityType.CORPORATE, UserType.USER);
    }

    @Override
    protected String createPassword(String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }

    @Override
    protected String getRootEmail() {
        return corporateEmail;
    }

    @Override
    protected String getIdentityId() {return corporateId;}

    @Test
    public void GetUser_NoDateOfBirth_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .setDateOfBirth(null)
                .build();
        final String userId = UsersHelper.createUser(usersModel, getSecretKey(), getAuthToken());

        UsersService.getUser(getSecretKey(), userId, getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("dateOfBirth.year", nullValue())
                .body("dateOfBirth.month", nullValue())
                .body("dateOfBirth.day", nullValue())
                .body("active", equalTo(true));
    }

    @Test
    public void GetUsers_NoDateOfBirth_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .setDateOfBirth(null)
                .build();
        UsersHelper.createUser(usersModel, getSecretKey(), getAuthToken());

        UsersService.getUsers(getSecretKey(), Optional.empty(), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("users[0].dateOfBirth.year", nullValue())
                .body("users[0].dateOfBirth.month", nullValue())
                .body("users[0].dateOfBirth.day", nullValue())
                .body("users[0].active", equalTo(true))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

}
