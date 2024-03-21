package opc.models.admin;

public class LimitsApiValueModel {

    private final int maxCount;
    private final int maxSum;
    private final LimitsApiIntervalModel interval;

    public LimitsApiValueModel(final Builder builder) {
        this.maxCount = builder.maxCount;
        this.maxSum = builder.maxSum;
        this.interval = builder.interval;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getMaxSum() {
        return maxSum;
    }

    public LimitsApiIntervalModel getInterval() {
        return interval;
    }

    public static class Builder {
        private int maxCount;
        private int maxSum;
        private LimitsApiIntervalModel interval;

        public Builder setMaxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder setMaxSum(int maxSum) {
            this.maxSum = maxSum;
            return this;
        }

        public Builder setInterval(LimitsApiIntervalModel interval) {
            this.interval = interval;
            return this;
        }

        public LimitsApiValueModel build() { return new LimitsApiValueModel(this);}
    }

    public static Builder builder() {
        return new Builder();
    }
}
