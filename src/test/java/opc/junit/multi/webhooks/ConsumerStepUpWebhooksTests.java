package opc.junit.multi.webhooks;

import opc.junit.helpers.multi.ConsumersHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerStepUpWebhooksTests extends AbstractStepUpWebhooksTests{
  private String identityId;
  private String identityToken;

  @BeforeEach
  public void BeforeEach() {
    final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(
        passcodeAppConsumerProfileId, passcodeAppSecretKey);
    identityToken = consumer.getRight();
    identityId = consumer.getLeft();
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
  protected String getIdentityType() {
    return "consumers";
  }
}
