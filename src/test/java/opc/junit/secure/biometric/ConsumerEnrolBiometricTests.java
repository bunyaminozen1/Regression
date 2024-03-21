package opc.junit.secure.biometric;

import opc.enums.opc.IdentityType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class ConsumerEnrolBiometricTests extends AbstractEnrolBiometricTests {

  private String identityMobileCountryCode;
  private String identityMobileNumber;

  @Override
  protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

    final CreateConsumerModel createConsumerModel = CreateConsumerModel
            .DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

    identityMobileCountryCode = createConsumerModel.getRootUser().getMobile().getCountryCode();
    identityMobileNumber = createConsumerModel.getRootUser().getMobile().getNumber();

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, programme.getSecretKey());

    return IdentityDetails.generateDetails(createConsumerModel.getRootUser().getEmail(),
            consumer.getLeft(), consumer.getRight(), IdentityType.CONSUMER, null, null);
  }

  @Override
  protected String getIdentityMobileNumber() { return identityMobileNumber;}

  @Override
  protected String getIdentityMobileCountryCode() {return identityMobileCountryCode;}

  @Override
  protected String getSmsAudit(final String identityId) {

    return TestHelper.ensureDatabaseDataRetrieved(10,
            () -> ConsumersDatabaseHelper.getConsumerSmsNewestToOldest(identityId),
            x -> x.size() > 0).get(0).get("text");
  }
}

