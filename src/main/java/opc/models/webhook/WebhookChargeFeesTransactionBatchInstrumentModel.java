package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookChargeFeesTransactionBatchInstrumentModel {
    public String id;
    public LinkedHashMap<String, String> instrumentId;
    public WebhookChargeFeesInstrumentDetailsModel instrument;
    public String state;
    public String conflict;
    public LinkedHashMap<String, String> after;
    public String executedTimestamp;
    public WebhookChargeFeesInstrumentRemainingModel remaining;
    public LinkedHashMap<String, String> afterCompensation;
    public String compensatedTimestamp;
    public Boolean treasuryPending;

    public String getId() {
        return id;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setId(String id) {
        this.id = id;
        return this;
    }

    public LinkedHashMap<String, String> getInstrumentId() {
        return instrumentId;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setInstrumentId(LinkedHashMap<String, String> instrumentId) {
        this.instrumentId = instrumentId;
        return this;
    }

    public WebhookChargeFeesInstrumentDetailsModel getInstrument() {
        return instrument;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setInstrument(WebhookChargeFeesInstrumentDetailsModel instrument) {
        this.instrument = instrument;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getConflict() {
        return conflict;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setConflict(String conflict) {
        this.conflict = conflict;
        return this;
    }

    public LinkedHashMap<String, String> getAfter() {
        return after;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setAfter(LinkedHashMap<String, String> after) {
        this.after = after;
        return this;
    }

    public String getExecutedTimestamp() {
        return executedTimestamp;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setExecutedTimestamp(String executedTimestamp) {
        this.executedTimestamp = executedTimestamp;
        return this;
    }

    public WebhookChargeFeesInstrumentRemainingModel getRemaining() {
        return remaining;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setRemaining(WebhookChargeFeesInstrumentRemainingModel remaining) {
        this.remaining = remaining;
        return this;
    }

    public LinkedHashMap<String, String> getAfterCompensation() {
        return afterCompensation;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setAfterCompensation(LinkedHashMap<String, String> afterCompensation) {
        this.afterCompensation = afterCompensation;
        return this;
    }

    public String getCompensatedTimestamp() {
        return compensatedTimestamp;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setCompensatedTimestamp(String compensatedTimestamp) {
        this.compensatedTimestamp = compensatedTimestamp;
        return this;
    }

    public Boolean getTreasuryPending() {
        return treasuryPending;
    }

    public WebhookChargeFeesTransactionBatchInstrumentModel setTreasuryPending(Boolean treasuryPending) {
        this.treasuryPending = treasuryPending;
        return this;
    }
}
