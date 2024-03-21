package opc.models.simulator;

public class ConversionRateModel {
    private int value;

    public ConversionRateModel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public ConversionRateModel setValue(int value) {
        this.value = value;
        return this;
    }
}
