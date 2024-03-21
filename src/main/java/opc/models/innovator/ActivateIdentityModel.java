package opc.models.innovator;

public class ActivateIdentityModel {

    private boolean sendWebhookNotification;

    public ActivateIdentityModel(final boolean sendWebhookNotification) {
        this.sendWebhookNotification = sendWebhookNotification;
    }

    public boolean getSendWebhookNotification() {
        return sendWebhookNotification;
    }

    public ActivateIdentityModel setSendWebhookNotification(boolean sendWebhookNotification) {
        this.sendWebhookNotification = sendWebhookNotification;
        return this;
    }
}
