package opc.models.admin;

public class LimitsRemainingDeltasModel {

    private final LimitsIdentityModel identity;
    private final LimitsApiContextModel context;

    public LimitsRemainingDeltasModel(final Builder builder) {
        this.identity = builder.identity;
        this.context = builder.context;
    }

    public LimitsIdentityModel getIdentity() {
        return identity;
    }

    public LimitsApiContextModel getContext() {
        return context;
    }

    public static class Builder {
        private LimitsIdentityModel identity;
        private LimitsApiContextModel context;

        public Builder setIdentity(LimitsIdentityModel identity) {
            this.identity = identity;
            return this;
        }

        public Builder setContext(LimitsApiContextModel context) {
            this.context = context;
            return this;
        }

        public LimitsRemainingDeltasModel build() { return new LimitsRemainingDeltasModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
