package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookAuthenticationFactorsEventModel {

    private LinkedHashMap<String, String> credentialId;
    private String publishedTimestamp;
    private String status;
    private String type;

    public LinkedHashMap<String, String> getCredentialId() {
        return credentialId;
    }

    public WebhookAuthenticationFactorsEventModel setCredentialId(LinkedHashMap<String, String> credentialId) {
        this.credentialId = credentialId;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookAuthenticationFactorsEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WebhookAuthenticationFactorsEventModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getType() {
        return type;
    }

    public WebhookAuthenticationFactorsEventModel setType(String type) {
        this.type = type;
        return this;
    }
}
