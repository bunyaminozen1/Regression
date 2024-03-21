package opc.junit.multi.sends.bulkpayments;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class CorporateIssuePushSendChallengesTests extends AbstractIssuePushSendChallengesTests {
  private String identityToken;
  private String identityId;
  private String currency;
  private String identityPrepaidManagedCardProfileId;
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
    identityId = authenticatedCorporate.getLeft();

    identityPrepaidManagedCardProfileId = corporatePrepaidManagedCardsProfileIdScaSendsApp;
    identityManagedAccountProfileId = corporateManagedAccountProfileIdScaSendsApp;
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
}
