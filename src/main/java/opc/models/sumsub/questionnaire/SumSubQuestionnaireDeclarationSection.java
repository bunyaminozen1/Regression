package opc.models.sumsub.questionnaire;

public class SumSubQuestionnaireDeclarationSection {
    private final SumSubQuestionnaireDeclarationSectionItems items;

    public SumSubQuestionnaireDeclarationSection(final Builder builder) {
        this.items = builder.items;
    }

    public SumSubQuestionnaireDeclarationSectionItems getItems() {
        return items;
    }

    public static class Builder {
        private SumSubQuestionnaireDeclarationSectionItems items;

        public SumSubQuestionnaireDeclarationSectionItems getItems() {
            return items;
        }

        public Builder setItems(SumSubQuestionnaireDeclarationSectionItems items) {
            this.items = items;
            return this;
        }

        public SumSubQuestionnaireDeclarationSection build() {
            return new SumSubQuestionnaireDeclarationSection(this);
        }
    }

    public static Builder builder() { return new Builder(); }
}
