package opc.junit.multi.owt.cannedibans;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class CorporateCannedOutgoingWireTransferTests extends AbstractCannedOutgoingWireTransferTests{
  private static String identityToken;
  private static String identityId;
  private static String currency;
  private static String identityManagedAccountProfileId;

  @BeforeEach
  public void SourceSetup() { sourceSetup(); }

  private static void sourceSetup() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(
            corporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, secretKey);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();
    identityId = authenticatedCorporate.getLeft();

    identityManagedAccountProfileId = corporateManagedAccountProfileId;
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
    return IdentityType.CORPORATE;
  }
}
