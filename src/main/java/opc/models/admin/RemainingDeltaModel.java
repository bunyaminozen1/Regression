package opc.models.admin;

public class RemainingDeltaModel {

    private String remainingCount;
    private String remainingSum;
    private RemainingDeltaReason reason;

    public String getRemainingCount() {
        return remainingCount;
    }

    public RemainingDeltaModel setRemainingCount(String remainingCount) {
        this.remainingCount = remainingCount;
        return this;
    }

    public String getRemainingSum() {
        return remainingSum;
    }

    public RemainingDeltaModel setRemainingSum(String remainingSum) {
        this.remainingSum = remainingSum;
        return this;
    }

    public RemainingDeltaReason getReason() {
        return reason;
    }

    public RemainingDeltaModel setReason(RemainingDeltaReason reason) {
        this.reason = reason;
        return this;
    }
}
