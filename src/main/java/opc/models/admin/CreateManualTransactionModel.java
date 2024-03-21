package opc.models.admin;

import opc.models.shared.ManagedInstrumentTypeId;

public class CreateManualTransactionModel {

    private final ManagedInstrumentTypeId targetInstrument;
    private final String currency;
    private final Long availableAmount;
    private final Long deltaAmount;
    private final Long pendingAmount;
    private final Long reservedAmount;
    private final String systemAccount;
    private final boolean forceBalance;
    private final String note;
    private final boolean remoteAvailableAdjustment;
    private final boolean bypassLimitCheck;

    public CreateManualTransactionModel(final Builder builder) {
        this.targetInstrument = builder.targetInstrument;
        this.currency = builder.currency;
        this.availableAmount = builder.availableAmount;
        this.deltaAmount = builder.deltaAmount;
        this.pendingAmount = builder.pendingAmount;
        this.reservedAmount = builder.reservedAmount;
        this.systemAccount = builder.systemAccount;
        this.forceBalance = builder.forceBalance;
        this.note = builder.note;
        this.remoteAvailableAdjustment = builder.remoteAvailableAdjustment;
        this.bypassLimitCheck = builder.bypassLimitCheck;
    }

    public ManagedInstrumentTypeId getTargetInstrument() {
        return targetInstrument;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getAvailableAmount() {
        return availableAmount;
    }

    public Long getDeltaAmount() {
        return deltaAmount;
    }

    public Long getPendingAmount() {
        return pendingAmount;
    }

    public Long getReservedAmount() {
        return reservedAmount;
    }

    public String getSystemAccount() {
        return systemAccount;
    }

    public boolean isForceBalance() {
        return forceBalance;
    }

    public String getNote() {
        return note;
    }

    public boolean isRemoteAvailableAdjustment() {
        return remoteAvailableAdjustment;
    }

    public boolean isBypassLimitCheck() {
        return bypassLimitCheck;
    }

    public static class Builder {
        private ManagedInstrumentTypeId targetInstrument;
        private String currency;
        private Long availableAmount;
        private Long deltaAmount;
        private Long pendingAmount;
        private Long reservedAmount;
        private String systemAccount;
        private boolean forceBalance;
        private String note;
        private boolean remoteAvailableAdjustment;
        private boolean bypassLimitCheck;

        public Builder setTargetInstrument(ManagedInstrumentTypeId targetInstrument) {
            this.targetInstrument = targetInstrument;
            return this;
        }

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setAvailableAmount(Long availableAmount) {
            this.availableAmount = availableAmount;
            return this;
        }

        public Builder setDeltaAmount(Long deltaAmount) {
            this.deltaAmount = deltaAmount;
            return this;
        }

        public Builder setPendingAmount(Long pendingAmount) {
            this.pendingAmount = pendingAmount;
            return this;
        }

        public Builder setReservedAmount(Long reservedAmount) {
            this.reservedAmount = reservedAmount;
            return this;
        }

        public Builder setSystemAccount(String systemAccount) {
            this.systemAccount = systemAccount;
            return this;
        }

        public Builder setForceBalance(boolean forceBalance) {
            this.forceBalance = forceBalance;
            return this;
        }

        public Builder setNote(String note) {
            this.note = note;
            return this;
        }

        public Builder setRemoteAvailableAdjustment(boolean remoteAvailableAdjustment) {
            this.remoteAvailableAdjustment = remoteAvailableAdjustment;
            return this;
        }

        public Builder setBypassLimitCheck(boolean bypassLimitCheck) {
            this.bypassLimitCheck = bypassLimitCheck;
            return this;
        }

        public CreateManualTransactionModel build() { return new CreateManualTransactionModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
