package opc.junit.secure.biometric;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

import static opc.enums.opc.UserType.ROOT;

public class ConsumerLoginWithPasswordTests extends AbstractLoginWithPasswordTests{

    private String identityId;
    private String identityToken;
    private String identityEmail;
    private String identityManagedAccountId;

    @BeforeEach
    public void BeforeEach() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        identityToken = consumer.getRight();
        identityId = consumer.getLeft();
        identityEmail = createConsumerModel.getRootUser().getEmail();

        identityManagedAccountId = ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), secretKey, identityToken);
        AdminHelper.fundManagedAccount(innovatorId, identityManagedAccountId, Currency.EUR.name(), 100000L);
    }

    @Override
    protected String getIdentityId() {
        return identityId;
    }

    @Override
    protected String getIdentityToken() {
        return identityToken;
    }

    @Override
    protected String getIdentityEmail() {
        return identityEmail;
    }

    @Override
    protected String getManagedAccountId() {
        return identityManagedAccountId;
    }

    @Override
    protected UserType getUserType() {
        return ROOT;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }
}
