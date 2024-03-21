package opc.models.admin;

import java.util.List;

public class LimitsApiLowValueExemptionModel {

    private final LimitsApiContextWithCurrencyModel context;
    private final LimitsApiLowValueModel value;

    public LimitsApiLowValueExemptionModel(final Builder builder) {
        this.context = builder.context;
        this.value = builder.value;
    }

    public LimitsApiContextWithCurrencyModel getContext() {
        return context;
    }

    public LimitsApiLowValueModel getValue() {
        return value;
    }

    public static class Builder {
        private LimitsApiContextWithCurrencyModel context;
        private LimitsApiLowValueModel value;

        public Builder setContext(LimitsApiContextWithCurrencyModel context) {
            this.context = context;
            return this;
        }

        public Builder setValue(LimitsApiLowValueModel value) {
            this.value = value;
            return this;
        }

        public LimitsApiLowValueExemptionModel build() {
            return new LimitsApiLowValueExemptionModel(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
