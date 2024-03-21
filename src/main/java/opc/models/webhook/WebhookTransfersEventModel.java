package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookTransfersEventModel {
    private String eventType;
    private String publishedTimestamp;
    private WebhookTransferDetailsModel transfer;

    public String getEventType() {
        return eventType;
    }

    public WebhookTransfersEventModel setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookTransfersEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public WebhookTransferDetailsModel getTransfer() {
        return transfer;
    }

    public WebhookTransfersEventModel setTransfer(WebhookTransferDetailsModel transfer) {
        this.transfer = transfer;
        return this;
    }
}
