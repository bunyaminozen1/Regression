package opc.junit.multi.owt.bulkpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class CorporateVerifyOtpOwtChallengesTests extends AbstractVerifyOtpOwtChallengesTests {
  private String identityToken;
  private String currency;
  private String identityManagedAccountProfileId;
  private String destinationIdentityName;

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(passcodeAppCorporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, passcodeAppSecretKey);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();

    identityManagedAccountProfileId = passcodeAppCorporateManagedAccountProfileId;

    destinationIdentityName = corporateDetails.getCompany().getName();
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


  @Override
  protected String getDestinationIdentityName() {
    return destinationIdentityName;
  }

  @Override
  protected IdentityType getIdentityType() {
    return IdentityType.CORPORATE;
  }
}
