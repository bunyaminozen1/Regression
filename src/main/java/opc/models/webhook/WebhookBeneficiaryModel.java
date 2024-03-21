package opc.models.webhook;

public class WebhookBeneficiaryModel {

    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String ongoingKybStatus;
    private String status;
    private String type;

    public String getEmail() {
        return email;
    }

    public WebhookBeneficiaryModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public WebhookBeneficiaryModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public WebhookBeneficiaryModel setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getMiddleName() {
        return middleName;
    }

    public WebhookBeneficiaryModel setMiddleName(String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getOngoingKybStatus() {
        return ongoingKybStatus;
    }

    public WebhookBeneficiaryModel setOngoingKybStatus(String ongoingKybStatus) {
        this.ongoingKybStatus = ongoingKybStatus;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WebhookBeneficiaryModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getType() {
        return type;
    }

    public WebhookBeneficiaryModel setType(String type) {
        this.type = type;
        return this;
    }
}
