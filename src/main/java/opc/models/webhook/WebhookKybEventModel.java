package opc.models.webhook;

public class WebhookKybEventModel {

    private String corporateEmail;
    private String corporateId;
    private String[] details;
    private String rejectionComment;
    private String status;
    private String ongoingStatus;

    public String getCorporateEmail() {
        return corporateEmail;
    }

    public WebhookKybEventModel setCorporateEmail(String corporateEmail) {
        this.corporateEmail = corporateEmail;
        return this;
    }

    public String getCorporateId() {
        return corporateId;
    }

    public WebhookKybEventModel setCorporateId(String corporateId) {
        this.corporateId = corporateId;
        return this;
    }

    public String[] getDetails() {
        return details;
    }

    public WebhookKybEventModel setDetails(String[] details) {
        this.details = details;
        return this;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public WebhookKybEventModel setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WebhookKybEventModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getOngoingStatus() {
        return ongoingStatus;
    }

    public WebhookKybEventModel setOngoingStatus(String ongoingStatus) {
        this.ongoingStatus = ongoingStatus;
        return this;
    }
}
