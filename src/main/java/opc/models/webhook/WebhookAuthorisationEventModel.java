package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookAuthorisationEventModel {
    private boolean approved;
    private LinkedHashMap<String, String> authForwardingDetails;
    private String authRuleFailedReason;
    private String authorisationType;
    private LinkedHashMap<String, String> availableBalance;
    private String cardPresent;
    private String cardholderPresent;
    private String declineReason;
    private WebhookCardModeDetailsModel details;
    private LinkedHashMap<String, String> id;
    private String merchantCategoryCode;
    private String merchantId;
    private String merchantName;
    private LinkedHashMap<String, String> owner;
    private String relatedAuthorisationId;
    private LinkedHashMap<String, String> sourceAmount;
    private LinkedHashMap<String, String> transactionAmount;
    private String transactionId;
    private String transactionTimestamp;
    private String authCode;
    private LinkedHashMap<String, String> forexFee;
    private LinkedHashMap<String, String> forexPadding;

    public boolean isApproved() {
        return approved;
    }

    public WebhookAuthorisationEventModel setApproved(boolean approved) {
        this.approved = approved;
        return this;
    }

    public String getAuthRuleFailedReason() {
        return authRuleFailedReason;
    }

    public WebhookAuthorisationEventModel setAuthRuleFailedReason(String authRuleFailedReason) {
        this.authRuleFailedReason = authRuleFailedReason;
        return this;
    }

    public String getAuthorisationType() {
        return authorisationType;
    }

    public WebhookAuthorisationEventModel setAuthorisationType(String authorisationType) {
        this.authorisationType = authorisationType;
        return this;
    }

    public LinkedHashMap<String, String> getAvailableBalance() {
        return availableBalance;
    }

    public WebhookAuthorisationEventModel setAvailableBalance(LinkedHashMap<String, String> availableBalance) {
        this.availableBalance = availableBalance;
        return this;
    }

    public String getCardPresent() {
        return cardPresent;
    }

    public WebhookAuthorisationEventModel setCardPresent(String cardPresent) {
        this.cardPresent = cardPresent;
        return this;
    }

    public String getCardholderPresent() {
        return cardholderPresent;
    }

    public WebhookAuthorisationEventModel setCardholderPresent(String cardholderPresent) {
        this.cardholderPresent = cardholderPresent;
        return this;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public WebhookAuthorisationEventModel setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
        return this;
    }

    public WebhookCardModeDetailsModel getDetails() {
        return details;
    }

    public WebhookAuthorisationEventModel setDetails(WebhookCardModeDetailsModel details) {
        this.details = details;
        return this;
    }

    public LinkedHashMap<String, String> getId() {
        return id;
    }

    public WebhookAuthorisationEventModel setId(LinkedHashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public WebhookAuthorisationEventModel setMerchantCategoryCode(String merchantCategoryCode) {
        this.merchantCategoryCode = merchantCategoryCode;
        return this;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public WebhookAuthorisationEventModel setMerchantId(String merchantId) {
        this.merchantId = merchantId;
        return this;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public WebhookAuthorisationEventModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public LinkedHashMap<String, String> getOwner() {
        return owner;
    }

    public WebhookAuthorisationEventModel setOwner(LinkedHashMap<String, String> owner) {
        this.owner = owner;
        return this;
    }

    public String getRelatedAuthorisationId() {
        return relatedAuthorisationId;
    }

    public WebhookAuthorisationEventModel setRelatedAuthorisationId(String relatedAuthorisationId) {
        this.relatedAuthorisationId = relatedAuthorisationId;
        return this;
    }

    public LinkedHashMap<String, String> getSourceAmount() {
        return sourceAmount;
    }

    public WebhookAuthorisationEventModel setSourceAmount(LinkedHashMap<String, String> sourceAmount) {
        this.sourceAmount = sourceAmount;
        return this;
    }

    public LinkedHashMap<String, String> getTransactionAmount() {
        return transactionAmount;
    }

    public WebhookAuthorisationEventModel setTransactionAmount(LinkedHashMap<String, String> transactionAmount) {
        this.transactionAmount = transactionAmount;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public WebhookAuthorisationEventModel setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public WebhookAuthorisationEventModel setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
        return this;
    }

    public String getAuthCode() {
        return authCode;
    }

    public WebhookAuthorisationEventModel setAuthCode(String authCode) {
        this.authCode = authCode;
        return this;
    }

    public LinkedHashMap<String, String> getForexFee() {
        return forexFee;
    }

    public WebhookAuthorisationEventModel setForexFee(LinkedHashMap<String, String> forexFee) {
        this.forexFee = forexFee;
        return this;
    }

    public LinkedHashMap<String, String> getForexPadding() {
        return forexPadding;
    }

    public WebhookAuthorisationEventModel setForexPadding(LinkedHashMap<String, String> forexPadding) {
        this.forexPadding = forexPadding;
        return this;
    }

    public LinkedHashMap<String, String> getAuthForwardingDetails() {
        return authForwardingDetails;
    }

    public LinkedHashMap<String, String> setAuthForwardingDetails(LinkedHashMap<String, String> authForwardingDetails) {
        this.authForwardingDetails = authForwardingDetails;
        return authForwardingDetails;
    }
}
