package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LimitsApiContextWithCurrencyModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("PROGRAMME_ID")
    private final String programmeIdContext;
    @JsonProperty("CURRENCY")
    private final String currencyContext;

    public LimitsApiContextWithCurrencyModel(final Builder builder) {
        this.programmeIdContext = builder.programmeIdContext;
        this.currencyContext = builder.currencyContext;
    }

    public String getProgrammeIdContext() {
        return programmeIdContext;
    }

    public String getCurrencyContext() {
        return currencyContext;
    }

    public static class Builder {
        private String programmeIdContext;
        private String currencyContext;

        public Builder setProgrammeIdContext(String programmeIdContext) {
            this.programmeIdContext = programmeIdContext;
            return this;
        }

        public Builder setCurrencyContext(String currencyContext) {
            this.currencyContext = currencyContext;
            return this;
        }

        public LimitsApiContextWithCurrencyModel build() {
            return new LimitsApiContextWithCurrencyModel(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
