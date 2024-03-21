package opc.models.webhook;

public class WebhookIdentityActivationModel {
    private String actionDoneBy;
    private String emailAddress;

    public String getActionDoneBy() {
        return actionDoneBy;
    }

    public WebhookIdentityActivationModel setActionDoneBy(String actionDoneBy) {
        this.actionDoneBy = actionDoneBy;
        return this;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public WebhookIdentityActivationModel setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }
}
