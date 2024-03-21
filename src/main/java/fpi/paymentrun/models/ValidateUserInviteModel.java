package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ValidateUserInviteModel {

    private String inviteCode;

    public static ValidateUserInviteModel validateModel(final String inviteCode) {
        return ValidateUserInviteModel.builder().inviteCode(inviteCode).build();
    }
}