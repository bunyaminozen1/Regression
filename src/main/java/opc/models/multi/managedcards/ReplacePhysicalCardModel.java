package opc.models.multi.managedcards;

public class ReplacePhysicalCardModel {
    private final String activationCode;

    public ReplacePhysicalCardModel(final String activationCode) {
        this.activationCode = activationCode;
    }

    public String getActivationCode() {
        return activationCode;
    }
}
