package opc.models.multi.managedcards;

import opc.models.shared.AddressModel;
import org.apache.commons.lang3.RandomStringUtils;

public class AssignManagedCardModel {
    private final String externalReference;
    private final String activationCode;
    private final String friendlyName;
    private final String nameOnCard;
    private final AddressModel billingAddress;
    private final String cardholderMobileNumber;
    private final String nameOnCardLine2;
    private final ThreeDSecureAuthConfigModel threeDSecureAuthConfig;

    public AssignManagedCardModel(final Builder builder) {
        this.externalReference = builder.externalReference;
        this.activationCode = builder.activationCode;
        this.friendlyName = builder.friendlyName;
        this.nameOnCard = builder.nameOnCard;
        this.billingAddress = builder.billingAddress;
        this.cardholderMobileNumber = builder.cardholderMobileNumber;
        this.nameOnCardLine2 = builder.nameOnCardLine2;
        this.threeDSecureAuthConfig = builder.threeDSecureAuthConfig;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getNameOnCard() {
        return nameOnCard;
    }

    public AddressModel getBillingAddress() {
        return billingAddress;
    }

    public String getCardholderMobileNumber() {
        return cardholderMobileNumber;
    }

    public String getNameOnCardLine2() {
        return nameOnCardLine2;
    }

    public ThreeDSecureAuthConfigModel getThreeDSecureAuthConfig() {
        return threeDSecureAuthConfig;
    }

    public static class Builder {
        private String externalReference;
        private String activationCode;
        private String friendlyName;
        private String nameOnCard;
        private AddressModel billingAddress;
        private String cardholderMobileNumber;
        private String nameOnCardLine2;
        private ThreeDSecureAuthConfigModel threeDSecureAuthConfig;

        public Builder setExternalReference(String externalReference) {
            this.externalReference = externalReference;
            return this;
        }

        public Builder setActivationCode(String activationCode) {
            this.activationCode = activationCode;
            return this;
        }

        public Builder setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder setNameOnCard(String nameOnCard) {
            this.nameOnCard = nameOnCard;
            return this;
        }

        public Builder setBillingAddress(AddressModel billingAddress) {
            this.billingAddress = billingAddress;
            return this;
        }

        public Builder setCardholderMobileNumber(String cardholderMobileNumber) {
            this.cardholderMobileNumber = cardholderMobileNumber;
            return this;
        }

        public Builder setNameOnCardLine2(String nameOnCardLine2) {
            this.nameOnCardLine2 = nameOnCardLine2;
            return this;
        }

        public Builder setThreeDSecureAuthConfig(
                ThreeDSecureAuthConfigModel threeDSecureAuthConfig) {
            this.threeDSecureAuthConfig = threeDSecureAuthConfig;
            return this;
        }

        public AssignManagedCardModel build() { return new AssignManagedCardModel(this); }
    }

    public static PatchManagedCardModel.Builder builder() {
        return new PatchManagedCardModel.Builder();
    }

    public static Builder DefaultAssignManagedCardModel(final String verificationCode){
        return new Builder()
                .setActivationCode(verificationCode)
                .setBillingAddress(AddressModel.RandomAddressModel())
                .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                .setExternalReference(RandomStringUtils.randomAlphabetic(5))
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCardLine2(RandomStringUtils.randomAlphabetic(5));
    }

    public static Builder assignManagedCardWithoutMobileNumberModel(final String verificationCode){
        return new Builder()
                .setActivationCode(verificationCode)
                .setBillingAddress(AddressModel.RandomAddressModel())
                .setExternalReference(RandomStringUtils.randomAlphabetic(5))
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCardLine2(RandomStringUtils.randomAlphabetic(5));
    }
}
