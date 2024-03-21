package opc.models.admin;

import opc.models.shared.CurrencyAmount;

public class RetryAuthorisationModel {

    private String retryType;
    private String note;
    private CurrencyAmount transactionAmount;
    private CurrencyAmount cardAmount;

    public String getRetryType() {
        return retryType;
    }

    public RetryAuthorisationModel setRetryType(String retryType) {
        this.retryType = retryType;
        return this;
    }

    public String getNote() {
        return note;
    }

    public RetryAuthorisationModel setNote(String note) {
        this.note = note;
        return this;
    }

    public CurrencyAmount getTransactionAmount() {
        return transactionAmount;
    }

    public RetryAuthorisationModel setTransactionAmount(CurrencyAmount transactionAmount) {
        this.transactionAmount = transactionAmount;
        return this;
    }

    public CurrencyAmount getCardAmount() {
        return cardAmount;
    }

    public RetryAuthorisationModel setCardAmount(CurrencyAmount cardAmount) {
        this.cardAmount = cardAmount;
        return this;
    }
}
