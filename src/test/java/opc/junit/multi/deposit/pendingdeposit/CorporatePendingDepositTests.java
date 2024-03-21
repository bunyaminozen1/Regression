package opc.junit.multi.deposit.pendingdeposit;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class CorporatePendingDepositTests extends AbstractPendingDepositTests {
  private String identityToken;
  private String identityManagedAccountProfileId;

  @BeforeEach
  public void Setup() {
    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
    identityToken = authenticatedCorporate.getRight();

    identityManagedAccountProfileId = corporateManagedAccountProfileId;
  }

  @Override
  protected String getToken() {
    return identityToken;
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
