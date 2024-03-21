package opc.junit.multi.owt.beneficiaries;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class CorporateOutgoingWireTransferBeneficiaryExemptionTests extends AbstractOutgoingWireTransferBeneficiaryExemptionTests {
  private String identityToken;
  private String identityId;
  private String currency;
  private String identityPrepaidManagedCardProfileId;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityToken;
  private static String destinationCurrency;
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
        CreateCorporateModel.EurCurrencyCreateCorporateModel(passcodeAppCorporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, passcodeAppSecretKey);
    destinationIdentityToken = authenticatedCorporate.getRight();
    destinationCurrency = corporateDetails.getBaseCurrency();
    destinationIdentityName = corporateDetails.getCompany().getName();

    destinationIdentityPrepaidManagedCardProfileId = passcodeAppCorporatePrepaidManagedCardsProfileId;
    destinationIdentityManagedAccountProfileId = passcodeAppCorporateManagedAccountProfileId;
  }

  private void sourceSetup() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(passcodeAppCorporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, passcodeAppSecretKey);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();
    identityId = authenticatedCorporate.getLeft();

    identityPrepaidManagedCardProfileId = passcodeAppCorporatePrepaidManagedCardsProfileId;
    identityManagedAccountProfileId = passcodeAppCorporateManagedAccountProfileId;
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
  public String getDestinationPrepaidManagedCardProfileId() {
    return destinationIdentityPrepaidManagedCardProfileId;
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

