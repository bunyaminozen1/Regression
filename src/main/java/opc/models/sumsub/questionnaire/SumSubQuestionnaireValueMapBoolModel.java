package opc.models.sumsub.questionnaire;

import java.util.ArrayList;
import java.util.List;

public  class  SumSubQuestionnaireValueMapBoolModel {
    private final boolean value;
    private final List<String> values;

    public boolean getValue() {
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    public SumSubQuestionnaireValueMapBoolModel(final Builder builder) {
        this.value = builder.value;
        this.values = builder.values;
    }

    public static class Builder {

        private boolean value;
        private List<String> values;

        public boolean getValue() {
            return value;
        }

        public List<String> getValues() {
            return values;
        }

        public Builder setValue(boolean value) {
            this.value = value;
            return this;
        }

        public Builder setValues(List<String> values) {
            this.values = values;
            return this;
        }

        public SumSubQuestionnaireValueMapBoolModel build() { return new SumSubQuestionnaireValueMapBoolModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
