package opc.models.shared;

import opc.enums.opc.LimitInterval;

public class SpendLimitModel {
    private CurrencyAmount limitAmount;
    private String interval;

    public SpendLimitModel(final CurrencyAmount limitAmount, final LimitInterval interval) {
        this.limitAmount = limitAmount;
        this.interval = interval.name();
    }

    public CurrencyAmount getLimitAmount() {
        return limitAmount;
    }

    public SpendLimitModel setLimitAmount(CurrencyAmount limitAmount) {
        this.limitAmount = limitAmount;
        return this;
    }

    public String getInterval() {
        return interval;
    }

    public SpendLimitModel setInterval(String interval) {
        this.interval = interval;
        return this;
    }
}
