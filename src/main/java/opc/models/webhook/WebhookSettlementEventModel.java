package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookSettlementEventModel {
    private LinkedHashMap<String, String> availableBalance;
    private WebhookCardModeDetailsModel details;
    private LinkedHashMap<String, String> feeAmount;
    private LinkedHashMap<String, String> id;
    private String merchantCategoryCode;
    private String merchantId;
    private String merchantName;
    private LinkedHashMap<String, String> owner;
    private String relatedAuthorisationId;
    private String relatedSettlementId;
    private String settlementType;
    private LinkedHashMap<String, String> sourceAmount;
    private LinkedHashMap<String, String> transactionAmount;
    private String transactionId;
    private String transactionTimestamp;
    private String authCode;
    private String state;
    private LinkedHashMap<String, String> forexFee;

    public LinkedHashMap<String, String> getAvailableBalance() {
        return availableBalance;
    }

    public WebhookSettlementEventModel setAvailableBalance(LinkedHashMap<String, String> availableBalance) {
        this.availableBalance = availableBalance;
        return this;
    }

    public WebhookCardModeDetailsModel getDetails() {
        return details;
    }

    public WebhookSettlementEventModel setDetails(WebhookCardModeDetailsModel details) {
        this.details = details;
        return this;
    }

    public LinkedHashMap<String, String> getFeeAmount() {
        return feeAmount;
    }

    public WebhookSettlementEventModel setFeeAmount(LinkedHashMap<String, String> feeAmount) {
        this.feeAmount = feeAmount;
        return this;
    }

    public LinkedHashMap<String, String> getId() {
        return id;
    }

    public WebhookSettlementEventModel setId(LinkedHashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public WebhookSettlementEventModel setMerchantCategoryCode(String merchantCategoryCode) {
        this.merchantCategoryCode = merchantCategoryCode;
        return this;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public WebhookSettlementEventModel setMerchantId(String merchantId) {
        this.merchantId = merchantId;
        return this;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public WebhookSettlementEventModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public LinkedHashMap<String, String> getOwner() {
        return owner;
    }

    public WebhookSettlementEventModel setOwner(LinkedHashMap<String, String> owner) {
        this.owner = owner;
        return this;
    }

    public String getRelatedAuthorisationId() {
        return relatedAuthorisationId;
    }

    public WebhookSettlementEventModel setRelatedAuthorisationId(String relatedAuthorisationId) {
        this.relatedAuthorisationId = relatedAuthorisationId;
        return this;
    }

    public String getRelatedSettlementId() {
        return relatedSettlementId;
    }

    public WebhookSettlementEventModel setRelatedSettlementId(String relatedSettlementId) {
        this.relatedSettlementId = relatedSettlementId;
        return this;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public WebhookSettlementEventModel setSettlementType(String settlementType) {
        this.settlementType = settlementType;
        return this;
    }

    public LinkedHashMap<String, String> getSourceAmount() {
        return sourceAmount;
    }

    public WebhookSettlementEventModel setSourceAmount(LinkedHashMap<String, String> sourceAmount) {
        this.sourceAmount = sourceAmount;
        return this;
    }

    public LinkedHashMap<String, String> getTransactionAmount() {
        return transactionAmount;
    }

    public WebhookSettlementEventModel setTransactionAmount(LinkedHashMap<String, String> transactionAmount) {
        this.transactionAmount = transactionAmount;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public WebhookSettlementEventModel setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public WebhookSettlementEventModel setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
        return this;
    }

    public String getAuthCode() {
        return authCode;
    }

    public WebhookSettlementEventModel setAuthCode(String authCode) {
        this.authCode = authCode;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookSettlementEventModel setState(String state) {
        this.state = state;
        return this;
    }

    public LinkedHashMap<String, String> getForexFee() {
        return forexFee;
    }

    public WebhookSettlementEventModel setForexFee(LinkedHashMap<String, String> forexFee) {
        this.forexFee = forexFee;
        return this;
    }
}
