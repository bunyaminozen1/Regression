package opc.models.secure;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrolBiometricModel {

  private final String random;
  private final String deviceId;
}
