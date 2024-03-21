package opc.models;

import opc.models.shared.PasswordModel;

public class PasswordValidationModel {

    private final PasswordModel password;

    public PasswordValidationModel(final PasswordModel password){
        this.password = password;
    }

    public PasswordModel getPassword() {
        return password;
    }
}
