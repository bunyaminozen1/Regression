package opc.models.admin;

import opc.models.shared.CurrencyAmount;

import java.util.List;

public class SetLimitModel {
    private final String limitType;
    private final CurrencyAmount baseLimit;
    private final List<CurrencyAmount> currencyLimit;

    public SetLimitModel(final Builder builder) {
        this.limitType = builder.limitType;
        this.baseLimit = builder.baseLimit;
        this.currencyLimit = builder.currencyLimit;
    }

    public String getLimitType() {
        return limitType;
    }

    public CurrencyAmount getBaseLimit() {
        return baseLimit;
    }

    public List<CurrencyAmount> getCurrencyLimit() {
        return currencyLimit;
    }

    public static class Builder {
        private String limitType;
        private CurrencyAmount baseLimit;
        private List<CurrencyAmount> currencyLimit;

        public Builder setLimitType(String limitType) {
            this.limitType = limitType;
            return this;
        }

        public Builder setBaseLimit(CurrencyAmount baseLimit) {
            this.baseLimit = baseLimit;
            return this;
        }

        public Builder setCurrencyLimit(List<CurrencyAmount> currencyLimit) {
            this.currencyLimit = currencyLimit;
            return this;
        }

        public SetLimitModel build() { return new SetLimitModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
