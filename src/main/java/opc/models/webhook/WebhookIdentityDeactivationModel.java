package opc.models.webhook;

public class WebhookIdentityDeactivationModel {
    private String actionDoneBy;
    private String emailAddress;
    private String reasonCode;

    public String getActionDoneBy() {
        return actionDoneBy;
    }

    public WebhookIdentityDeactivationModel setActionDoneBy(String actionDoneBy) {
        this.actionDoneBy = actionDoneBy;
        return this;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public WebhookIdentityDeactivationModel setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public WebhookIdentityDeactivationModel setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
        return this;
    }


}
