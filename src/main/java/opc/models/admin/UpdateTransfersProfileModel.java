package opc.models.admin;

public class UpdateTransfersProfileModel {
    private String allowUnsecuredTransfers;

    public UpdateTransfersProfileModel(final String allowUnsecuredTransfers) {
        this.allowUnsecuredTransfers = allowUnsecuredTransfers;
    }

    public String getAllowUnsecuredTransfers() {
        return allowUnsecuredTransfers;
    }

    public UpdateTransfersProfileModel setAllowUnsecuredTransfers(String allowUnsecuredTransfers) {
        this.allowUnsecuredTransfers = allowUnsecuredTransfers;
        return this;
    }
}
