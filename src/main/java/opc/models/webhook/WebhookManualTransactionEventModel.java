package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookManualTransactionEventModel {
    private String transactionId;
    private LinkedHashMap<String, String> targetInstrument;
    private String availableBalanceAdjustment;
    private String actualBalanceAdjustment;
    private String transactionTimestamp;

    public String getTransactionId() {
        return transactionId;
    }

    public WebhookManualTransactionEventModel setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getAvailableBalanceAdjustment() {
        return availableBalanceAdjustment;
    }

    public WebhookManualTransactionEventModel setAvailableBalanceAdjustment(String availableBalanceAdjustment) {
        this.availableBalanceAdjustment = availableBalanceAdjustment;
        return this;
    }

    public String getActualBalanceAdjustment() {
        return actualBalanceAdjustment;
    }

    public WebhookManualTransactionEventModel setActualBalanceAdjustment(String actualBalanceAdjustment) {
        this.actualBalanceAdjustment = actualBalanceAdjustment;
        return this;
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public WebhookManualTransactionEventModel setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
        return this;
    }

    public LinkedHashMap<String, String> getTargetInstrument() {
        return targetInstrument;
    }

    public WebhookManualTransactionEventModel setTargetInstrument(LinkedHashMap<String, String> targetInstrument) {
        this.targetInstrument = targetInstrument;
        return this;
    }
}


