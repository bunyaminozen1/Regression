package opc.models.webhook;

public class WebhookKycEventModel {
    private String consumerEmail;
    private String consumerId;
    private String[] details;
    private String kycLevel;
    private String ongoingKycLevel;
    private String ongoingStatus;
    private String rejectionComment;
    private String status;
    private String eventTimestamp;

    public String getConsumerEmail() {
        return consumerEmail;
    }

    public WebhookKycEventModel setConsumerEmail(String consumerEmail) {
        this.consumerEmail = consumerEmail;
        return this;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public WebhookKycEventModel setConsumerId(String consumerId) {
        this.consumerId = consumerId;
        return this;
    }

    public String[] getDetails() {
        return details;
    }

    public WebhookKycEventModel setDetails(String[] details) {
        this.details = details;
        return this;
    }

    public String getKycLevel() {
        return kycLevel;
    }

    public WebhookKycEventModel setKycLevel(String kycLevel) {
        this.kycLevel = kycLevel;
        return this;
    }

    public String getOngoingKycLevel() {
        return ongoingKycLevel;
    }

    public WebhookKycEventModel setOngoingKycLevel(String ongoingKycLevel) {
        this.ongoingKycLevel = ongoingKycLevel;
        return this;
    }

    public String getOngoingStatus() {
        return ongoingStatus;
    }

    public WebhookKycEventModel setOngoingStatus(String ongoingStatus) {
        this.ongoingStatus = ongoingStatus;
        return this;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public WebhookKycEventModel setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WebhookKycEventModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getEventTimestamp() {
        return eventTimestamp;
    }

    public WebhookKycEventModel setEventTimestamp(String eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
        return this;
    }
}
