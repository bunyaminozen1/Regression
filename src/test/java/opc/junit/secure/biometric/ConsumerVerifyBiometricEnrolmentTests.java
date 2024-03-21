package opc.junit.secure.biometric;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerVerifyBiometricEnrolmentTests extends AbstractVerifyBiometricEnrolmentTests {

  private String identityId;
  private String identityToken;
  final private IdentityType identityType = IdentityType.CONSUMER;
  private String identityEmail;

  @BeforeEach
  public void BeforeEach() {
    final CreateConsumerModel createConsumerModel= CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
    identityToken = consumer.getRight();
    identityId = consumer.getLeft();
    identityEmail = createConsumerModel.getRootUser().getEmail();
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
  protected IdentityType getIdentityType() {
    return identityType;
  }

  @Override
  protected String getIdentityEmail() {return identityEmail;}

  @Override
  protected Triple<String, String, String> createNewIdentity() {
    final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey);
    return Triple.of(consumer.getLeft(), consumerModel.getRootUser().getEmail(), consumer.getRight());
  }
}
