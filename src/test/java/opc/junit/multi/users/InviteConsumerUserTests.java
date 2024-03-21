package opc.junit.multi.users;

import opc.enums.mailhog.MailHogEmail;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
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

public class InviteConsumerUserTests extends AbstractInviteUserTests {

    private String consumerId;
    private String authenticationToken;
    private CreateConsumerModel createConsumerModel;

    @BeforeEach
    public void BeforeEach() {
        createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
    }

    @Test
    public void SendInvite_CheckEmail_Success() throws SQLException {

        final User user = createNewUser();
        UsersService.inviteUser(getSecretKey(), user.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(user.userDetails.getEmail());
        assertEquals(MailHogEmail.CONSUMER_USER_INVITE.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CONSUMER_USER_INVITE.getSubject(), email.getSubject());
        assertEquals(user.userDetails.getEmail(), email.getTo());
        assertEquals(String.format(MailHogEmail.CONSUMER_USER_INVITE.getEmailText(), createConsumerModel.getRootUser().getName(),
                createConsumerModel.getRootUser().getSurname(), consumerId, user.id, ConsumersDatabaseHelper.getEmailNonce(user.id).get(0).get("id"), user.userDetails.getEmail()), email.getBody());
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
        return new User(userId, model, consumerId, IdentityType.CONSUMER, UserType.USER);
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
        return createConsumerModel.getRootUser().getEmail();
    }

}
