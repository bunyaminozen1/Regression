package opc.junit.secure.biometric;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class ConsumerLoginBiometricTests extends AbstractLoginBiometricTests{

  @Override
  protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
            programme.getConsumersProfileId(), programme.getSecretKey());

    return IdentityDetails.generateDetails(null, consumer.getLeft(), consumer.getRight(),
            IdentityType.CONSUMER, null, null);
  }
}
