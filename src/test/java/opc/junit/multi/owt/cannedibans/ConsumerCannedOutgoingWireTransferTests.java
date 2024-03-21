package opc.junit.multi.owt.cannedibans;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerCannedOutgoingWireTransferTests extends AbstractCannedOutgoingWireTransferTests {
  private static String identityToken;
  private static String identityId;
  private static String currency;
  private static String identityManagedAccountProfileId;

  @BeforeEach
  public void SourceSetup() { sourceSetup(); }

  private static void sourceSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(
            consumerProfileId).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, secretKey);
    currency = consumerDetails.getBaseCurrency();
    identityToken = authenticatedConsumer.getRight();
    identityId = authenticatedConsumer.getLeft();

    identityManagedAccountProfileId = consumerManagedAccountProfileId;
  }

  @Override
  protected String getToken() {
    return identityToken;
  }

  @Override
  protected String getIdentityId() {
    return identityId;
  }

  @Override
  protected String getCurrency() {
    return currency;
  }

  @Override
  public String getManagedAccountProfileId() {
    return identityManagedAccountProfileId;
  }

  @Override
  protected IdentityType getIdentityType() {
    return IdentityType.CONSUMER;
  }
}
