package opc.models.shared;

public class SendEmailVerificationModel {
    private final String email;

    public SendEmailVerificationModel(final String email){
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
