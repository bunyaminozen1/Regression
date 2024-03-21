package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import commons.models.DateOfBirthModel;
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
import static org.hamcrest.CoreMatchers.equalTo;

public class PatchCorporateUserTests extends AbstractPatchUserTests {

    private String corporateId;
    private String authenticationToken;
    private String corporateEmail;
    final private IdentityType identityType = IdentityType.CORPORATE;

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
    protected IdentityType getIdentityType(){return identityType; }

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

    @Test
    public void PatchUser_PatchDateOfBirthNotAddedDuringCreate_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().setDateOfBirth(null).build();
        final String userId =
                UsersHelper.createUser(usersModel, getSecretKey(), getAuthToken());

        final UsersModel patchUserDetails = new UsersModel.Builder()
                .setDateOfBirth(new DateOfBirthModel(1981, 11, 11))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), userId, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(userId))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(patchUserDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(patchUserDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(patchUserDetails.getDateOfBirth().getDay()));
    }

}
