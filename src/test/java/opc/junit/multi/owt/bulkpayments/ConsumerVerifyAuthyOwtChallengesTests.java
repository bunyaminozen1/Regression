package opc.junit.multi.owt.bulkpayments;

import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerVerifyAuthyOwtChallengesTests extends AbstractVerifyAuthyOwtChallengesTests {
  private String identityToken;
  private String currency;
  private String identityManagedAccountProfileId;

  @BeforeEach
  public void SourceSetup() {
    final CreateConsumerModel consumerDetails =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(passcodeAppConsumerProfileId).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
        consumerDetails, passcodeAppSecretKey);
    currency = consumerDetails.getBaseCurrency();
    identityToken = authenticatedConsumer.getRight();
    String identityId = authenticatedConsumer.getLeft();

    identityManagedAccountProfileId = passcodeAppConsumerManagedAccountProfileId;

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(identityId, passcodeAppSecretKey, identityToken);
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
  public String getManagedAccountProfileId() {
    return identityManagedAccountProfileId;
  }
}
