package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import org.junit.jupiter.api.BeforeEach;

public class RequestAccessTokenConsumersTests extends AbstractRequestAccessTokenTests {

    private String identityId;
    private String consumerRootEmail;

    @BeforeEach
    public void BeforeEach() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        this.identityId = ConsumersHelper.createConsumer(createConsumerModel, secretKey);
        this.consumerRootEmail = createConsumerModel.getRootUser().getEmail();
        ConsumersHelper.createConsumerPassword(identityId, secretKey);
        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String getLoginEmail() {
        return this.consumerRootEmail;
    }

    @Override
    protected String getIdentityId() {
        return this.identityId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }

    @Override
    protected UserType getUserType() {
        return UserType.ROOT;
    }

    @Override
    protected String createPassword(final String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }
}
