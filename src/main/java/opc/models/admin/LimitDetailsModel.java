package opc.models.admin;

import opc.models.shared.CurrencyAmount;

public class LimitDetailsModel {

    private final String name;
    private final String counterValue;
    private final CurrencyAmount limit;

    public LimitDetailsModel(final Builder builder) {
        this.name = builder.name;
        this.counterValue = builder.counterValue;
        this.limit = builder.limit;
    }

    public String getName() {
        return name;
    }

    public String getCounterValue() {
        return counterValue;
    }

    public CurrencyAmount getLimit() {
        return limit;
    }

    public static class Builder {
        private String name;
        private String counterValue;
        private CurrencyAmount limit;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setCounterValue(String counterValue) {
            this.counterValue = counterValue;
            return this;
        }

        public Builder setLimit(CurrencyAmount limit) {
            this.limit = limit;
            return this;
        }

        public LimitDetailsModel build() { return new LimitDetailsModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
