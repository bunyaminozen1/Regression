package opc.models.shared;

import commons.enums.Currency;

public class CurrencyAmount {

    private String currency;
    private Long amount;

    public CurrencyAmount(final String currency, final Long amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public CurrencyAmount(final Currency currency, final Long amount) {
        this.currency = currency.name();
        this.amount = amount;
    }

    public CurrencyAmount() {

    }

    public String getCurrency() {
        return currency;
    }

    public CurrencyAmount setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Long getAmount() {
        return amount;
    }

    public CurrencyAmount setAmount(Long amount) {
        this.amount = amount;
        return this;
    }
}
