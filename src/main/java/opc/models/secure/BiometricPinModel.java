package opc.models.secure;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.PasswordModel;

@Data
@Builder
public class BiometricPinModel {

  private String passcode;

  public BiometricPinModel (final String passcode) {
    this.passcode = passcode;
  }
}
