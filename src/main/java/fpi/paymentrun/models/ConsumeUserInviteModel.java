package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.models.shared.PasswordModel;

@Builder
@Getter
@Setter
public class ConsumeUserInviteModel {

    private String inviteCode;
    private PasswordModel password;
}
