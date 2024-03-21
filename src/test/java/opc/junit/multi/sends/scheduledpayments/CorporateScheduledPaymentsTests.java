package opc.junit.multi.sends.scheduledpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class CorporateScheduledPaymentsTests extends AbstractScheduledPaymentsTests {
  private String identityToken;
  private String currency;
  private String identityPrepaidManagedCardProfileId;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityToken;
  private static String destinationCurrency;
  private static String destinationIdentityName;

  @BeforeAll
  public static void BeforeAll() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, secretKeyScaSendsApp);
    destinationIdentityToken = authenticatedCorporate.getRight();
    destinationCurrency = corporateDetails.getBaseCurrency();
    destinationIdentityName = corporateDetails.getCompany().getName();
  }

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledAllFactorsVerifiedCorporate(
        corporateDetails, secretKeyScaSendsApp, sharedKeyScaSendsApp);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();

    identityPrepaidManagedCardProfileId = corporatePrepaidManagedCardsProfileIdScaSendsApp;
    identityManagedAccountProfileId = corporateManagedAccountProfileIdScaSendsApp;
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
  protected IdentityType getIdentityType() {
    return IdentityType.CORPORATE;
  }
}
