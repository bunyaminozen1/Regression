package opc.models.secure;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginBiometricModel {

    private String deviceId;

    public LoginBiometricModel(final String deviceId){
        this.deviceId = deviceId;
    }
}
