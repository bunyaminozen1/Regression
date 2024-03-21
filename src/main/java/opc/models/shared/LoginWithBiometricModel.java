package opc.models.shared;

import lombok.Builder;
import lombok.Data;
import opc.enums.opc.IdentityType;
import opc.models.backoffice.IdentityModel;

@Data
@Builder
public class LoginWithBiometricModel {

    private String email;
    private IdentityModel identity;

    public static LoginWithBiometricModel loginWithBiometricModel(final String email){
        return LoginWithBiometricModel.builder()
                .email(email)
                .identity(null)
                .build();
    }

    public static LoginWithBiometricModel loginWithBiometricSemiModel(final String email, final String linkedUserId){
        return LoginWithBiometricModel.builder()
                .email(email)
                .identity(new IdentityModel(linkedUserId, IdentityType.CORPORATE))
                .build();
    }


}
