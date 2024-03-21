package opc.junit.secure.biometric;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.innovator.UpdateOkayBrandingModel;
import opc.models.secure.EnrolBiometricModel;
import opc.services.innovator.InnovatorService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GetBiometricBrandingTests extends BaseBiometricSetup {

  /**
   * After biometric enrolment moved to Secure Gateway, get branding Api was exposed
   * to retrieve the branding for the consent screen. This Api retrieve data via innovator Api
   * and consent screen is displayed according to changes that is made with innovator portal
   */

  private static UpdateOkayBrandingModel updateBiometricBrandingModel;

  @BeforeAll
  public static void updateBranding(){
    updateBiometricBrandingModel = UpdateOkayBrandingModel
        .builder()
        .fontFaceFamily("Arial")
        .textColor("#252D27")
        .confirmButtonColor("#33FF6E")
        .confirmTextColor("#12345D")
        .declineButtonColor("#D31347")
        .declineTextColor("#1245DE")
        .backgroundColor("#DAFF33").build();

    final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    
    InnovatorService.updateOkayBranding(updateBiometricBrandingModel, innovatorToken, programmeId)
        .then()
        .statusCode(SC_OK)
        .body("fontFaceFamily", equalTo(updateBiometricBrandingModel.getFontFaceFamily()))
        .body("textColor", equalTo(updateBiometricBrandingModel.getTextColor()))
        .body("confirmButtonColor", equalTo(updateBiometricBrandingModel.getConfirmButtonColor()))
        .body("confirmTextColor", equalTo(updateBiometricBrandingModel.getConfirmTextColor()))
        .body("declineButtonColor", equalTo(updateBiometricBrandingModel.getDeclineButtonColor()))
        .body("declineTextColor", equalTo(updateBiometricBrandingModel.getDeclineTextColor()))
        .body("backgroundColor", equalTo(updateBiometricBrandingModel.getBackgroundColor()));
  }

  @Test
  public void Consumer_GetBiometricBranding_Success() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(
        consumerProfileId, secretKey);

    final EnrolBiometricModel enrolBiometricModel = enrolBiometric(consumer.getRight());

    // random that is retrieved with SecureService.associate call should be provided in the payload
    // device id is optional. We are providing that ones with enrolBiometricModel
    SecureService.getBiometricBranding(consumer.getRight(), sharedKey, enrolBiometricModel)
        .then()
        .statusCode(SC_OK)
        .body("fontFaceFamily", equalTo(updateBiometricBrandingModel.getFontFaceFamily()))
        .body("textColor", equalTo(updateBiometricBrandingModel.getTextColor()))
        .body("confirmButtonColor", equalTo(updateBiometricBrandingModel.getConfirmButtonColor()))
        .body("confirmTextColor", equalTo(updateBiometricBrandingModel.getConfirmTextColor()))
        .body("declineButtonColor", equalTo(updateBiometricBrandingModel.getDeclineButtonColor()))
        .body("declineTextColor", equalTo(updateBiometricBrandingModel.getDeclineTextColor()))
        .body("backgroundColor", equalTo(updateBiometricBrandingModel.getBackgroundColor()));
  }

  @Test
  public void Corporate_GetBiometricBranding_Success() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(
        corporateProfileId, secretKey);

    final EnrolBiometricModel enrolBiometricModel = enrolBiometric(corporate.getRight());

    // random that is retrieved with SecureService.associate call should be provided in the payload
    // device id is optional. We are providing that ones with enrolBiometricModel
    SecureService.getBiometricBranding(corporate.getRight(), sharedKey, enrolBiometricModel)
        .then()
        .statusCode(SC_OK)
        .body("fontFaceFamily", equalTo(updateBiometricBrandingModel.getFontFaceFamily()))
        .body("textColor", equalTo(updateBiometricBrandingModel.getTextColor()))
        .body("confirmButtonColor", equalTo(updateBiometricBrandingModel.getConfirmButtonColor()))
        .body("confirmTextColor", equalTo(updateBiometricBrandingModel.getConfirmTextColor()))
        .body("declineButtonColor", equalTo(updateBiometricBrandingModel.getDeclineButtonColor()))
        .body("declineTextColor", equalTo(updateBiometricBrandingModel.getDeclineTextColor()))
        .body("backgroundColor", equalTo(updateBiometricBrandingModel.getBackgroundColor()));
  }

  private EnrolBiometricModel enrolBiometric(final String authToken){
    final String random = SecureHelper.associate(authToken, sharedKey);
    final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
        .random(random)
        .deviceId(RandomStringUtils.randomAlphanumeric(40))
        .build();

    SecureService.enrolDeviceBiometric(authToken, sharedKey, enrolBiometricModel);

    return enrolBiometricModel;
  }
}