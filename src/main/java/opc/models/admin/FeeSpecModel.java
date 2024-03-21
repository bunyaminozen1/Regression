package opc.models.admin;

import opc.models.shared.CurrencyAmount;
import opc.models.shared.PercentageAmount;

import java.util.List;

public class FeeSpecModel {

    private final String type;
    private final List<CurrencyAmount> flatAmount;
    private final PercentageAmount percentage;

    public FeeSpecModel(final Builder builder) {
        this.type = builder.type;
        this.flatAmount = builder.flatAmount;
        this.percentage = builder.percentage;
    }

    public String getType() {
        return type;
    }

    public List<CurrencyAmount> getFlatAmount() {
        return flatAmount;
    }

    public PercentageAmount getPercentage() {
        return percentage;
    }

    public static class Builder {
        private String type;
        private List<CurrencyAmount> flatAmount;
        private PercentageAmount percentage;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setFlatAmount(List<CurrencyAmount> flatAmount) {
            this.flatAmount = flatAmount;
            return this;
        }

        public Builder setPercentage(PercentageAmount percentage) {
            this.percentage = percentage;
            return this;
        }

        public FeeSpecModel build() { return new FeeSpecModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FeeSpecModel defaultFeeSpecModel(final List<CurrencyAmount> currencyAmount) {
        return new Builder()
                .setType("FLAT")
                .setFlatAmount(currencyAmount)
                .build();
    }
}
