package opc.models.innovator;

import java.util.List;

public class DepositTypeFeeModel {

    private final String type;
    private final List<FeeDetailsModel> fees;

    public DepositTypeFeeModel(final Builder builder) {
        this.type = builder.type;
        this.fees = builder.fees;
    }

    public String getType() {
        return type;
    }

    public List<FeeDetailsModel> getFees() {
        return fees;
    }

    public static class Builder {
        private String type;
        private List<FeeDetailsModel> fees;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setFees(List<FeeDetailsModel> fees) {
            this.fees = fees;
            return this;
        }

        public DepositTypeFeeModel build() { return new DepositTypeFeeModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
