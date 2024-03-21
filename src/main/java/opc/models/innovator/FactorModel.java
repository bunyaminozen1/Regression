package opc.models.innovator;

public class FactorModel {

    private final String type;
    private final String providerKey;

    public FactorModel(final String type, final String providerKey) {
        this.type = type;
        this.providerKey = providerKey;
    }

    public String getType() {
        return type;
    }

    public String getProviderKey() {
        return providerKey;
    }
}
