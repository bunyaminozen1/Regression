package opc.junit.multi.securitysimulated.passwords;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerPasswordSecurityDisabledTests extends AbstractPasswordSecurityDisabledTests{

    @BeforeEach
    public void IndividualSetup(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        identityId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
        identityEmail = createConsumerModel.getRootUser().getEmail();
        identityType = IdentityType.CONSUMER;
        identityProfileId = consumerProfileId;

        ConsumersHelper.verifyKyc(secretKey, identityId);
    }

    @Override
    protected String createIdentity() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(identityProfileId).build();

        return ConsumersHelper.createConsumer(createConsumerModel, secretKey);
    }
}
