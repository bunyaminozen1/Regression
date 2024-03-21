package opc.models.admin;

public class WindowWidthModel {

    private int width;
    private String unit;

    public WindowWidthModel(int width, String unit) {
        this.width = width;
        this.unit = unit;
    }

    public int getWidth() {
        return width;
    }

    public WindowWidthModel setWidth(int width) {
        this.width = width;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public WindowWidthModel setUnit(String unit) {
        this.unit = unit;
        return this;
    }
}
