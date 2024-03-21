package opc.models.innovator;

public class PasswordConfigModel {

    private int minimumLength;
    private int maximumLength;
    private int complexity;

    public int getMinimumLength() {
        return minimumLength;
    }

    public PasswordConfigModel setMinimumLength(int minimumLength) {
        this.minimumLength = minimumLength;
        return this;
    }

    public int getMaximumLength() {
        return maximumLength;
    }

    public PasswordConfigModel setMaximumLength(int maximumLength) {
        this.maximumLength = maximumLength;
        return this;
    }

    public int getComplexity() {
        return complexity;
    }

    public PasswordConfigModel setComplexity(int complexity) {
        this.complexity = complexity;
        return this;
    }
}
