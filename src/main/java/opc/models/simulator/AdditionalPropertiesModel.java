package opc.models.simulator;

public class AdditionalPropertiesModel {
    private String value;
    private String field;

    public AdditionalPropertiesModel(final String value, final String field) {
        this.value = value;
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public AdditionalPropertiesModel setValue(String value) {
        this.value = value;
        return this;
    }

    public String getField() {
        return field;
    }

    public AdditionalPropertiesModel setField(String field) {
        this.field = field;
        return this;
    }
}
