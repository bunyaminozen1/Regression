package opc.models.multi.passwords;

import opc.models.shared.PasswordModel;

public class LostPasswordResumeModel {
    private final String nonce;
    private final String email;
    private final PasswordModel newPassword;

    public LostPasswordResumeModel(final Builder builder) {
        this.nonce = builder.nonce;
        this.email = builder.email;
        this.newPassword = builder.newPassword;
    }

    public String getNonce() {
        return nonce;
    }

    public String getEmail() {
        return email;
    }

    public PasswordModel getNewPassword() {
        return newPassword;
    }

    public static class Builder {
        private String nonce;
        private String email;
        private PasswordModel newPassword;

        public Builder setNonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setNewPassword(PasswordModel newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public LostPasswordResumeModel build() {return new LostPasswordResumeModel(this);}
    }

    public static Builder newBuilder(){
        return new Builder();
    }
}