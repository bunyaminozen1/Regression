package opc.models.admin;

import opc.models.innovator.FeeDetailsModel;

import java.util.List;

public class FeesOWTModel {
    private String type;
    private List<FeeDetailsModel> fees;
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<FeeDetailsModel> getFees() {
        return fees;
    }

    public void setFees(List<FeeDetailsModel> fees) {
        this.fees = fees;
    }

}
