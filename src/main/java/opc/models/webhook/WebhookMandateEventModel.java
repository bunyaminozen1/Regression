package opc.models.webhook;

public class WebhookMandateEventModel {

    private String eventType;
    private String publishedTimestamp;
    private WebhookMandateModel mandate;

    public String getEventType() {
        return eventType;
    }

    public WebhookMandateEventModel setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookMandateEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public WebhookMandateModel getMandate() {
        return mandate;
    }

    public WebhookMandateEventModel setMandate(WebhookMandateModel mandate) {
        this.mandate = mandate;
        return this;
    }
}
