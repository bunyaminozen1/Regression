package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookChargeFeesDetailsModel {
    private String id;
    private String feeType;
    private String feeSubType;
    private String note;
    private String profileId;
    private LinkedHashMap<String, String> source;
    private WebhookChargeFeesBatchModel batch;
    private WebhookChargeFeesFeeSpecModel feeSpec;

    public String getId() {
        return id;
    }

    public WebhookChargeFeesDetailsModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getFeeType() {
        return feeType;
    }

    public WebhookChargeFeesDetailsModel setFeeType(String feeType) {
        this.feeType = feeType;
        return this;
    }

    public String getFeeSubType() {
        return feeSubType;
    }

    public WebhookChargeFeesDetailsModel setFeeSubType(String feeSubType) {
        this.feeSubType = feeSubType;
        return this;
    }

    public LinkedHashMap<String, String> getSource() {
        return source;
    }

    public WebhookChargeFeesDetailsModel setSource(LinkedHashMap<String, String> source) {
        this.source = source;
        return this;
    }

    public WebhookChargeFeesFeeSpecModel getFeeSpec() {
        return feeSpec;
    }

    public WebhookChargeFeesDetailsModel setFeeSpec(WebhookChargeFeesFeeSpecModel feeSpec) {
        this.feeSpec = feeSpec;
        return this;
    }

    public WebhookChargeFeesDetailsModel setNote(String note) {
        this.note = note;
        return this;
    }

    public String getNote() {
        return note;
    }

    public WebhookChargeFeesBatchModel getBatch() {
        return batch;
    }

    public WebhookChargeFeesDetailsModel setBatch(WebhookChargeFeesBatchModel batch) {
        this.batch = batch;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public WebhookChargeFeesDetailsModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }
}
