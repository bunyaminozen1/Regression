package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookSendDetailsModel {

    private LinkedHashMap<String, String> id;
    private String profileId;
    private String tag;
    private LinkedHashMap<String, String> source;
    private LinkedHashMap<String, String> destination;
    private LinkedHashMap<String, String> destinationAmount;
    private String state;
    private String creationTimestamp;
    private String conflict;
    private String description;
    private String challengeExemptionReason;
    private String beneficiaryId;

    public LinkedHashMap<String, String> getId() {
        return id;
    }

    public WebhookSendDetailsModel setId(LinkedHashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public WebhookSendDetailsModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public WebhookSendDetailsModel setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public LinkedHashMap<String, String> getSource() {
        return source;
    }

    public WebhookSendDetailsModel setSource(LinkedHashMap<String, String> source) {
        this.source = source;
        return this;
    }

    public LinkedHashMap<String, String> getDestination() {
        return destination;
    }

    public WebhookSendDetailsModel setDestination(LinkedHashMap<String, String> destination) {
        this.destination = destination;
        return this;
    }

    public LinkedHashMap<String, String> getDestinationAmount() {
        return destinationAmount;
    }

    public WebhookSendDetailsModel setDestinationAmount(LinkedHashMap<String, String> destinationAmount) {
        this.destinationAmount = destinationAmount;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookSendDetailsModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public WebhookSendDetailsModel setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    public String getConflict() {
        return conflict;
    }

    public WebhookSendDetailsModel setConflict(String conflict) {
        this.conflict = conflict;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WebhookSendDetailsModel setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getChallengeExemptionReason() {
        return challengeExemptionReason;
    }

    public WebhookSendDetailsModel setChallengeExemptionReason(String challengeExemptionReason) {
        this.challengeExemptionReason = challengeExemptionReason;
        return this;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }

    public WebhookSendDetailsModel setBeneficiaryId(String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
        return this;
    }
}
