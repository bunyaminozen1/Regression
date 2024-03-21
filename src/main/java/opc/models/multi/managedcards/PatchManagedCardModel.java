package opc.models.multi.managedcards;

import java.util.List;
import opc.models.shared.AddressModel;
import org.apache.commons.lang3.RandomStringUtils;

public class PatchManagedCardModel {
    private final String tag;
    private final String friendlyName;
    private final String cardholderMobileNumber;
    private final AddressModel billingAddress;
    private final PhysicalCardAddressModel deliveryAddress;
    private final String deliveryMethod;
    private final DigitalWalletsModel digitalWallets;
    private final String authForwardingDefaultTimeoutDecision;
    private final ThreeDSecureAuthConfigModel threeDSecureAuthConfig;
    private final String nameOnCard;
    private final String nameOnCardLine2;
    private final List<ExternalDataModel> externalData;
    private final String renewalType;

    public PatchManagedCardModel(final Builder builder) {
        this.tag = builder.tag;
        this.friendlyName = builder.friendlyName;
        this.cardholderMobileNumber = builder.cardholderMobileNumber;
        this.billingAddress = builder.billingAddress;
        this.deliveryAddress = builder.deliveryAddress;
        this.deliveryMethod = builder.deliveryMethod;
        this.digitalWallets = builder.digitalWallets;
        this.authForwardingDefaultTimeoutDecision = builder.authForwardingDefaultTimeoutDecision;
        this.threeDSecureAuthConfig = builder.threeDSecureAuthConfig;
        this.nameOnCard = builder.nameOnCard;
        this.nameOnCardLine2 = builder.nameOnCardLine2;
        this.externalData = builder.externalData;
        this.renewalType = builder.renewalType;
    }

    public String getTag() {
        return tag;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getCardholderMobileNumber() {
        return cardholderMobileNumber;
    }

    public AddressModel getBillingAddress() {
        return billingAddress;
    }

    public PhysicalCardAddressModel getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public DigitalWalletsModel getDigitalWallets() {
        return digitalWallets;
    }
    public String getAuthForwardingDefaultTimeoutDecision() { return authForwardingDefaultTimeoutDecision; }

    public ThreeDSecureAuthConfigModel getThreeDSecureAuthConfig() { return threeDSecureAuthConfig; }

    public String getNameOnCard() { return nameOnCard; }

    public String getNameOnCardLine2() { return nameOnCardLine2; }
    public List<ExternalDataModel> getExternalData() {return externalData; }
    public String getRenewalType() {return renewalType; }

    public static class Builder {
        private String tag;
        private String friendlyName;
        private String cardholderMobileNumber;
        private AddressModel billingAddress;
        private PhysicalCardAddressModel deliveryAddress;
        private String deliveryMethod;
        private DigitalWalletsModel digitalWallets;
        private String authForwardingDefaultTimeoutDecision;
        private ThreeDSecureAuthConfigModel threeDSecureAuthConfig;
        private String nameOnCard;
        private String nameOnCardLine2;
        private List<ExternalDataModel> externalData;
        private String renewalType;

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder setCardholderMobileNumber(String cardholderMobileNumber) {
            this.cardholderMobileNumber = cardholderMobileNumber;
            return this;
        }

        public Builder setBillingAddress(AddressModel billingAddress) {
            this.billingAddress = billingAddress;
            return this;
        }

        public Builder setDeliveryAddress(PhysicalCardAddressModel deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
            return this;
        }

        public Builder setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
            return this;
        }

        public Builder setDigitalWallets(DigitalWalletsModel digitalWallets) {
            this.digitalWallets = digitalWallets;
            return this;
        }
        public Builder setAuthForwardingDefaultTimeoutDecision(String authForwardingDefaultTimeoutDecision) {
            this.authForwardingDefaultTimeoutDecision = authForwardingDefaultTimeoutDecision;
            return this;
        }

        public Builder setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel threeDSecureAuthConfig) {
            this.threeDSecureAuthConfig = threeDSecureAuthConfig;
            return this;
        }
        public Builder setNameOnCard(String nameOnCard) {
            this.nameOnCard = nameOnCard;
            return this;
        }

        public Builder setNameOnCardLine2(String nameOnCardLine2) {
            this.nameOnCardLine2 = nameOnCardLine2;
            return this;
        }
        public Builder setExternalData(List<ExternalDataModel> externalData) {
            this.externalData = externalData;
            return this;
        }

        public Builder setRenewalType (String renewalType) {
            this.renewalType = renewalType;
            return this;
        }

        public PatchManagedCardModel build() { return new PatchManagedCardModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder DefaultPatchManagedCardModel() {
        return new Builder()
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                .setBillingAddress(AddressModel.RandomAddressModel())
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setNameOnCardLine2(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder DefaultPatchManagedCardAuthForwardingModel(final String authForwardingDefaultTimeoutDecision) {
        return new Builder()
                .setAuthForwardingDefaultTimeoutDecision(authForwardingDefaultTimeoutDecision);
    }

    public static Builder ThreeDSecurePatchManagedCardModelPrimaryBiometrics(final String linkedUserId) {
        return new Builder()
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.DefaultThreeDSecureAuthConfigModel(linkedUserId).build());
    }


    public static Builder DefaultThreeDSecurePatchManagedCardModelPrimaryOTP(final String linkedUserId) {
        return new Builder()
            .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                .setLinkedUserId(linkedUserId)
                .setPrimaryChannel("OTP_SMS").build());
    }
}