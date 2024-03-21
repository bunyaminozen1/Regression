package opc.junit.multi.sends.bulkpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class CorporateVerifyOkaySendChallengesTests extends AbstractVerifyOkaySendChallengesTests {
  private String identityToken;
  private String currency;
  private String identityManagedAccountProfileId;
  private static String destinationIdentityToken;
  private static String destinationCurrency;

  @BeforeAll
  public static void BeforeAll() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, secretKeyScaSendsApp);
    destinationIdentityToken = authenticatedCorporate.getRight();
    destinationCurrency = corporateDetails.getBaseCurrency();
  }

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel corporateDetails =
        CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
        corporateDetails, secretKeyScaSendsApp);
    currency = corporateDetails.getBaseCurrency();
    identityToken = authenticatedCorporate.getRight();
    String identityId = authenticatedCorporate.getLeft();

    identityManagedAccountProfileId = corporateManagedAccountProfileIdScaSendsApp;

    SecureHelper.enrolAndVerifyBiometric(identityId, sharedKeyScaSendsApp, secretKeyScaSendsApp, identityToken);
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
  protected String getDestinationToken() {
    return destinationIdentityToken;
  }

  @Override
  protected String getDestinationCurrency() {
    return destinationCurrency;
  }
}
