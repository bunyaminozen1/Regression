package opc.junit.innovator.okay;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

import opc.models.innovator.UpdateOkayBrandingModel;
import opc.services.innovator.InnovatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class GetOkayBrandingTests extends BaseOkaySetup {

  @Test
  public void getBranding_Success() {

    final UpdateOkayBrandingModel updateOkayBrandingModel = UpdateOkayBrandingModel
        .builder()
        .fontFaceFamily("Arial")
        .textColor("#252D27")
        .confirmButtonColor("#33FF6E")
        .confirmTextColor("#12345D")
        .declineButtonColor("#D31347")
        .declineTextColor("#1245DE")
        .backgroundColor("#DAFF33").build();

    InnovatorService.updateOkayBranding(updateOkayBrandingModel, innovatorToken, programmeId)
        .then()
        .statusCode(SC_OK);

    InnovatorService.getOkayBranding(innovatorToken, programmeId)
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
}
