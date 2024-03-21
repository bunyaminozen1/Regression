package opc.models.webhook;

public class WebhookKybBeneficiaryEventModel {

    private WebhookBeneficiaryAdditionalInformationModel additionalInformation;
    private String[] event;
    private String[] eventDetails;
    private String rejectionComment;

    public WebhookBeneficiaryAdditionalInformationModel getAdditionalInformation() {
        return additionalInformation;
    }

    public WebhookKybBeneficiaryEventModel setAdditionalInformation(WebhookBeneficiaryAdditionalInformationModel additionalInformation) {
        this.additionalInformation = additionalInformation;
        return this;
    }

    public String[] getEvent() {
        return event;
    }

    public WebhookKybBeneficiaryEventModel setEvent(String[] event) {
        this.event = event;
        return this;
    }

    public String[] getEventDetails() {
        return eventDetails;
    }

    public WebhookKybBeneficiaryEventModel setEventDetails(String[] eventDetails) {
        this.eventDetails = eventDetails;
        return this;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public WebhookKybBeneficiaryEventModel setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
        return this;
    }
}
