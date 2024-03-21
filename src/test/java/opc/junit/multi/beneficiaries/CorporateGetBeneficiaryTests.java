package opc.junit.multi.beneficiaries;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class CorporateGetBeneficiaryTests extends AbstractGetBeneficiaryTests {
  private static String destinationIdentityToken;
  private static String destinationCurrency;
  private String identityToken;
  private String identityId;
  private static String destinationIdentityName;
  private static String destinationIdentityPrepaidManagedCardProfileId;
  private static String destinationIdentityDebitManagedCardProfileId;
  private static String destinationIdentityManagedAccountProfileId;

  @BeforeAll
  public static void DestinationSetup() { destinationSetup(); }

  @BeforeEach
  public void SourceSetup() { sourceSetup(); }

  private static void destinationSetup() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.DefaultCreateCorporateModel(destinationCorporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, destinationSecretKey);
    destinationIdentityToken = authenticatedCorporate.getRight();
    destinationCurrency = corporateDetails.getBaseCurrency();
    destinationIdentityName = corporateDetails.getCompany().getName();

    destinationIdentityPrepaidManagedCardProfileId = destinationCorporatePrepaidManagedCardsProfileId;
    destinationIdentityDebitManagedCardProfileId = destinationCorporateDebitManagedCardsProfileId;
    destinationIdentityManagedAccountProfileId = destinationCorporateManagedAccountsProfileId;
  }

  private void sourceSetup() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, secretKey);
    identityId = authenticatedCorporate.getLeft();
    identityToken = authenticatedCorporate.getRight();
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
    return IdentityType.CORPORATE;
  }
}
