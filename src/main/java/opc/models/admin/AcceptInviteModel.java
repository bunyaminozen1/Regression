package opc.models.admin;

import opc.models.shared.PasswordModel;

public class AcceptInviteModel {
    private String nonce;
    private String email;
    private PasswordModel password;

    public PasswordModel getPassword() {
        return password;
    }

    public void setPassword(PasswordModel passwordModel) {
        this.password = passwordModel;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public AcceptInviteModel(String nonce, String email, PasswordModel passwordModel) {
        this.nonce = nonce;
        this.email = email;
        this.password = passwordModel;
    }
}
