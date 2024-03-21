package opc.models.simulator;

import commons.enums.Currency;
import opc.models.shared.CurrencyAmount;
import org.apache.commons.lang3.RandomStringUtils;

public class SimulateCreateCollectionModel {

    private final String reference;
    private final CurrencyAmount amount;

    public SimulateCreateCollectionModel(final Builder builder) {
        this.reference = builder.reference;
        this.amount = builder.amount;
    }

    public String getReference() {
        return reference;
    }

    public CurrencyAmount getAmount() {
        return amount;
    }

    public static class Builder {
        private String reference;
        private CurrencyAmount amount;

        public Builder setReference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder setAmount(CurrencyAmount amount) {
            this.amount = amount;
            return this;
        }

        public SimulateCreateCollectionModel build() { return new SimulateCreateCollectionModel(this); }
    }

    public static Builder builder() { return new Builder(); }

    public static Builder createCollection(final Currency currency, final Long amount) {
        return new Builder()
                .setReference(String.format("C%s", RandomStringUtils.randomNumeric(18)))
                .setAmount(new CurrencyAmount(currency.name(), amount));
    }
}
