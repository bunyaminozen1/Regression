package opc.models.shared;

public class PercentageAmount {

    private long value;
    private int scale;

    public PercentageAmount(long value, int scale) {
        this.value = value;
        this.scale = scale;
    }

    public long getValue() {
        return value;
    }

    public PercentageAmount setValue(long value) {
        this.value = value;
        return this;
    }

    public int getScale() {
        return scale;
    }

    public PercentageAmount setScale(int scale) {
        this.scale = scale;
        return this;
    }
}
