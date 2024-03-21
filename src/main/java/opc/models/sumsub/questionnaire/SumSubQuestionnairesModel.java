package opc.models.sumsub.questionnaire;

public class SumSubQuestionnairesModel {
    private final String id;
    private final SumSubQuestionnaireSections sections;

    public SumSubQuestionnairesModel(final Builder builder) {
        this.id = builder.id;
        this.sections = builder.sections;
    }

    public String getId() {
        return id;
    }

    public SumSubQuestionnaireSections getSections() {
        return sections;
    }

    public static class Builder {
        private String id;
        private SumSubQuestionnaireSections sections;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setSections(SumSubQuestionnaireSections sections) {
            this.sections = sections;
            return this;
        }

        public String getId() {
            return id;
        }

        public SumSubQuestionnaireSections getSections() {
            return sections;
        }

        public SumSubQuestionnairesModel build() {
            return new SumSubQuestionnairesModel(this);
        }
    }

    public static Builder builder() { return new Builder(); }

}
