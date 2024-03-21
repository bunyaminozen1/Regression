package opc.junit.multi.owt.scheduledpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class CorporateScheduledPaymentsTests extends AbstractScheduledPaymentsTests {
  private String identityToken;
  private String identityId;
  private String currency;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityName;

  @BeforeAll
  public static void DestinationSetup() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(passcodeAppCorporateProfileId).build();

    destinationIdentityName = corporateDetails.getCompany().getName();
  }

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(passcodeAppCorporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledAllFactorsVerifiedCorporate(
        corporateDetails, passcodeAppSecretKey, passcodeAppSharedKey);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();
    identityId = authenticatedCorporate.getLeft();

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