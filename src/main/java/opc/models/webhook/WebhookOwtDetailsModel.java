package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;

public class WebhookOwtDetailsModel {

    private String creationTimestamp;
    private String description;
    private WebhookOwtDestinationModel destination;
    private LinkedHashMap<String, String> id;
    private String profileId;
    private LinkedHashMap<String, String> source;
    private String state;
    private String tag;
    private LinkedHashMap<String, String> transactionAmount;
    private LinkedHashMap<String, String> transactionFee;
    private LinkedHashMap<String, String> transferAmount;
    private String type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("conflict")
    private String conflict;
    private String beneficiaryId;
    private String challengeExemptionReason;

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public WebhookOwtDetailsModel setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WebhookOwtDetailsModel setDescription(String description) {
        this.description = description;
        return this;
    }

    public WebhookOwtDestinationModel getDestination() {
        return destination;
    }

    public WebhookOwtDetailsModel setDestination(WebhookOwtDestinationModel destination) {
        this.destination = destination;
        return this;
    }

    public LinkedHashMap<String, String> getId() {
        return id;
    }

    public WebhookOwtDetailsModel setId(LinkedHashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }


    public WebhookOwtDetailsModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public String getConflict() {
        return conflict;
    }

    public WebhookOwtDetailsModel setConflict(String conflict) {
        this.conflict = conflict;
        return this;
    }

    public LinkedHashMap<String, String> getSource() {
        return source;
    }

    public WebhookOwtDetailsModel setSource(LinkedHashMap<String, String> source) {
        this.source = source;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookOwtDetailsModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public WebhookOwtDetailsModel setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public LinkedHashMap<String, String> getTransactionAmount() {
        return transactionAmount;
    }

    public WebhookOwtDetailsModel setTransactionAmount(LinkedHashMap<String, String> transactionAmount) {
        this.transactionAmount = transactionAmount;
        return this;
    }

    public LinkedHashMap<String, String> getTransactionFee() {
        return transactionFee;
    }

    public WebhookOwtDetailsModel setTransactionFee(LinkedHashMap<String, String> transactionFee) {
        this.transactionFee = transactionFee;
        return this;
    }

    public LinkedHashMap<String, String> getTransferAmount() {
        return transferAmount;
    }

    public WebhookOwtDetailsModel setTransferAmount(LinkedHashMap<String, String> transferAmount) {
        this.transferAmount = transferAmount;
        return this;
    }

    public String getType() {
        return type;
    }

    public WebhookOwtDetailsModel setType(String type) {
        this.type = type;
        return this;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }

    public WebhookOwtDetailsModel setBeneficiaryId(String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
        return this;
    }

    public String getChallengeExemptionReason() {
        return challengeExemptionReason;
    }

    public WebhookOwtDetailsModel setChallengeExemptionReason(String challengeExemptionReason) {
        this.challengeExemptionReason = challengeExemptionReason;
        return this;
    }
}
