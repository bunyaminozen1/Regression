package opc.junit.multi.owt.scheduledpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerScheduledPaymentsTests extends AbstractScheduledPaymentsTests {
  private String identityToken;
  private String identityId;
  private String currency;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityName;

  @BeforeAll
  public static void DestinationSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(passcodeAppConsumerProfileId).build();

    destinationIdentityName = String.format("%s %s", consumerDetails.getRootUser().getName(),
        consumerDetails.getRootUser().getSurname());
  }

  @BeforeEach
  public void SourceSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(passcodeAppConsumerProfileId).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledAllFactorsVerifiedConsumer(
        consumerDetails, passcodeAppSecretKey, passcodeAppSharedKey);
    currency = consumerDetails.getBaseCurrency();
    identityToken = authenticatedConsumer.getRight();
    identityId = authenticatedConsumer.getLeft();

    identityManagedAccountProfileId = passcodeAppConsumerManagedAccountProfileId;
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
  protected String getDestinationIdentityName() {
    return destinationIdentityName;
  }

  @Override
  protected IdentityType getIdentityType() {
    return IdentityType.CONSUMER;
  }
}
