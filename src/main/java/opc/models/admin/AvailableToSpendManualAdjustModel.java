package opc.models.admin;

public class AvailableToSpendManualAdjustModel {

    private final long adjustmentAmount;
    private final long adjustmentId;
    private final long adjustmentTimestamp;

    public AvailableToSpendManualAdjustModel(final Builder builder) {
        this.adjustmentAmount = builder.adjustmentAmount;
        this.adjustmentId = builder.adjustmentId;
        this.adjustmentTimestamp = builder.adjustmentTimestamp;
    }

    public long getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public long getAdjustmentId() {
        return adjustmentId;
    }

    public long getAdjustmentTimestamp() {
        return adjustmentTimestamp;
    }

    public static class Builder {
        private long adjustmentAmount;
        private long adjustmentId;
        private long adjustmentTimestamp;

        public void setAdjustmentAmount(long amount) {
            this.adjustmentAmount = amount;
        }

        public void setAdjustmentId(Long adjustmentId) {
            this.adjustmentId = adjustmentId;
        }

        public void setAdjustmentTimestamp(Long adjustmentTimestamp) {
            this.adjustmentTimestamp = adjustmentTimestamp;
        }

        public AvailableToSpendManualAdjustModel build() {return new AvailableToSpendManualAdjustModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
