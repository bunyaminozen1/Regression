package opc.models.admin;

public class MaxAmountModel {

    private String value;

    public MaxAmountModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public MaxAmountModel setValue(String value) {
        this.value = value;
        return this;
    }
}
