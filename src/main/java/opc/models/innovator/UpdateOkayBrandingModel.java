package opc.models.innovator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateOkayBrandingModel {

  private final String fontFaceFamily;
  private final String textColor;
  private final String confirmButtonColor;
  private final String confirmTextColor;
  private final String declineButtonColor;
  private final String declineTextColor;
  private final String backgroundColor;
}
