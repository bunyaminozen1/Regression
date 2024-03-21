package opc.junit.multi.webhooks;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerBeneficiariesWebhooksTests extends AbstractBeneficiariesWebhooksTests{
  private static String destinationIdentityToken;
  private static String destinationCurrency;
  private String identityToken;
  private String identityId;
  private static String destinationIdentityName;
  private static String destinationIdentityPrepaidManagedCardProfileId;
  private static String destinationIdentityDebitManagedCardProfileId;
  private static String destinationIdentityManagedAccountProfileId;

  @BeforeAll
  public static void DestinationSetup() { destinationSetup(); }

  @BeforeEach
  public void SourceSetup() { sourceSetup(); }

  private static void destinationSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.DefaultCreateConsumerModel(destinationConsumerProfileId).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, destinationSecretKey);
    destinationIdentityToken = authenticatedConsumer.getRight();
    destinationCurrency = consumerDetails.getBaseCurrency();
    destinationIdentityName = String.format("%s %s", consumerDetails.getRootUser().getName(),
        consumerDetails.getRootUser().getSurname());

    destinationIdentityPrepaidManagedCardProfileId = destinationConsumerPrepaidManagedCardsProfileId;
    destinationIdentityDebitManagedCardProfileId = destinationConsumerDebitManagedCardsProfileId;
    destinationIdentityManagedAccountProfileId = destinationConsumerManagedAccountsProfileId;
  }

  private void sourceSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, secretKey);
    identityToken = authenticatedConsumer.getRight();
    identityId = authenticatedConsumer.getLeft();
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
  protected String getDestinationToken() {
    return destinationIdentityToken;
  }

  @Override
  protected String getDestinationCurrency() {
    return destinationCurrency;
  }

  @Override
  protected String getDestinationIdentityName() {
    return destinationIdentityName;
  }

  @Override
  public String getDestinationPrepaidManagedCardProfileId() {
    return destinationIdentityPrepaidManagedCardProfileId;
  }

  @Override
  public String getDestinationDebitManagedCardProfileId() {
    return destinationIdentityDebitManagedCardProfileId;
  }

  @Override
  public String getDestinationManagedAccountProfileId() {
    return destinationIdentityManagedAccountProfileId;
  }

  @Override
  protected IdentityType getIdentityType() {
    return IdentityType.CONSUMER;
  }
}
