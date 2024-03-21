package opc.models.innovator;

public class PercentageModel {

    private int value;
    private int scale;

    public int getValue() {
        return value;
    }

    public PercentageModel setValue(int value) {
        this.value = value;
        return this;
    }

    public int getScale() {
        return scale;
    }

    public PercentageModel setScale(int scale) {
        this.scale = scale;
        return this;
    }
}
