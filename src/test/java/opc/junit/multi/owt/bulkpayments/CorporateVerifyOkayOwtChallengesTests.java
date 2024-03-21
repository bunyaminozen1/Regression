package opc.junit.multi.owt.bulkpayments;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class CorporateVerifyOkayOwtChallengesTests extends AbstractVerifyOkayOwtChallengesTests {
  private String identityToken;
  private String currency;
  private String identityManagedAccountProfileId;

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(passcodeAppCorporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, passcodeAppSecretKey);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();
    String identityId = authenticatedCorporate.getLeft();

    identityManagedAccountProfileId = passcodeAppCorporateManagedAccountProfileId;

    SecureHelper.enrolAndVerifyBiometric(identityId, passcodeAppSharedKey, passcodeAppSecretKey, identityToken);
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
