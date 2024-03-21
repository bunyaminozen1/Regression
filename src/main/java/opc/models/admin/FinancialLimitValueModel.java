package opc.models.admin;

public class FinancialLimitValueModel {

    private final String refCurrency;
    private final CurrencyMinMaxModel currencyMinMax;

    public FinancialLimitValueModel(final Builder builder) {
        this.refCurrency = builder.refCurrency;
        this.currencyMinMax = builder.currencyMinMax;
    }

    public String getRefCurrency() {
        return refCurrency;
    }

    public CurrencyMinMaxModel getCurrencyMinMax() {
        return currencyMinMax;
    }

    public static class Builder {
        private String refCurrency;
        private CurrencyMinMaxModel currencyMinMax;

        public Builder setRefCurrency(String refCurrency) {
            this.refCurrency = refCurrency;
            return this;
        }

        public Builder setCurrencyMinMax(CurrencyMinMaxModel currencyMinMax) {
            this.currencyMinMax = currencyMinMax;
            return this;
        }

        public FinancialLimitValueModel build() { return new FinancialLimitValueModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
