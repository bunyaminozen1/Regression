package opc.models.admin;

public class CurrencyAmountLimitModel {

    private String currency;
    private String amount;

    public CurrencyAmountLimitModel(String currency, String amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public CurrencyAmountLimitModel setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getAmount() {
        return amount;
    }

    public CurrencyAmountLimitModel setAmount(String amount) {
        this.amount = amount;
        return this;
    }
}
