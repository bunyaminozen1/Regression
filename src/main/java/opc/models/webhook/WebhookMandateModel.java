package opc.models.webhook;

import opc.models.shared.TypeIdModel;

public class WebhookMandateModel {

    private String id;
    private String profileId;
    private TypeIdModel instrumentId;
    private TypeIdModel ownerId;
    private String status;
    private Long setupDate;
    private String type;
    private Long paymentAmount;
    private Long numberOfPayments;
    private String collectionFrequency;
    private Long collectionDueDate;
    private String merchantName;
    private String merchantAccountNumber;
    private String merchantReference;
    private String merchantSortCode;

    public String getId() {
        return id;
    }

    public WebhookMandateModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public WebhookMandateModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public TypeIdModel getInstrumentId() {
        return instrumentId;
    }

    public WebhookMandateModel setInstrumentId(TypeIdModel instrumentId) {
        this.instrumentId = instrumentId;
        return this;
    }

    public TypeIdModel getOwnerId() {
        return ownerId;
    }

    public WebhookMandateModel setOwnerId(TypeIdModel ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WebhookMandateModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public Long getSetupDate() {
        return setupDate;
    }

    public WebhookMandateModel setSetupDate(Long setupDate) {
        this.setupDate = setupDate;
        return this;
    }

    public String getType() {
        return type;
    }

    public WebhookMandateModel setType(String type) {
        this.type = type;
        return this;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public WebhookMandateModel setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
        return this;
    }

    public Long getNumberOfPayments() {
        return numberOfPayments;
    }

    public WebhookMandateModel setNumberOfPayments(Long numberOfPayments) {
        this.numberOfPayments = numberOfPayments;
        return this;
    }

    public String getCollectionFrequency() {
        return collectionFrequency;
    }

    public WebhookMandateModel setCollectionFrequency(String collectionFrequency) {
        this.collectionFrequency = collectionFrequency;
        return this;
    }

    public Long getCollectionDueDate() {
        return collectionDueDate;
    }

    public WebhookMandateModel setCollectionDueDate(Long collectionDueDate) {
        this.collectionDueDate = collectionDueDate;
        return this;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public WebhookMandateModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public String getMerchantAccountNumber() {
        return merchantAccountNumber;
    }

    public WebhookMandateModel setMerchantAccountNumber(String merchantAccountNumber) {
        this.merchantAccountNumber = merchantAccountNumber;
        return this;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public WebhookMandateModel setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    public String getMerchantSortCode() {
        return merchantSortCode;
    }

    public WebhookMandateModel setMerchantSortCode(String merchantSortCode) {
        this.merchantSortCode = merchantSortCode;
        return this;
    }
}
