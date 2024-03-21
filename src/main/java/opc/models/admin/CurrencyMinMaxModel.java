package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CurrencyMinMaxModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("EUR")
    private LimitValueModel eur;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("GBP")
    private LimitValueModel gbp;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("USD")
    private LimitValueModel usd;

    public CurrencyMinMaxModel(final Builder builder) {
        this.eur = builder.eur;
        this.gbp = builder.gbp;
        this.usd = builder.usd;
    }

    public LimitValueModel getEur() {
        return eur;
    }

    public CurrencyMinMaxModel setEur(LimitValueModel eur) {
        this.eur = eur;
        return this;
    }

    public LimitValueModel getGbp() {
        return gbp;
    }

    public CurrencyMinMaxModel setGbp(LimitValueModel gbp) {
        this.gbp = gbp;
        return this;
    }

    public LimitValueModel getUsd() {
        return usd;
    }

    public CurrencyMinMaxModel setUsd(LimitValueModel usd) {
        this.usd = usd;
        return this;
    }

    public static class Builder {

        private LimitValueModel eur;
        private LimitValueModel gbp;
        private LimitValueModel usd;

        public Builder setEur(LimitValueModel eur) {
            this.eur = eur;
            return this;
        }

        public Builder setGbp(LimitValueModel gbp) {
            this.gbp = gbp;
            return this;
        }

        public Builder setUsd(LimitValueModel usd) {
            this.usd = usd;
            return this;
        }

        public CurrencyMinMaxModel build() { return new CurrencyMinMaxModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
