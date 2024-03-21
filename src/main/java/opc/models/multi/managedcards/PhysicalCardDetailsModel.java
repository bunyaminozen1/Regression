package opc.models.multi.managedcards;

import opc.enums.opc.ReplacementType;

public class PhysicalCardDetailsModel {
    private final String productReference;
    private final String carrierType;
    private final boolean pendingActivation;
    private final boolean pinBlocked;
    private final ReplacementType replacement;

    public PhysicalCardDetailsModel(final Builder builder) {
        this.productReference = builder.productReference;
        this.carrierType = builder.carrierType;
        this.pendingActivation = builder.pendingActivation;
        this.pinBlocked = builder.pinBlocked;
        this.replacement = builder.replacement;
    }

    public String getProductReference() {
        return productReference;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public boolean isPendingActivation() {
        return pendingActivation;
    }

    public boolean isPinBlocked() {
        return pinBlocked;
    }

    public ReplacementType getReplacement() {
        return replacement;
    }

    public static class Builder {
        private String productReference;
        private String carrierType;
        private boolean pendingActivation;
        private boolean pinBlocked;
        private ReplacementType replacement;

        public Builder setProductReference(String productReference) {
            this.productReference = productReference;
            return this;
        }

        public Builder setCarrierType(String carrierType) {
            this.carrierType = carrierType;
            return this;
        }

        public Builder setPendingActivation(boolean pendingActivation) {
            this.pendingActivation = pendingActivation;
            return this;
        }

        public Builder setPinBlocked(boolean pinBlocked) {
            this.pinBlocked = pinBlocked;
            return this;
        }

        public Builder setReplacement(ReplacementType replacement) {
            this.replacement = replacement;
            return this;
        }

        public PhysicalCardDetailsModel build() { return new PhysicalCardDetailsModel(this); }
    }
}
