package opc.models.backoffice;

import opc.enums.opc.LimitInterval;
import opc.models.shared.CurrencyAmount;

public class SpendLimitModel {
    private CurrencyAmount value;
    private String interval;

    public SpendLimitModel(final CurrencyAmount value, final LimitInterval interval) {
        this.value = value;
        this.interval = interval.name();
    }

    public CurrencyAmount getValue() {
        return value;
    }

    public SpendLimitModel setValue(CurrencyAmount value) {
        this.value = value;
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
