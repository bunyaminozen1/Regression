package opc.models.sumsub.questionnaire;

public class SumSubQuestionnairePep {
    private final SumSubQuestionnairePepItems items;
    
    public SumSubQuestionnairePep(final Builder builder) {
        this.items = builder.items;
    }

    public SumSubQuestionnairePepItems getItems() {
        return items;
    }

    public static class Builder {
        private SumSubQuestionnairePepItems items;

        public SumSubQuestionnairePepItems getItems() {
            return items;
        }

        public Builder setItems(SumSubQuestionnairePepItems items) {
            this.items = items;
            return this;
        }

        public SumSubQuestionnairePep build() {
            return new SumSubQuestionnairePep(this);
        }
    }

    public static Builder builder() { return new Builder(); }
}
