package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class PatchConsumerUserTests extends AbstractPatchUserTests {

    private String consumerId;
    private String authenticationToken;
    private String consumerEmail;
    final private IdentityType identityType = IdentityType.CONSUMER;

    @BeforeEach
    public void BeforeEach() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
        consumerEmail = createConsumerModel.getRootUser().getEmail();
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
    protected IdentityType getIdentityType() {
        return identityType;
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
        return consumerEmail;
    }

}
