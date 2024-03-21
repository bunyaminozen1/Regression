package opc.models.webhook;

public class WebhookBeneficiaryAdditionalInformationModel {

    private WebhookBeneficiaryModel beneficiary;
    private String corporateId;
    private String corporateName;
    private String kybStatus;
    private String rootUserEmail;

    public WebhookBeneficiaryModel getBeneficiary() {
        return beneficiary;
    }

    public WebhookBeneficiaryAdditionalInformationModel setBeneficiary(WebhookBeneficiaryModel beneficiary) {
        this.beneficiary = beneficiary;
        return this;
    }

    public String getCorporateId() {
        return corporateId;
    }

    public WebhookBeneficiaryAdditionalInformationModel setCorporateId(String corporateId) {
        this.corporateId = corporateId;
        return this;
    }

    public String getCorporateName() {
        return corporateName;
    }

    public WebhookBeneficiaryAdditionalInformationModel setCorporateName(String corporateName) {
        this.corporateName = corporateName;
        return this;
    }

    public String getKybStatus() {
        return kybStatus;
    }

    public WebhookBeneficiaryAdditionalInformationModel setKybStatus(String kybStatus) {
        this.kybStatus = kybStatus;
        return this;
    }

    public String getRootUserEmail() {
        return rootUserEmail;
    }

    public WebhookBeneficiaryAdditionalInformationModel setRootUserEmail(String rootUserEmail) {
        this.rootUserEmail = rootUserEmail;
        return this;
    }
}
