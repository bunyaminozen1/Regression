package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LimitsApiLowValueModel {

    @JsonProperty("count_max")
    private final int maxCount;
    @JsonProperty("sum_max")
    private final int maxSum;
    @JsonProperty("amount_max")
    private final int maxAmount;

    public LimitsApiLowValueModel(final Builder builder) {
        this.maxCount = builder.maxCount;
        this.maxSum = builder.maxSum;
        this.maxAmount = builder.maxAmount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getMaxSum() {
        return maxSum;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public static class Builder {
        private int maxCount;
        private int maxSum;
        private int maxAmount;

        public Builder setMaxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder setMaxSum(int maxSum) {
            this.maxSum = maxSum;
            return this;
        }

        public Builder setMaxAmount(int maxAmount) {
            this.maxAmount = maxAmount;
            return this;
        }

        public LimitsApiLowValueModel build() {
            return new LimitsApiLowValueModel(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
