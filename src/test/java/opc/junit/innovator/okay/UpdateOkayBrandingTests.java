package opc.junit.innovator.okay;


import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import opc.models.innovator.UpdateOkayBrandingModel;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@Execution(ExecutionMode.CONCURRENT)
public class UpdateOkayBrandingTests extends BaseOkaySetup {

  private final String DEFAULT_FONT_FAMILY = "Inter";
  private final String DEFAULT_TEXT_COLOUR = "#091445";
  private final String DEFAULT_BUTTON_CONFIRM_COLOUR = "#0086FF";
  private final String DEFAULT_TEXT_CONFIRM_COLOUR = "#FFFFFF";
  private final String DEFAULT_BUTTON_DECLINE_COLOUR = "#FFFFFF";
  private final String DEFAULT_TEXT_DECLINE_COLOUR = "#0086FF";
  private final String DEFAULT_BACKGROUND_COLOUR = "#FFFFFF";

  @Test
  public void updateBranding_Success() {
    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .fontFaceFamily("Arial")
        .textColor("#252D27")
        .confirmButtonColor("#33FF6E")
        .confirmTextColor("#12345D")
        .declineButtonColor("#D31347")
        .declineTextColor("#252D27")
        .backgroundColor("#DAFF33").build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, innovatorToken, programmeId)
        .then()
        .statusCode(SC_OK)
        .body("fontFaceFamily", equalTo(updateOkayBrandingModel.getFontFaceFamily()))
        .body("textColor", equalTo(updateOkayBrandingModel.getTextColor()))
        .body("confirmButtonColor", equalTo(updateOkayBrandingModel.getConfirmButtonColor()))
        .body("confirmTextColor", equalTo(updateOkayBrandingModel.getConfirmTextColor()))
        .body("declineButtonColor", equalTo(updateOkayBrandingModel.getDeclineButtonColor()))
        .body("declineTextColor", equalTo(updateOkayBrandingModel.getDeclineTextColor()))
        .body("backgroundColor", equalTo(updateOkayBrandingModel.getBackgroundColor()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "\t", "\n"})
  public void updateBrandingWithAllTypesOfBlankStrings_DefaultValuesShouldBeReturned_Success(
      final String colorCode) {
    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .fontFaceFamily(colorCode)
        .textColor(colorCode)
        .confirmButtonColor(colorCode)
        .confirmTextColor(colorCode)
        .declineButtonColor(colorCode)
        .declineTextColor(colorCode)
        .backgroundColor(colorCode).build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, innovatorToken, programmeId)
        .then()
        .statusCode(SC_OK)
        .body("fontFaceFamily", equalTo(DEFAULT_FONT_FAMILY))
        .body("textColor", equalTo(DEFAULT_TEXT_COLOUR))
        .body("confirmButtonColor", equalTo(DEFAULT_BUTTON_CONFIRM_COLOUR))
        .body("confirmTextColor", equalTo(DEFAULT_TEXT_CONFIRM_COLOUR))
        .body("declineButtonColor", equalTo(DEFAULT_BUTTON_DECLINE_COLOUR))
        .body("declineTextColor", equalTo(DEFAULT_TEXT_DECLINE_COLOUR))
        .body("backgroundColor", equalTo(DEFAULT_BACKGROUND_COLOUR));
  }

  @ParameterizedTest
  @ValueSource(ints = {255})
  public void updateBrandingWithStringLongerThan255_BadRequest(int StringLength) {
    String colorCode = RandomStringUtils.randomAlphabetic(StringLength);
    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .fontFaceFamily(colorCode)
        .textColor(colorCode)
        .confirmButtonColor(colorCode)
        .confirmTextColor(colorCode)
        .declineButtonColor(colorCode)
        .declineTextColor(colorCode)
        .backgroundColor(colorCode).build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, innovatorToken, programmeId)
        .then()
        .statusCode(SC_CONFLICT);
  }

  @Test
  public void updateBrandingWithInvalidProgrammeIDFormat_BadRequest() {
    String invalidProgrammeID = RandomStringUtils.randomAlphabetic(4);
    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .backgroundColor("#DAFF33")
        .confirmButtonColor("#33FF6E")
        .textColor("#252D27")
        .declineButtonColor("#D31347").build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, innovatorToken, invalidProgrammeID)
        .then()
        .statusCode(SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void updateBrandingWithInvalidProgrammeID_BadRequest() {
    String invalidProgrammeID = RandomStringUtils.randomNumeric(18);
    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .backgroundColor("#DAFF33")
        .confirmButtonColor("#33FF6E")
        .textColor("#252D27")
        .declineButtonColor("#D31347").build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, innovatorToken, invalidProgrammeID)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
  }

  @Test
  public void updateBrandingWithUnauthorizedInnovatorToken_BadRequest() {
    String wrongInnovatorToken = RandomStringUtils.randomNumeric(18);
    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .backgroundColor("#DAFF33")
        .confirmButtonColor("#33FF6E")
        .textColor("#252D27")
        .declineButtonColor("#D31347").build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, wrongInnovatorToken, programmeId)
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}
