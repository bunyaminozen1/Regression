package opc.models.webhook;

public class WebhookManagedAccountEventModel {
    private String type;
    private String publishedTimestamp;
    private WebhookManagedAccountModel account;

    public WebhookManagedAccountEventModel() {
    }

    public WebhookManagedAccountEventModel(final String type, final String publishedTimestamp, final WebhookManagedAccountModel account) {
        this.type = type;
        this.publishedTimestamp = publishedTimestamp;
        this.account = account;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public void setPublishedTimestamp(final String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
    }

    public WebhookManagedAccountModel getAccount() {
        return account;
    }

    public void setAccount(final WebhookManagedAccountModel account) {
        this.account = account;
    }
}
