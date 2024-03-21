package opc.models.testmodels;

import opc.enums.opc.InstrumentType;
import opc.enums.opc.ManagedCardMode;
import opc.models.innovator.CreateUnassignedCardBatchModel;
import opc.models.innovator.UnassignedCardResponseModel;

public class UnassignedManagedCardDetails {
    private final InstrumentType instrumentType;
    private final CreateUnassignedCardBatchModel createUnassignedCardBatchModel;
    private final UnassignedCardResponseModel unassignedCardResponseModel;
    private final ManagedCardMode managedCardMode;

    public UnassignedManagedCardDetails(final Builder builder) {
        this.instrumentType = builder.instrumentType;
        this.createUnassignedCardBatchModel = builder.createUnassignedCardBatchModel;
        this.unassignedCardResponseModel = builder.unassignedCardResponseModel;
        this.managedCardMode = builder.managedCardMode;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public CreateUnassignedCardBatchModel getCreateUnassignedCardBatchModel() {
        return createUnassignedCardBatchModel;
    }

    public UnassignedCardResponseModel getUnassignedCardResponseModel() {
        return unassignedCardResponseModel;
    }

    public ManagedCardMode getManagedCardMode() {
        return managedCardMode;
    }

    public static class Builder {
        private InstrumentType instrumentType;
        private CreateUnassignedCardBatchModel createUnassignedCardBatchModel;
        private UnassignedCardResponseModel unassignedCardResponseModel;
        private ManagedCardMode managedCardMode;

        public Builder setInstrumentType(InstrumentType instrumentType) {
            this.instrumentType = instrumentType;
            return this;
        }

        public Builder setCreateUnassignedCardBatchModel(CreateUnassignedCardBatchModel createUnassignedCardBatchModel) {
            this.createUnassignedCardBatchModel = createUnassignedCardBatchModel;
            return this;
        }

        public Builder setUnassignedCardResponseModel(UnassignedCardResponseModel unassignedCardResponseModel) {
            this.unassignedCardResponseModel = unassignedCardResponseModel;
            return this;
        }

        public Builder setManagedCardMode(ManagedCardMode managedCardMode) {
            this.managedCardMode = managedCardMode;
            return this;
        }

        public UnassignedManagedCardDetails build() { return new UnassignedManagedCardDetails(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
