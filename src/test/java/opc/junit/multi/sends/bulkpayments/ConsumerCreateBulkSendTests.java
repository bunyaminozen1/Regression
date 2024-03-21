package opc.junit.multi.sends.bulkpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerCreateBulkSendTests extends AbstractCreateBulkSendTests {
  private String identityToken;
  private String currency;
  private String identityPrepaidManagedCardProfileId;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityToken;
  private static String destinationCurrency;
  private static String destinationIdentityName;


  @BeforeAll
  public static void BeforeAll() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, secretKeyScaSendsApp);
    destinationIdentityToken = authenticatedConsumer.getRight();
    destinationCurrency = consumerDetails.getBaseCurrency();
    destinationIdentityName = String.format("%s %s", consumerDetails.getRootUser().getName(),
        consumerDetails.getRootUser().getSurname());
  }

  @BeforeEach
  public void BeforeEach() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, secretKeyScaSendsApp);
    currency = consumerDetails.getBaseCurrency();
    identityToken = authenticatedConsumer.getRight();

    identityPrepaidManagedCardProfileId = consumerPrepaidManagedCardsProfileIdScaSendsApp;
    identityManagedAccountProfileId = consumerManagedAccountProfileIdScaSendsApp;
  }

  @Override
  protected String getToken() {
    return identityToken;
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
  protected IdentityType getIdentityType() {
    return IdentityType.CONSUMER;
  }
}
