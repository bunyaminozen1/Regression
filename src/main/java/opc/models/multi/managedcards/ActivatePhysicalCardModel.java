package opc.models.multi.managedcards;

public class ActivatePhysicalCardModel {
    private final String activationCode;

    public ActivatePhysicalCardModel(final String activationCode) {
        this.activationCode = activationCode;
    }

    public String getActivationCode() {
        return activationCode;
    }
}
