package opc.models.innovator;

public class DeactivateIdentityModel {

    private boolean sendWebhookNotification;
    private String reasonCode;

    public DeactivateIdentityModel(final boolean sendWebhookNotification, final String reasonCode) {
        this.sendWebhookNotification = sendWebhookNotification;
        this.reasonCode = reasonCode;
    }

    public boolean getSendWebhookNotification() {
        return sendWebhookNotification;
    }

    public DeactivateIdentityModel setSendWebhookNotification(boolean sendWebhookNotification) {
        this.sendWebhookNotification = sendWebhookNotification;
        return this;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public DeactivateIdentityModel setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
        return this;
    }
}
