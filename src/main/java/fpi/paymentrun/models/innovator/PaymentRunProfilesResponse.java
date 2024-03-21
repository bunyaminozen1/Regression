package fpi.paymentrun.models.innovator;

public class PaymentRunProfilesResponse {

    private String corporateProfileId;
    private String managedAccountProfileId;
    private String owtProfileId;
    private String withdrawProfileId;
    private String linkedAccountProfileId;

    public String getCorporateProfileId() {
        return corporateProfileId;
    }

    public void setCorporateProfileId(String corporateProfileId) {
        this.corporateProfileId = corporateProfileId;
    }

    public String getManagedAccountProfileId() {
        return managedAccountProfileId;
    }

    public void setManagedAccountProfileId(String managedAccountProfileId) {
        this.managedAccountProfileId = managedAccountProfileId;
    }

    public String getOwtProfileId() {
        return owtProfileId;
    }

    public void setOwtProfileId(String owtProfileId) {
        this.owtProfileId = owtProfileId;
    }

    public String getWithdrawProfileId() {
        return withdrawProfileId;
    }

    public void setWithdrawProfileId(String withdrawProfileId) {
        this.withdrawProfileId = withdrawProfileId;
    }

    public String getLinkedAccountProfileId() {
        return linkedAccountProfileId;
    }

    public void setLinkedAccountProfileId(String linkedAccountProfileId) {
        this.linkedAccountProfileId = linkedAccountProfileId;
    }
}
