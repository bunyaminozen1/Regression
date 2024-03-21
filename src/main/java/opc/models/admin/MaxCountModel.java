package opc.models.admin;

public class MaxCountModel {

    private String value;

    public MaxCountModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public MaxCountModel setValue(String value) {
        this.value = value;
        return this;
    }
}
