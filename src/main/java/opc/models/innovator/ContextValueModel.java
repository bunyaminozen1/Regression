package opc.models.innovator;

public class ContextValueModel {

    private final ContextDimensionsModel context;
    private final ContextDimensionPartModel value;

    public ContextValueModel(final Builder builder) {
        this.context = builder.context;
        this.value = builder.value;
    }

    public ContextDimensionsModel getContext() {
        return context;
    }

    public ContextDimensionPartModel getValue() {
        return value;
    }

    public static class Builder {
        private ContextDimensionsModel context;
        private ContextDimensionPartModel value;

        public Builder setContext(ContextDimensionsModel context) {
            this.context = context;
            return this;
        }

        public Builder setValue(ContextDimensionPartModel value) {
            this.value = value;
            return this;
        }

        public ContextValueModel build() { return new ContextValueModel(this); }
    }

    public static Builder builder(){ return new Builder(); }
}
