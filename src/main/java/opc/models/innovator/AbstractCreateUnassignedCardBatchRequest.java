package opc.models.innovator;

import opc.enums.opc.CardBureau;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.InstrumentType;
import opc.models.shared.DeliveryAddressModel;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Optional;

public class AbstractCreateUnassignedCardBatchRequest {
    private final String profileId;
    private final String tag;
    private final String productReference;
    private final String carrierType;
    private final String type;
    private final String cardLevelClassification;
    private final String deliveryMethod;
    private final DeliveryAddressModel deliveryAddress;
    private final String activationCode;
    private final int batchSize;

    public AbstractCreateUnassignedCardBatchRequest(final Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.productReference = builder.productReference;
        this.carrierType = builder.carrierType;
        this.type = builder.type;
        this.cardLevelClassification = builder.cardLevelClassification;
        this.deliveryMethod = builder.deliveryMethod;
        this.deliveryAddress = builder.deliveryAddress;
        this.activationCode = builder.activationCode;
        this.batchSize = builder.batchSize;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getTag() {
        return tag;
    }

    public String getProductReference() {
        return productReference;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public String getType() {
        return type;
    }

    public String getCardLevelClassification() {
        return cardLevelClassification;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public DeliveryAddressModel getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public static class Builder {
        private String profileId;
        private String tag;
        private String productReference;
        private String carrierType;
        private String type;
        private String cardLevelClassification;
        private String deliveryMethod;
        private DeliveryAddressModel deliveryAddress;
        private String activationCode;
        private int batchSize;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setProductReference(String productReference) {
            this.productReference = productReference;
            return this;
        }

        public Builder setCarrierType(String carrierType) {
            this.carrierType = carrierType;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setCardLevelClassification(String cardLevelClassification) {
            this.cardLevelClassification = cardLevelClassification;
            return this;
        }

        public Builder setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
            return this;
        }

        public Builder setDeliveryAddress(DeliveryAddressModel deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
            return this;
        }

        public Builder setActivationCode(String activationCode) {
            this.activationCode = activationCode;
            return this;
        }

        public Builder setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public AbstractCreateUnassignedCardBatchRequest build() { return new AbstractCreateUnassignedCardBatchRequest(this); }
    }

    public static Builder DefaultCardPoolReplenishModel(final String managedCardsProfileId,
                                                        final CardLevelClassification cardLevelClassification,
                                                        final InstrumentType instrumentType,
                                                        final String verificationCode,
                                                        final Optional<Integer> batchSize) {
        return new Builder()
                .setProfileId(managedCardsProfileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setType(instrumentType.name())
                .setCardLevelClassification(cardLevelClassification.name())
                .setDeliveryMethod("STANDARD_DELIVERY")
                .setDeliveryAddress(DeliveryAddressModel.DefaultDeliveryAddressModel().build())
                .setActivationCode(verificationCode)
                .setBatchSize(batchSize.orElse(14))
                .setProductReference(instrumentType.equals(InstrumentType.PHYSICAL) ? CardBureau.NITECREST.getProductReference() : null)
                .setCarrierType(instrumentType.equals(InstrumentType.PHYSICAL) ? CardBureau.NITECREST.getCarrierType() : null);
    }
}
