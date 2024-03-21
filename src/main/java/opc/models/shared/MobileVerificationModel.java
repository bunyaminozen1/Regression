package opc.models.shared;

public class MobileVerificationModel {
    private final String verificationCode;

    public MobileVerificationModel(final String verificationCode){
        this.verificationCode = verificationCode;
    }

    public String getVerificationCode() {
        return verificationCode;
    }
}
