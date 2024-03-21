package opc.models.sumsub.questionnaire;

public class SumSubQuestionnaireSections {
    private final SumSubQuestionnaireDeclarationSection declarationsection;
    private final SumSubQuestionnairePep pep;
    private final SumSubQuestionnaireSectionOne section1;

    public SumSubQuestionnaireSections(final Builder builder) {
        this.declarationsection = builder.declarationSection;
        this.pep = builder.pep;
        this.section1 = builder.section1;
    }

    public SumSubQuestionnaireDeclarationSection getDeclarationsection() {
        return declarationsection;
    }

    public SumSubQuestionnairePep getPep() {
        return pep;
    }

    public SumSubQuestionnaireSectionOne getSection1() {
        return section1;
    }

    public static class Builder {
        private SumSubQuestionnaireDeclarationSection declarationSection;
        private SumSubQuestionnairePep pep;
        private SumSubQuestionnaireSectionOne section1;

        public SumSubQuestionnaireDeclarationSection getDeclarationSection() {
            return declarationSection;
        }

        public Builder setDeclarationSection(SumSubQuestionnaireDeclarationSection declarationSection) {
            this.declarationSection = declarationSection;
            return this;
        }

        public SumSubQuestionnairePep getPep() {
            return pep;
        }

        public Builder setPep(SumSubQuestionnairePep pep) {
            this.pep = pep;
            return this;
        }

        public SumSubQuestionnaireSectionOne getSection1() {
            return section1;
        }

        public Builder setSection1(SumSubQuestionnaireSectionOne section1) {
            this.section1 = section1;
            return this;
        }

        public SumSubQuestionnaireSections build() {
            return new SumSubQuestionnaireSections(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
