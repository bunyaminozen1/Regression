package opc.models.shared;

public class CurrencyAmountResponse {

    private String currency;
    private Long amount;

    public String getCurrency() {
        return currency;
    }

    public CurrencyAmountResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Long getAmount() {
        return amount;
    }

    public CurrencyAmountResponse setAmount(Long amount) {
        this.amount = amount;
        return this;
    }
}
