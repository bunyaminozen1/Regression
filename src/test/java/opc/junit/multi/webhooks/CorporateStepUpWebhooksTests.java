package opc.junit.multi.webhooks;

import opc.junit.helpers.multi.CorporatesHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class CorporateStepUpWebhooksTests extends AbstractStepUpWebhooksTests{
  private String identityId;
  private String identityToken;

  @BeforeEach
  public void BeforeEach() {
    final Pair<String, String> corporate = CorporatesHelper.createEnrolledCorporate(
        passcodeAppCorporateProfileId, passcodeAppSecretKey);
    identityToken = corporate.getRight();
    identityId = corporate.getLeft();
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
    return "corporates";
  }
}
