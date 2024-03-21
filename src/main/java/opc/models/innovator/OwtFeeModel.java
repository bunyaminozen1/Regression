package opc.models.innovator;

import java.util.List;

public class OwtFeeModel {
    private String type;
    private List<FeeDetailsModel> fees;

    public String getType() {
        return type;
    }

    public OwtFeeModel setType(String type) {
        this.type = type;
        return this;
    }

    public List<FeeDetailsModel> getFees() {
        return fees;
    }

    public OwtFeeModel setFees(List<FeeDetailsModel> fees) {
        this.fees = fees;
        return this;
    }
}
