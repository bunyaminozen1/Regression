package opc.models.multi.directdebit;

import opc.models.shared.TypeIdModel;

import java.util.LinkedHashMap;

public class GetMandateModel {

    private String id;
    private String profileId;
    private TypeIdModel instrumentId;
    private TypeIdModel ownerId;
    private String type;
    private String state;
    private String merchantName;
    private String merchantNumber;
    private String merchantReference;
    private Long creationTimestamp;
    private LinkedHashMap<String, String> lastPaymentAmount;
    private Long lastPaymentDate;

    public String getId() {
        return id;
    }

    public GetMandateModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public GetMandateModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public TypeIdModel getInstrumentId() {
        return instrumentId;
    }

    public GetMandateModel setInstrumentId(TypeIdModel instrumentId) {
        this.instrumentId = instrumentId;
        return this;
    }

    public TypeIdModel getOwnerId() {
        return ownerId;
    }

    public GetMandateModel setOwnerId(TypeIdModel ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public String getType() {
        return type;
    }

    public GetMandateModel setType(String type) {
        this.type = type;
        return this;
    }

    public String getState() {
        return state;
    }

    public GetMandateModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public GetMandateModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public String getMerchantNumber() {
        return merchantNumber;
    }

    public GetMandateModel setMerchantNumber(String merchantNumber) {
        this.merchantNumber = merchantNumber;
        return this;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public GetMandateModel setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public GetMandateModel setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    public LinkedHashMap<String, String> getLastPaymentAmount() {
        return lastPaymentAmount;
    }

    public GetMandateModel setLastPaymentAmount(LinkedHashMap<String, String> lastPaymentAmount) {
        this.lastPaymentAmount = lastPaymentAmount;
        return this;
    }

    public Long getLastPaymentDate() {
        return lastPaymentDate;
    }

    public GetMandateModel setLastPaymentDate(Long lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
        return this;
    }

}
