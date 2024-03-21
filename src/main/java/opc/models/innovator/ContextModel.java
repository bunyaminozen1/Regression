package opc.models.innovator;

public class ContextModel {
    private final ContextDimensionsModel context;
    private final ContextDimensionValueModel added;
    private final ContextDimensionValueModel set;
    private final ContextDimensionPartModel value;

    public ContextModel(final Builder builder) {
        this.context = builder.context;
        this.added = builder.added;
        this.set = builder.set;
        this.value = builder.value;
    }

    public ContextDimensionsModel getContext() {
        return context;
    }

    public ContextDimensionValueModel getAdded() {
        return added;
    }

    public ContextDimensionPartModel getValue() { return value;}

    public ContextDimensionValueModel getSet() { return set;}

    public static class Builder {
        private ContextDimensionsModel context;
        private ContextDimensionValueModel added;
        private ContextDimensionValueModel set;
        private ContextDimensionPartModel value;

        public Builder setContext(ContextDimensionsModel context) {
            this.context = context;
            return this;
        }

        public Builder setAdded(ContextDimensionValueModel added) {
            this.added = added;
            return this;
        }

        public Builder setSet(ContextDimensionValueModel set) {
            this.set = set;
            return this;
        }

        public Builder setValue(ContextDimensionPartModel value) {
            this.value = value;
            return this;
        }

        public ContextModel build() {return new ContextModel(this); }
    }

    public static Builder builder(){ return new Builder(); }
}
