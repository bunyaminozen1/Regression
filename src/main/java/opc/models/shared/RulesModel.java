package opc.models.shared;

import java.util.List;

public class RulesModel {

    private final List<String> value;
    private final boolean hasValue;

    public RulesModel(final Builder builder) {
        this.value = builder.value;
        this.hasValue = builder.hasValue;
    }

    public List<String> getValue() {
        return value;
    }

    public boolean isHasValue() {
        return hasValue;
    }

    public static class Builder {
        private List<String> value;
        private boolean hasValue;

        public Builder setValue(List<String> value) {
            this.value = value;
            return this;
        }

        public Builder setHasValue(boolean hasValue) {
            this.hasValue = hasValue;
            return this;
        }

        public RulesModel build() { return new RulesModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static RulesModel defaultRulesModel(final List<String> value){
        return new Builder().setValue(value).setHasValue(true).build();
    }
}