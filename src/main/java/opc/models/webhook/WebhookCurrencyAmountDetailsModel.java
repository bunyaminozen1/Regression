package opc.models.webhook;

public class WebhookCurrencyAmountDetailsModel {

    private String currency;
    private String amount;

    public String getCurrency() {
        return currency;
    }

    public WebhookCurrencyAmountDetailsModel setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getAmount() {
        return amount;
    }

    public WebhookCurrencyAmountDetailsModel setAmount(String amount) {
        this.amount = amount;
        return this;
    }
}
