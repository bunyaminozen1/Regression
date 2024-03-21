package opc.models.admin;

import opc.enums.opc.AdminFeeType;
import opc.models.shared.ManagedInstrumentTypeId;

public class ChargeFeeModel {

    private final AdminFeeType feeType;
    private final String feeSubType;
    private final String note;
    private final ManagedInstrumentTypeId source;
    private final FeeSpecModel feeSpec;

    public ChargeFeeModel(final Builder builder) {
        this.feeType = builder.feeType;
        this.feeSubType = builder.feeSubType;
        this.note = builder.note;
        this.source = builder.source;
        this.feeSpec = builder.feeSpec;
    }

    public AdminFeeType getFeeType() {
        return feeType;
    }

    public String getFeeSubType() {
        return feeSubType;
    }

    public String getNote() {
        return note;
    }

    public ManagedInstrumentTypeId getSource() {
        return source;
    }

    public FeeSpecModel getFeeSpec() {
        return feeSpec;
    }

    public static class Builder {
        private AdminFeeType feeType;
        private String feeSubType;
        private String note;
        private ManagedInstrumentTypeId source;
        private FeeSpecModel feeSpec;

        public Builder setFeeType(AdminFeeType feeType) {
            this.feeType = feeType;
            return this;
        }

        public Builder setFeeSubType(String feeSubType) {
            this.feeSubType = feeSubType;
            return this;
        }

        public Builder setNote(String note) {
            this.note = note;
            return this;
        }

        public Builder setSource(ManagedInstrumentTypeId source) {
            this.source = source;
            return this;
        }

        public Builder setFeeSpec(FeeSpecModel feeSpec) {
            this.feeSpec = feeSpec;
            return this;
        }

        public ChargeFeeModel build() { return new ChargeFeeModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
