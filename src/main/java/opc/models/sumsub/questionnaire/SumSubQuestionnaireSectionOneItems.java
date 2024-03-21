package opc.models.sumsub.questionnaire;

import java.util.ArrayList;


public class SumSubQuestionnaireSectionOneItems {
    private final SumSubQuestionnaireValueMapModel declaration;
    private final SumSubQuestionnaireValueMapModel whichIndustryDoYouDe;
    private final SumSubQuestionnaireValueMapModel industry;

    public SumSubQuestionnaireSectionOneItems(final Builder builder) {
        this.declaration = builder.declaration;
        this.whichIndustryDoYouDe = builder.whichIndustryDoYouDe;
        this.industry = SumSubQuestionnaireValueMapModel.builder().setValue("ACCOUNTING_AUDIT_FINANCE").build();
    }

    public SumSubQuestionnaireValueMapModel getDeclaration() {
        return declaration;
    }

    public SumSubQuestionnaireValueMapModel getWhichIndustryDoYouDe() {
        return whichIndustryDoYouDe;
    }

    public static class Builder {
        private SumSubQuestionnaireValueMapModel declaration;
        private SumSubQuestionnaireValueMapModel whichIndustryDoYouDe;

        public SumSubQuestionnaireValueMapModel getDeclaration() {
            return declaration;
        }

        public Builder setDeclaration(SumSubQuestionnaireValueMapModel declaration) {
            this.declaration = declaration;
            return this;
        }

        public SumSubQuestionnaireValueMapModel getWhichIndustryDoYouDe() {
            return whichIndustryDoYouDe;
        }

        public Builder setWhichIndustryDoYouDe(SumSubQuestionnaireValueMapModel whichIndustryDoYouDe) {
            this.whichIndustryDoYouDe = whichIndustryDoYouDe;
            return this;
        }

        public SumSubQuestionnaireSectionOneItems build() {
            return new SumSubQuestionnaireSectionOneItems(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder defaultsection1Model() {
        return new Builder()
                .setDeclaration(SumSubQuestionnaireValueMapModel.builder().setValue("true").build())
                .setWhichIndustryDoYouDe(SumSubQuestionnaireValueMapModel.builder().setValue("PUBLIC_SECTOR_ADMINISTRATION").setValues(new ArrayList<>()).build());
    }


    public static Builder defaultsection1ModelCorp() {
        return new Builder();
    }
}
