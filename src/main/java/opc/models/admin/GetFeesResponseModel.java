package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;

public class GetFeesResponseModel {

    private String id;
    private String feeType;
    private String feeSubType;
    private String note;
    private LinkedHashMap<String, String> source;
    private FeeSpecResponseModel feeSpec;
    @JsonIgnore
    private LinkedHashMap<String, String> batch;
    private LinkedHashMap<String, String> txId;
    private LinkedHashMap<String, String> instrumentId;
    private String profileId;

    public String getId() {
        return id;
    }

    public GetFeesResponseModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getFeeType() {
        return feeType;
    }

    public GetFeesResponseModel setFeeType(String feeType) {
        this.feeType = feeType;
        return this;
    }

    public String getFeeSubType() {
        return feeSubType;
    }

    public GetFeesResponseModel setFeeSubType(String feeSubType) {
        this.feeSubType = feeSubType;
        return this;
    }

    public String getNote() {
        return note;
    }

    public GetFeesResponseModel setNote(String note) {
        this.note = note;
        return this;
    }

    public LinkedHashMap<String, String> getSource() {
        return source;
    }

    public GetFeesResponseModel setSource(LinkedHashMap<String, String> source) {
        this.source = source;
        return this;
    }

    public FeeSpecResponseModel getFeeSpec() {
        return feeSpec;
    }

    public GetFeesResponseModel setFeeSpec(FeeSpecResponseModel feeSpec) {
        this.feeSpec = feeSpec;
        return this;
    }

    public LinkedHashMap<String, String> getBatch() {
        return batch;
    }

    public GetFeesResponseModel setBatch(LinkedHashMap<String, String> batch) {
        this.batch = batch;
        return this;
    }

    public LinkedHashMap<String, String> getTxId() {
        return txId;
    }

    public GetFeesResponseModel setTxId(LinkedHashMap<String, String> txId) {
        this.txId = txId;
        return this;
    }

    public LinkedHashMap<String, String> getInstrumentId() {
        return instrumentId;
    }

    public GetFeesResponseModel setInstrumentId(LinkedHashMap<String, String> instrumentId) {
        this.instrumentId = instrumentId;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public GetFeesResponseModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }
}
