package opc.models.webhook;

public class WebhookOwtEventModel {

    private String eventType;
    private String publishedTimestamp;
    private WebhookOwtDetailsModel transfer;

    public String getEventType() {
        return eventType;
    }

    public WebhookOwtEventModel setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookOwtEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public WebhookOwtDetailsModel getTransfer() {
        return transfer;
    }

    public WebhookOwtEventModel setTransfer(WebhookOwtDetailsModel transfer) {
        this.transfer = transfer;
        return this;
    }
}
