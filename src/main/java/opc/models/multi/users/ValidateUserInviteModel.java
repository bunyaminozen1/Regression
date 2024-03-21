package opc.models.multi.users;

public class ValidateUserInviteModel {
    private final String inviteCode;

    public ValidateUserInviteModel(final String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getInviteCode() {
        return inviteCode;
    }
}