package opc.models.shared;

public class SensitiveValueModel {
    private final String value;

    public SensitiveValueModel(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
