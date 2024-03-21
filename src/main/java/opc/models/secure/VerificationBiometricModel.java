package opc.models.secure;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerificationBiometricModel {

    private final String verificationCode;
    private final String random;
}
