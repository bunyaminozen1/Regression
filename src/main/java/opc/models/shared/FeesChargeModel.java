package opc.models.shared;

public class FeesChargeModel {

    private final String feeType;
    private final FeeSourceModel source;

    public FeesChargeModel(final String feeType, final FeeSourceModel source) {
        this.feeType = feeType;
        this.source = source;
    }

    public String getFeeType() {
        return feeType;
    }

    public FeeSourceModel getSource() {
        return source;
    }
}
