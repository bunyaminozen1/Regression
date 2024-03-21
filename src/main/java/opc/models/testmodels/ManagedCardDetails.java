package opc.models.testmodels;

import opc.enums.opc.InstrumentType;
import opc.enums.opc.ManagedCardMode;
import opc.enums.opc.ManufacturingState;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ThreeDSecureAuthConfigModel;

public class ManagedCardDetails {
    private final InstrumentType instrumentType;
    private final String managedCardId;
    private final CreateManagedCardModel managedCardModel;
    private final ManagedCardMode managedCardMode;
    private final PhysicalCardAddressModel physicalCardAddress;
    private final int initialManagedAccountBalance;
    private final int initialDepositAmount;
    private final ManufacturingState manufacturingState;
    private final ThreeDSecureAuthConfigModel threeDSecureAuthConfig;
    private final String cardholderMobileNumber;

    public ManagedCardDetails(final Builder builder) {
        this.instrumentType = builder.instrumentType;
        this.managedCardId = builder.managedCardId;
        this.managedCardModel = builder.managedCardModel;
        this.managedCardMode = builder.managedCardMode;
        this.physicalCardAddress = builder.physicalCardAddress;
        this.initialManagedAccountBalance = builder.initialManagedAccountBalance;
        this.initialDepositAmount = builder.initialDepositAmount;
        this.manufacturingState = builder.manufacturingState;
        this.threeDSecureAuthConfig = builder.threeDSecureAuthConfig;
        this.cardholderMobileNumber = builder.cardholderMobileNumber;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public String getManagedCardId() {
        return managedCardId;
    }

    public CreateManagedCardModel getManagedCardModel() {
        return managedCardModel;
    }

    public ManagedCardMode getManagedCardMode() {
        return managedCardMode;
    }

    public PhysicalCardAddressModel getPhysicalCardAddressModel() {
        return physicalCardAddress;
    }

    public int getInitialManagedAccountBalance() {
        return initialManagedAccountBalance;
    }

    public int getInitialDepositAmount() {
        return initialDepositAmount;
    }

    public ManufacturingState getManufacturingState() {
        return manufacturingState;
    }

    public ThreeDSecureAuthConfigModel getThreeDSecureAuthConfig() {
        return threeDSecureAuthConfig;
    }

    public String getCardholderMobileNumber() {
        return cardholderMobileNumber;
    }

    public static class Builder {
        private InstrumentType instrumentType;
        private String managedCardId;
        private CreateManagedCardModel managedCardModel;
        private ManagedCardMode managedCardMode;
        private PhysicalCardAddressModel physicalCardAddress;
        private int initialManagedAccountBalance;
        private int initialDepositAmount;
        private ManufacturingState manufacturingState;
        private ThreeDSecureAuthConfigModel threeDSecureAuthConfig;
        private String cardholderMobileNumber;

        public Builder setInstrumentType(InstrumentType instrumentType) {
            this.instrumentType = instrumentType;
            return this;
        }

        public Builder setManagedCardId(String managedCardId) {
            this.managedCardId = managedCardId;
            return this;
        }

        public Builder setManagedCardModel(CreateManagedCardModel managedCardModel) {
            this.managedCardModel = managedCardModel;
            return this;
        }

        public Builder setManagedCardMode(ManagedCardMode managedCardMode) {
            this.managedCardMode = managedCardMode;
            return this;
        }

        public Builder setPhysicalCardAddressModel(PhysicalCardAddressModel physicalCardAddress) {
            this.physicalCardAddress = physicalCardAddress;
            return this;
        }

        public Builder setInitialManagedAccountBalance(int initialManagedAccountBalance) {
            this.initialManagedAccountBalance = initialManagedAccountBalance;
            return this;
        }

        public Builder setInitialDepositAmount(int initialDepositAmount) {
            this.initialDepositAmount = initialDepositAmount;
            return this;
        }

        public Builder setManufacturingState(ManufacturingState manufacturingState) {
            this.manufacturingState = manufacturingState;
            return this;
        }

        public Builder setThreeDSecureAuthConfig(
            ThreeDSecureAuthConfigModel threeDSecureAuthConfig) {
            this.threeDSecureAuthConfig = threeDSecureAuthConfig;
            return this;
        }

        public Builder setCardholderMobileNumber(
            String cardholderMobileNumber) {
            this.cardholderMobileNumber = cardholderMobileNumber;
            return this;
        }

        public ManagedCardDetails build() { return new ManagedCardDetails(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ManagedCardDetails setPhysicalAddress(final ManagedCardDetails managedCardDetails, final PhysicalCardAddressModel physicalCardAddressModel) {
        return new Builder()
                .setInitialManagedAccountBalance(managedCardDetails.getInitialManagedAccountBalance())
                .setManagedCardModel(managedCardDetails.getManagedCardModel())
                .setManagedCardMode(managedCardDetails.getManagedCardMode())
                .setInstrumentType(managedCardDetails.getInstrumentType())
                .setPhysicalCardAddressModel(physicalCardAddressModel)
                .setManagedCardId(managedCardDetails.getManagedCardId())
                .setInitialDepositAmount(managedCardDetails.getInitialDepositAmount())
                .build();
    }
}
