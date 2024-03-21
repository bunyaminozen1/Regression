package opc.models.sumsub.questionnaire;

public class SumSubQuestionnaireSectionOne {
    private final SumSubQuestionnaireSectionOneItems items;

    public SumSubQuestionnaireSectionOne(final Builder builder) {
        this.items = builder.items;
    }

    public SumSubQuestionnaireSectionOneItems getItems() {
        return items;
    }

    public static class Builder {
        private SumSubQuestionnaireSectionOneItems items;

        public SumSubQuestionnaireSectionOneItems getItems() {
            return items;
        }

        public Builder setItems(SumSubQuestionnaireSectionOneItems items) {
            this.items = items;
            return this;
        }

        public SumSubQuestionnaireSectionOne build() {
            return new SumSubQuestionnaireSectionOne(this);
        }
    }

    public static Builder builder() { return new Builder(); }
}
