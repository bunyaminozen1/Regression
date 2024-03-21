package opc.models.innovator;

public class FeeDetailsModel {

    private String name;
    private FeeValuesModel fee;

    public String getName() {
        return name;
    }

    public FeeValuesModel getFee() {
        return fee;
    }

    public FeeDetailsModel setName(String name) {
        this.name = name;
        return this;
    }

    public FeeDetailsModel setFee(FeeValuesModel fee) {
        this.fee = fee;
        return this;
    }
}
