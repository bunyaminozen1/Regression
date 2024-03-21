package opc.models.admin;

public class RemainingDeltaReason {

    private String maxCount;
    private String maxSum;
    private RemainingDeltaIntervalModel interval;

    public String getMaxCount() {
        return maxCount;
    }

    public RemainingDeltaReason setMaxCount(String maxCount) {
        this.maxCount = maxCount;
        return this;
    }

    public String getMaxSum() {
        return maxSum;
    }

    public RemainingDeltaReason setMaxSum(String maxSum) {
        this.maxSum = maxSum;
        return this;
    }

    public RemainingDeltaIntervalModel getInterval() {
        return interval;
    }

    public RemainingDeltaReason setInterval(RemainingDeltaIntervalModel interval) {
        this.interval = interval;
        return this;
    }
}
