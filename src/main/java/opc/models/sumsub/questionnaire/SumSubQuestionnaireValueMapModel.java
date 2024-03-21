package opc.models.sumsub.questionnaire;

import java.util.ArrayList;
import java.util.List;

public class SumSubQuestionnaireValueMapModel {
    private final String value;
    private final List<String> values;

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    public SumSubQuestionnaireValueMapModel(final Builder builder) {
        this.value = builder.value;
        this.values = builder.values;
    }

    public static class Builder {

        private String value;
        private List<String> values;

        public String getValue() {
            return value;
        }

        public List<String> getValues() {
            return values;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder setValues(List<String> values) {
            this.values = values;
            return this;
        }

        public SumSubQuestionnaireValueMapModel build() { return new SumSubQuestionnaireValueMapModel(this); }
    }

    public static Builder builder() { return new Builder(); }

    public static Builder notAPep() {
        return new Builder().setValue("NOT_A_PEP").setValues(new ArrayList<>());
    }

    public static Builder notAnRca() {
        return new Builder().setValue("NOT_AN_RCA").setValues(new ArrayList<>());
    }
}
