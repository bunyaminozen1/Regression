package opc.models.multi.passwords;

public class LostPasswordValidateModel {
    private final String nonce;
    private final String email;

    public LostPasswordValidateModel(final Builder builder) {
        this.nonce = builder.nonce;
        this.email = builder.email;
    }

    public String getNonce() {
        return nonce;
    }

    public String getEmail() {
        return email;
    }

    public static class Builder {
        private String nonce;
        private String email;

        public Builder setNonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public LostPasswordValidateModel build() {return new LostPasswordValidateModel(this);}
    }

    public static Builder newBuilder(){
        return new Builder();
    }
}