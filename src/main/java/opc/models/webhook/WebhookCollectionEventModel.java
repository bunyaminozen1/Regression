package opc.models.webhook;

public class WebhookCollectionEventModel {

    private String eventType;
    private String publishedTimestamp;
    private WebhookCollectionModel collection;

    public String getEventType() {
        return eventType;
    }

    public WebhookCollectionEventModel setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookCollectionEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public WebhookCollectionModel getCollection() {
        return collection;
    }

    public WebhookCollectionEventModel setCollection(WebhookCollectionModel collection) {
        this.collection = collection;
        return this;
    }
}
