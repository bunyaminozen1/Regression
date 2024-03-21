package opc.models.multi.passwords;

import opc.models.shared.PasswordModel;

public class UpdatePasswordModel {
    private final PasswordModel oldPassword;
    private final PasswordModel newPassword;

    public UpdatePasswordModel(PasswordModel oldPassword, PasswordModel newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public PasswordModel getOldPassword() {
        return oldPassword;
    }

    public PasswordModel getNewPassword() {
        return newPassword;
    }
}
