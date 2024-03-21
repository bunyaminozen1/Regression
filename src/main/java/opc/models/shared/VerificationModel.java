package opc.models.shared;

public class VerificationModel {
    private final String verificationCode;

    public VerificationModel(final String verificationCode){
        this.verificationCode = verificationCode;
    }

    public String getVerificationCode() {
        return verificationCode;
    }
}
