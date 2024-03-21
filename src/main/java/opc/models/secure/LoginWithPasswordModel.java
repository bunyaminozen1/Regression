package opc.models.secure;

import opc.models.shared.PasswordModel;

public class LoginWithPasswordModel {
    private String email;
    private PasswordModel password;
    private String deviceId;

    public LoginWithPasswordModel(final Builder builder) {
        this.email = builder.email;
        this.password = builder.password;
        this.deviceId = builder.deviceId;
    }

    public String getEmail() {
        return email;
    }

    public PasswordModel getPassword() {
        return password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public static class Builder {
        private String email;
        private PasswordModel password;
        private String deviceId;

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPassword(PasswordModel password) {
            this.password = password;
            return this;
        }

        public Builder setDeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public LoginWithPasswordModel build() { return new LoginWithPasswordModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
