package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookChargeFeesInstrumentDetailsModel {
    public LinkedHashMap<String, String> owner;
    public String fiProvider;
    public String channelProvider;
    public LinkedHashMap<String, String> originalAmount;
    public String currency;
    public String availableAdjustment;
    public String reservedAdjustment;
    public String pendingAdjustment;
    public String availableSync;
    public String deltaAdjustment;
    public String limitAdjustment;
    public LinkedHashMap<String, LinkedHashMap<String, String>> fee;
    public LinkedHashMap<String, String> system;
    public Boolean create;
    public Boolean upgrade;
    public Boolean activate;
    public String blockType;
    public String blockSubType;
    public String unblockType;
    public String unblockSubType;
    public String destroyType;
    public String replacementType;
    public Boolean related;
    public Boolean forceState;
    public Boolean forceBalance;

    public LinkedHashMap<String, String> getOwner() {
        return owner;
    }

    public WebhookChargeFeesInstrumentDetailsModel setOwner(LinkedHashMap<String, String> owner) {
        this.owner = owner;
        return this;
    }

    public String getFiProvider() {
        return fiProvider;
    }

    public WebhookChargeFeesInstrumentDetailsModel setFiProvider(String fiProvider) {
        this.fiProvider = fiProvider;
        return this;
    }

    public String getChannelProvider() {
        return channelProvider;
    }

    public WebhookChargeFeesInstrumentDetailsModel setChannelProvider(String channelProvider) {
        this.channelProvider = channelProvider;
        return this;
    }

    public LinkedHashMap<String, String> getOriginalAmount() {
        return originalAmount;
    }

    public WebhookChargeFeesInstrumentDetailsModel setOriginalAmount(LinkedHashMap<String, String> originalAmount) {
        this.originalAmount = originalAmount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public WebhookChargeFeesInstrumentDetailsModel setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getAvailableAdjustment() {
        return availableAdjustment;
    }

    public WebhookChargeFeesInstrumentDetailsModel setAvailableAdjustment(String availableAdjustment) {
        this.availableAdjustment = availableAdjustment;
        return this;
    }

    public String getReservedAdjustment() {
        return reservedAdjustment;
    }

    public WebhookChargeFeesInstrumentDetailsModel setReservedAdjustment(String reservedAdjustment) {
        this.reservedAdjustment = reservedAdjustment;
        return this;
    }

    public String getPendingAdjustment() {
        return pendingAdjustment;
    }

    public WebhookChargeFeesInstrumentDetailsModel setPendingAdjustment(String pendingAdjustment) {
        this.pendingAdjustment = pendingAdjustment;
        return this;
    }

    public String getAvailableSync() {
        return availableSync;
    }

    public WebhookChargeFeesInstrumentDetailsModel setAvailableSync(String availableSync) {
        this.availableSync = availableSync;
        return this;
    }

    public String getDeltaAdjustment() {
        return deltaAdjustment;
    }

    public WebhookChargeFeesInstrumentDetailsModel setDeltaAdjustment(String deltaAdjustment) {
        this.deltaAdjustment = deltaAdjustment;
        return this;
    }

    public String getLimitAdjustment() {
        return limitAdjustment;
    }

    public WebhookChargeFeesInstrumentDetailsModel setLimitAdjustment(String limitAdjustment) {
        this.limitAdjustment = limitAdjustment;
        return this;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getFee() {
        return fee;
    }

    public WebhookChargeFeesInstrumentDetailsModel setFee(LinkedHashMap<String, LinkedHashMap<String, String>> fee) {
        this.fee = fee;
        return this;
    }

    public LinkedHashMap<String, String> getSystem() {
        return system;
    }

    public WebhookChargeFeesInstrumentDetailsModel setSystem(LinkedHashMap<String, String> system) {
        this.system = system;
        return this;
    }

    public Boolean getCreate() {
        return create;
    }

    public WebhookChargeFeesInstrumentDetailsModel setCreate(Boolean create) {
        this.create = create;
        return this;
    }

    public Boolean getUpgrade() {
        return upgrade;
    }

    public WebhookChargeFeesInstrumentDetailsModel setUpgrade(Boolean upgrade) {
        this.upgrade = upgrade;
        return this;
    }

    public Boolean getActivate() {
        return activate;
    }

    public WebhookChargeFeesInstrumentDetailsModel setActivate(Boolean activate) {
        this.activate = activate;
        return this;
    }

    public String getBlockType() {
        return blockType;
    }

    public WebhookChargeFeesInstrumentDetailsModel setBlockType(String blockType) {
        this.blockType = blockType;
        return this;
    }

    public String getBlockSubType() {
        return blockSubType;
    }

    public WebhookChargeFeesInstrumentDetailsModel setBlockSubType(String blockSubType) {
        this.blockSubType = blockSubType;
        return this;
    }

    public String getUnblockType() {
        return unblockType;
    }

    public WebhookChargeFeesInstrumentDetailsModel setUnblockType(String unblockType) {
        this.unblockType = unblockType;
        return this;
    }

    public String getUnblockSubType() {
        return unblockSubType;
    }

    public WebhookChargeFeesInstrumentDetailsModel setUnblockSubType(String unblockSubType) {
        this.unblockSubType = unblockSubType;
        return this;
    }

    public String getDestroyType() {
        return destroyType;
    }

    public WebhookChargeFeesInstrumentDetailsModel setDestroyType(String destroyType) {
        this.destroyType = destroyType;
        return this;
    }

    public String getReplacementType() {
        return replacementType;
    }

    public WebhookChargeFeesInstrumentDetailsModel setReplacementType(String replacementType) {
        this.replacementType = replacementType;
        return this;
    }

    public Boolean getRelated() {
        return related;
    }

    public WebhookChargeFeesInstrumentDetailsModel setRelated(Boolean related) {
        this.related = related;
        return this;
    }

    public Boolean getForceState() {
        return forceState;
    }

    public WebhookChargeFeesInstrumentDetailsModel setForceState(Boolean forceState) {
        this.forceState = forceState;
        return this;
    }

    public Boolean getForceBalance() {
        return forceBalance;
    }

    public WebhookChargeFeesInstrumentDetailsModel setForceBalance(Boolean forceBalance) {
        this.forceBalance = forceBalance;
        return this;
    }
}
