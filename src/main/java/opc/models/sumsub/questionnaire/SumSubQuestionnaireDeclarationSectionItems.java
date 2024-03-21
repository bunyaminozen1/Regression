package opc.models.sumsub.questionnaire;

import java.util.ArrayList;

public class SumSubQuestionnaireDeclarationSectionItems {
    private final SumSubQuestionnaireValueMapBoolModel declaration;

    public SumSubQuestionnaireDeclarationSectionItems(final Builder builder) {
        this.declaration = builder.declaration;
    }

    public SumSubQuestionnaireValueMapBoolModel getDeclaration() {
        return declaration;
    }

    public static class Builder {
        private SumSubQuestionnaireValueMapBoolModel declaration;

        public SumSubQuestionnaireValueMapBoolModel getDeclaration() {
            return declaration;
        }

        public Builder setDeclaration(SumSubQuestionnaireValueMapBoolModel declaration) {
            this.declaration = declaration;
            return this;
        }

        public SumSubQuestionnaireDeclarationSectionItems build() {
            return new SumSubQuestionnaireDeclarationSectionItems(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder defaultDeclarationSection() {
        return new Builder().setDeclaration(SumSubQuestionnaireValueMapBoolModel.builder().setValue(true).setValues(new ArrayList<>()).build());
    }
}
