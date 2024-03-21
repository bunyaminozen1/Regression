package opc.models.simulator;

public class SimulateSecretExpiryModel {

    private final String credentialId;

    public SimulateSecretExpiryModel(final String credentialId) {
        this.credentialId = credentialId;
    }

    public String getCredentialId() {
        return credentialId;
    }

}
