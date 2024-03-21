package opc.models.shared;

public class EmailVerificationModel {
    private String email;
    private String verificationCode;

    public EmailVerificationModel(final String email, final String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
    }

    public String getEmail() {
        return email;
    }

    public EmailVerificationModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public EmailVerificationModel setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
        return this;
    }
}
