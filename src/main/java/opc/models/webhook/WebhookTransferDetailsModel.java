package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookTransferDetailsModel {

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

    public LinkedHashMap<String, String> getId() {
        return id;
    }

    public WebhookTransferDetailsModel setId(LinkedHashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public WebhookTransferDetailsModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public WebhookTransferDetailsModel setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public LinkedHashMap<String, String> getSource() {
        return source;
    }

    public WebhookTransferDetailsModel setSource(LinkedHashMap<String, String> source) {
        this.source = source;
        return this;
    }

    public LinkedHashMap<String, String> getDestination() {
        return destination;
    }

    public WebhookTransferDetailsModel setDestination(LinkedHashMap<String, String> destination) {
        this.destination = destination;
        return this;
    }

    public LinkedHashMap<String, String> getDestinationAmount() {
        return destinationAmount;
    }

    public WebhookTransferDetailsModel setDestinationAmount(LinkedHashMap<String, String> destinationAmount) {
        this.destinationAmount = destinationAmount;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookTransferDetailsModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public WebhookTransferDetailsModel setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    public String getConflict() {
        return conflict;
    }

    public WebhookTransferDetailsModel setConflict(String conflict) {
        this.conflict = conflict;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WebhookTransferDetailsModel setDescription(String description) {
        this.description = description;
        return this;
    }
}
