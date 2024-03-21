package opc.models.multi.users;

import opc.models.shared.PasswordModel;

public class ConsumerUserInviteModel {
    private String inviteCode;
    private PasswordModel password;

    public ConsumerUserInviteModel(final String inviteCode, final PasswordModel password) {
        this.inviteCode = inviteCode;
        this.password = password;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public PasswordModel getPassword() {
        return password;
    }

    public ConsumerUserInviteModel setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
        return this;
    }

    public ConsumerUserInviteModel setPassword(PasswordModel password) {
        this.password = password;
        return this;
    }
}