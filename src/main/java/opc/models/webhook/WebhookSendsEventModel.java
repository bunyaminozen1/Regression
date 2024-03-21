package opc.models.webhook;

public class WebhookSendsEventModel {
    private String eventType;
    private String publishedTimestamp;
    private WebhookSendDetailsModel send;

    public String getEventType() {
        return eventType;
    }

    public WebhookSendsEventModel setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookSendsEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public WebhookSendDetailsModel getSend() {
        return send;
    }

    public WebhookSendsEventModel setSend(WebhookSendDetailsModel send) {
        this.send = send;
        return this;
    }
}
