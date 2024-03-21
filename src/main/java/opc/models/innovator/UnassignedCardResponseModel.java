package opc.models.innovator;

import java.util.HashMap;

public class UnassignedCardResponseModel {
    private HashMap<String, String> id;
    private String externalHandle;
    private String profileId;
    private String tag;
    private String currency;
    private String type;
    private String productReference;
    private String carrierType;
    private String cardBureau;
    private String deliveryMethod;
    private String activationCode;
    private String cardFundingType;
    private HashMap<String, String> deliveryAddress;
    private boolean isUnassigned = true;
    private String parentManagedAccountId;
    private String manufacturingState;

    public HashMap<String, String> getId() {
        return id;
    }

    public UnassignedCardResponseModel setId(HashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public String getExternalHandle() {
        return externalHandle;
    }

    public UnassignedCardResponseModel setExternalHandle(String externalHandle) {
        this.externalHandle = externalHandle;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public UnassignedCardResponseModel setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public UnassignedCardResponseModel setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public UnassignedCardResponseModel setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getType() {
        return type;
    }

    public UnassignedCardResponseModel setType(String type) {
        this.type = type;
        return this;
    }

    public String getProductReference() {
        return productReference;
    }

    public UnassignedCardResponseModel setProductReference(String productReference) {
        this.productReference = productReference;
        return this;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public UnassignedCardResponseModel setCarrierType(String carrierType) {
        this.carrierType = carrierType;
        return this;
    }

    public String getCardBureau() {
        return cardBureau;
    }

    public UnassignedCardResponseModel setCardBureau(String cardBureau) {
        this.cardBureau = cardBureau;
        return this;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public UnassignedCardResponseModel setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
        return this;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public UnassignedCardResponseModel setActivationCode(String activationCode) {
        this.activationCode = activationCode;
        return this;
    }

    public String getCardFundingType() {
        return cardFundingType;
    }

    public UnassignedCardResponseModel setCardFundingType(String cardFundingType) {
        this.cardFundingType = cardFundingType;
        return this;
    }

    public HashMap<String, String> getDeliveryAddress() {
        return deliveryAddress;
    }

    public UnassignedCardResponseModel setDeliveryAddress(HashMap<String, String> deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
        return this;
    }

    public boolean isUnassigned() {
        return isUnassigned;
    }

    public UnassignedCardResponseModel setUnassigned(boolean unassigned) {
        isUnassigned = unassigned;
        return this;
    }

    public String getParentManagedAccountId() {
        return parentManagedAccountId;
    }

    public UnassignedCardResponseModel setParentManagedAccountId(String parentManagedAccountId) {
        this.parentManagedAccountId = parentManagedAccountId;
        return this;
    }

    public String getManufacturingState() {
        return manufacturingState;
    }

    public UnassignedCardResponseModel setManufacturingState(String manufacturingState) {
        this.manufacturingState = manufacturingState;
        return this;
    }
}
