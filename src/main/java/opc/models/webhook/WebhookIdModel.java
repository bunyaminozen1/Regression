package opc.models.webhook;

public class WebhookIdModel {
    private String id;
    private String type;

    public WebhookIdModel() {
    }

    public WebhookIdModel(final String id, final String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
