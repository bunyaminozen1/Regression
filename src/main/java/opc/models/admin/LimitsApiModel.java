package opc.models.admin;

import java.util.List;

public class LimitsApiModel {

    private final LimitsApiContextModel context;
    private final List<LimitsApiValueModel> value;

    public LimitsApiModel(final Builder builder) {
        this.context = builder.context;
        this.value = builder.value;
    }

    public LimitsApiContextModel getContext() {
        return context;
    }

    public List<LimitsApiValueModel> getValue() {
        return value;
    }

    public static class Builder {
        private LimitsApiContextModel context;
        private List<LimitsApiValueModel> value;

        public Builder setContext(LimitsApiContextModel context) {
            this.context = context;
            return this;
        }

        public Builder setValue(List<LimitsApiValueModel> value) {
            this.value = value;
            return this;
        }

        public LimitsApiModel build() { return new LimitsApiModel(this);}
    }

    public static Builder builder(){
        return new Builder();
    }
}
