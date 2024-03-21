package opc.models.multi.managedcards;

import opc.enums.opc.CardBureau;
import org.apache.commons.lang3.RandomStringUtils;

public class UpgradeToPhysicalCardModel {
    private final String productReference;
    private final String carrierType;
    private final String deliveryMethod;
    private final PhysicalCardAddressModel deliveryAddress;
    private final String activationCode;
    private final PinValueModel pin;
    private final String nameOnCardLine2;
    private final boolean bulkDelivery;

    public UpgradeToPhysicalCardModel(final Builder builder) {
        this.productReference = builder.productReference;
        this.carrierType = builder.carrierType;
        this.deliveryMethod = builder.deliveryMethod;
        this.deliveryAddress = builder.deliveryAddress;
        this.activationCode = builder.activationCode;
        this.pin = builder.pin;
        this.nameOnCardLine2 = builder.nameOnCardLine2;
        this.bulkDelivery = builder.bulkDelivery;
    }

    public String getProductReference() {
        return productReference;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public PhysicalCardAddressModel getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public PinValueModel getPin() {
        return pin;
    }

    public String getNameOnCardLine2() { return nameOnCardLine2; }
    public boolean isBulkDelivery() { return bulkDelivery; }



    public static class Builder {
        private String productReference;
        private String carrierType;
        private String deliveryMethod;
        private PhysicalCardAddressModel deliveryAddress;
        private String activationCode;
        private PinValueModel pin;
        private String nameOnCardLine2;
        private boolean bulkDelivery;

        public Builder setProductReference(String productReference) {
            this.productReference = productReference;
            return this;
        }

        public Builder setCarrierType(String carrierType) {
            this.carrierType = carrierType;
            return this;
        }

        public Builder setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
            return this;
        }

        public Builder setDeliveryAddress(PhysicalCardAddressModel deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
            return this;
        }

        public Builder setActivationCode(String activationCode) {
            this.activationCode = activationCode;
            return this;
        }

        public Builder setPin(PinValueModel pin) {
            this.pin = pin;
            return this;
        }

        public Builder setNameOnCardLine2(String nameOnCardLine2) {
            this.nameOnCardLine2 = nameOnCardLine2;
            return this;
        }

        public Builder setBulkDelivery(boolean bulkDelivery) {
            this.bulkDelivery = bulkDelivery;
            return this;
        }


        public UpgradeToPhysicalCardModel build() { return new UpgradeToPhysicalCardModel(this); }
    }

    public static Builder DefaultUpgradeToPhysicalCardModel(final String verificationCode){
        return new Builder()
                .setActivationCode(verificationCode)
                .setDeliveryAddress(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build())
                .setDeliveryMethod("STANDARD_DELIVERY")
                .setProductReference(CardBureau.NITECREST.getProductReference())
                .setNameOnCardLine2(RandomStringUtils.randomAlphabetic(5));
    }

    public static Builder DefaultUpgradeToPhysicalCardModel(final PhysicalCardAddressModel physicalCardAddressModel, final String verificationCode){
        return new Builder()
                .setActivationCode(verificationCode)
                .setDeliveryAddress(physicalCardAddressModel)
                .setDeliveryMethod("STANDARD_DELIVERY")
                .setProductReference(CardBureau.NITECREST.getProductReference())
                .setNameOnCardLine2(RandomStringUtils.randomAlphabetic(5));
    }

    public static Builder DefaultUpgradeToPhysicalCardModel(final PhysicalCardAddressModel physicalCardAddressModel,
                                                            final String verificationCode,
                                                            final String deliveryMethod) {
        return new Builder()
            .setActivationCode(verificationCode)
            .setDeliveryAddress(physicalCardAddressModel)
            .setDeliveryMethod(deliveryMethod)
            .setProductReference(CardBureau.NITECREST.getProductReference())
            .setNameOnCardLine2(RandomStringUtils.randomAlphabetic(5));
    }
}