package opc.models.webhook;

import java.util.LinkedHashMap;
import java.util.List;

public class WebhookChargeFeesBatchModel {
    private String id;
    private LinkedHashMap<String, String> transaction;
    private String relatedBatchId;
    private List<LinkedHashMap<String, String>> intercept;
    private LinkedHashMap<String, WebhookChargeFeesTransactionBatchInstrumentModel> instrument;
    private List<LinkedHashMap<String, String>> forexTrade;
    private String completedTimestamp;
    private String state;
    private String conflict;
    private String authSessionId;
    private String note;


    public String getId() {
        return id;
    }

    public WebhookChargeFeesBatchModel setId(String id) {
        this.id = id;
        return this;
    }

    public LinkedHashMap<String, String> getTransaction() {
        return transaction;
    }

    public WebhookChargeFeesBatchModel setTransaction(LinkedHashMap<String, String> transaction) {
        this.transaction = transaction;
        return this;
    }

    public String getRelatedBatchId() {
        return relatedBatchId;
    }

    public WebhookChargeFeesBatchModel setRelatedBatchId(String relatedBatchId) {
        this.relatedBatchId = relatedBatchId;
        return this;
    }

    public List<LinkedHashMap<String, String>> getIntercept() {
        return intercept;
    }

    public WebhookChargeFeesBatchModel setIntercept(List<LinkedHashMap<String, String>> intercept) {
        this.intercept = intercept;
        return this;
    }

    public LinkedHashMap<String, WebhookChargeFeesTransactionBatchInstrumentModel> getInstrument() {
        return instrument;
    }

    public WebhookChargeFeesBatchModel setInstrument(LinkedHashMap<String, WebhookChargeFeesTransactionBatchInstrumentModel> instrument) {
        this.instrument = instrument;
        return this;
    }

    public List<LinkedHashMap<String, String>> getForexTrade() {
        return forexTrade;
    }

    public WebhookChargeFeesBatchModel setForexTrade(List<LinkedHashMap<String, String>> forexTrade) {
        this.forexTrade = forexTrade;
        return this;
    }

    public String getCompletedTimestamp() {
        return completedTimestamp;
    }

    public WebhookChargeFeesBatchModel setCompletedTimestamp(String completedTimestamp) {
        this.completedTimestamp = completedTimestamp;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookChargeFeesBatchModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getConflict() {
        return conflict;
    }

    public WebhookChargeFeesBatchModel setConflict(String conflict) {
        this.conflict = conflict;
        return this;
    }

    public String getAuthSessionId() {
        return authSessionId;
    }

    public WebhookChargeFeesBatchModel setAuthSessionId(String authSessionId) {
        this.authSessionId = authSessionId;
        return this;
    }

    public String getNote() {
        return note;
    }

    public WebhookChargeFeesBatchModel setNote(String note) {
        this.note = note;
        return this;
    }
}
