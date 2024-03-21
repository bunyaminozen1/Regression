package opc.junit.multi.users;

import opc.enums.mailhog.MailHogEmail;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InviteCorporateUserTests extends AbstractInviteUserTests {

    private String corporateId;
    private String authenticationToken;
    private CreateCorporateModel createCorporateModel;

    @BeforeEach
    public void BeforeEach() {
        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        authenticationToken = authenticatedCorporate.getRight();
    }

    @Test
    public void SendInvite_CheckEmail_Success() throws SQLException {

        final User user = createNewUser();
        UsersService.inviteUser(getSecretKey(), user.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(user.userDetails.getEmail());
        assertEquals(MailHogEmail.CORPORATE_USER_INVITE.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_USER_INVITE.getSubject(), email.getSubject());
        assertEquals(user.userDetails.getEmail(), email.getTo());
        assertEquals(String.format(MailHogEmail.CORPORATE_USER_INVITE.getEmailText(), createCorporateModel.getRootUser().getName(),
                createCorporateModel.getRootUser().getSurname(), createCorporateModel.getCompany().getName(), corporateId, user.id, CorporatesDatabaseHelper.getEmailNonce(user.id).get(0).get("id"), user.userDetails.getEmail().substring(0, 9) + "= " + user.userDetails.getEmail().substring(9)), email.getBody());
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
        return createCorporateModel.getRootUser().getEmail();
    }

}
