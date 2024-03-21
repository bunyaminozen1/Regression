package opc.models.admin;

public class TransactionAmountModel {

    private final Long value;
    private final boolean hasValue;

    public TransactionAmountModel(final Builder builder) {
        this.value = builder.value;
        this.hasValue = builder.hasValue;
    }

    public Long getValue() {
        return value;
    }

    public boolean isHasValue() {
        return hasValue;
    }

    public static class Builder {
        private Long value;
        private boolean hasValue;

        public Builder setValue(Long value) {
            this.value = value;
            return this;
        }

        public Builder setHasValue(boolean hasValue) {
            this.hasValue = hasValue;
            return this;
        }

        public TransactionAmountModel build() { return new TransactionAmountModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
