package opc.models.innovator;

import java.util.List;

public class FeeModel {

    private String feeKey;
    private List<FeeDetailsModel> fee;

    public String getFeeKey() {
        return feeKey;
    }

    public FeeModel setFeeKey(String feeKey) {
        this.feeKey = feeKey;
        return this;
    }

    public List<FeeDetailsModel> getFee() {
        return fee;
    }

    public FeeModel setFee(List<FeeDetailsModel> fee) {
        this.fee = fee;
        return this;
    }
}
