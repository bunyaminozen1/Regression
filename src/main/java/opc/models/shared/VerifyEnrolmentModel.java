package opc.models.shared;

public class VerifyEnrolmentModel {

    private String oneTimePassword;

    public VerifyEnrolmentModel(final String oneTimePassword) {
        this.oneTimePassword = oneTimePassword;
    }

    public String getOneTimePassword() {
        return oneTimePassword;
    }

    public VerifyEnrolmentModel setOneTimePassword(final String oneTimePassword) {
        this.oneTimePassword = oneTimePassword;
        return this;
    }
}
