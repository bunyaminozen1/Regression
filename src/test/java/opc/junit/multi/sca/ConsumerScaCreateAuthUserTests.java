package opc.junit.multi.sca;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class ConsumerScaCreateAuthUserTests extends AbstractScaCreateAuthUserTests{

    @Override
    protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel
                .DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createConsumerModel.getRootUser().getEmail(),
                consumer.getLeft(), consumer.getRight(), IdentityType.CONSUMER, null, null);
    }
}
