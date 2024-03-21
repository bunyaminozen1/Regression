package opc.models.innovator;

public class ContextSetModel {

    private final ContextDimensionsModel context;
    private final ContextDimensionValueModel set;

    public ContextSetModel(final Builder builder) {
        this.context = builder.context;
        this.set = builder.set;
    }

    public ContextDimensionsModel getContext() {
        return context;
    }

    public ContextDimensionValueModel getSet() {
        return set;
    }

    public static class Builder {
        private ContextDimensionsModel context;
        private ContextDimensionValueModel set;

        public Builder setContext(ContextDimensionsModel context) {
            this.context = context;
            return this;
        }

        public Builder setSet(ContextDimensionValueModel set) {
            this.set = set;
            return this;
        }

        public ContextSetModel build() {return new ContextSetModel(this); }
    }

    public static Builder builder(){ return new Builder(); }
}
