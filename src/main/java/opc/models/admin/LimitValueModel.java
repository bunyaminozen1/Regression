package opc.models.admin;

public class LimitValueModel {

    private MaxAmountModel maxAmount;
    private MaxCountModel maxCount;

    public LimitValueModel(MaxAmountModel maxAmount) {
        this.maxAmount = maxAmount;
    }

    public LimitValueModel(MaxCountModel maxCount) {
        this.maxCount = maxCount;
    }

    public MaxAmountModel getMaxAmount() {
        return maxAmount;
    }

    public LimitValueModel setMaxAmount(MaxAmountModel maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }

    public MaxCountModel getMaxCount() {
        return maxCount;
    }

    public LimitValueModel setMaxCount(MaxCountModel maxCount) {
        this.maxCount = maxCount;
        return this;
    }
}
