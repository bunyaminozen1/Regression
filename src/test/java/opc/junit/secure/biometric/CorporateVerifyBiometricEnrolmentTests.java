package opc.junit.secure.biometric;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;

public class CorporateVerifyBiometricEnrolmentTests extends AbstractVerifyBiometricEnrolmentTests {

  private String identityId;
  private String identityToken;
  private String identityEmail;
  final private IdentityType identityType = IdentityType.CORPORATE;

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
    identityToken = corporate.getRight();
    identityId = corporate.getLeft();
    identityEmail = createCorporateModel.getRootUser().getEmail();
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
  protected IdentityType getIdentityType() {
    return identityType;
  }

  @Override
  protected String getIdentityEmail() {return identityEmail;}

  @Override
  protected Triple<String, String, String> createNewIdentity() {
    final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);
    return Triple.of(corporate.getLeft(), corporateModel.getRootUser().getEmail(), corporate.getRight());
  }
}
