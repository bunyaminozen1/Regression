package opc.junit.multi.sends.beneficiaries;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerSendBeneficiaryExemptionTests extends AbstractSendBeneficiaryExemptionTests {
  private String identityToken;
  private String identityId;
  private String currency;
  private String identityPrepaidManagedCardProfileId;
  private String identityDebitManagedCardProfileId;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityToken;
  private static String destinationCurrency;
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
        CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, secretKeyScaSendsApp);
    destinationIdentityToken = authenticatedConsumer.getRight();
    destinationCurrency = consumerDetails.getBaseCurrency();
    destinationIdentityName = String.format("%s %s", consumerDetails.getRootUser().getName(),
        consumerDetails.getRootUser().getSurname());

    destinationIdentityPrepaidManagedCardProfileId = consumerPrepaidManagedCardsProfileIdScaSendsApp;
    destinationIdentityDebitManagedCardProfileId = consumerDebitManagedCardsProfileIdScaSendsApp;
    destinationIdentityManagedAccountProfileId = consumerManagedAccountProfileIdScaSendsApp;
  }

  private void sourceSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, secretKeyScaSendsApp);
    currency = consumerDetails.getBaseCurrency();
    identityToken = authenticatedConsumer.getRight();
    identityId = authenticatedConsumer.getLeft();

    identityPrepaidManagedCardProfileId = consumerPrepaidManagedCardsProfileIdScaSendsApp;
    identityDebitManagedCardProfileId = consumerDebitManagedCardsProfileIdScaSendsApp;
    identityManagedAccountProfileId = consumerManagedAccountProfileIdScaSendsApp;
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
  public String getPrepaidManagedCardProfileId() {
    return identityPrepaidManagedCardProfileId;
  }

  @Override
  public String getDebitManagedCardProfileId() {
    return identityDebitManagedCardProfileId;
  }

  @Override
  public String getManagedAccountProfileId() {
    return identityManagedAccountProfileId;
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
