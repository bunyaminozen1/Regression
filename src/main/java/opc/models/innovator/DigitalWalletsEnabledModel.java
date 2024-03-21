package opc.models.innovator;

public class DigitalWalletsEnabledModel {

    private boolean manualProvisioningEnabled;

    private boolean pushProvisioningEnabled;

    public DigitalWalletsEnabledModel(boolean manualProvisioningEnabled, boolean pushProvisioningEnabled) {
        this.manualProvisioningEnabled = manualProvisioningEnabled;
        this.pushProvisioningEnabled = pushProvisioningEnabled;
    }

    public boolean getManualProvisioningEnabled() {
        return manualProvisioningEnabled;
    }

    public void setManualProvisioningEnabled(boolean manualProvisioningEnabled) {
        this.manualProvisioningEnabled = manualProvisioningEnabled;
    }

    public boolean getPushProvisioningEnabled() {
        return pushProvisioningEnabled;
    }

    public void setPushProvisioningEnabled(boolean pushProvisioningEnabled) {
        this.pushProvisioningEnabled = pushProvisioningEnabled;
    }
}
