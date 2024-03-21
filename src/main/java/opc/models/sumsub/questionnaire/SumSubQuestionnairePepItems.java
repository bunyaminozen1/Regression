package opc.models.sumsub.questionnaire;

import java.util.ArrayList;

public class SumSubQuestionnairePepItems {
    private final SumSubQuestionnaireValueMapModel declaration;
    private final SumSubQuestionnaireValueMapModel pepcategory;
    private final SumSubQuestionnaireValueMapModel nolongerpep;
    private final SumSubQuestionnaireValueMapModel rcacategory;
    private final SumSubQuestionnaireValueMapModel nolongerrca;

    public SumSubQuestionnairePepItems(final Builder builder) {
        this.declaration = builder.declaration;
        this.pepcategory = builder.pepcategory;
        this.nolongerpep = builder.nolongerpep;
        this.rcacategory = builder.rcacategory;
        this.nolongerrca = builder.nolongerrca;
    }

    public SumSubQuestionnaireValueMapModel getDeclaration() {
        return declaration;
    }

    public SumSubQuestionnaireValueMapModel getPepcategory() {
        return pepcategory;
    }

    public SumSubQuestionnaireValueMapModel getNolongerpep() {
        return nolongerpep;
    }

    public SumSubQuestionnaireValueMapModel getRcacategory() {
        return rcacategory;
    }

    public SumSubQuestionnaireValueMapModel getNolongerrca() {
        return nolongerrca;
    }

    public static class Builder {
        private SumSubQuestionnaireValueMapModel declaration;
        private SumSubQuestionnaireValueMapModel pepcategory;
        private SumSubQuestionnaireValueMapModel nolongerpep;
        private SumSubQuestionnaireValueMapModel rcacategory;
        private SumSubQuestionnaireValueMapModel nolongerrca;

        public SumSubQuestionnaireValueMapModel getDeclaration() { return declaration; }

        public Builder setDeclaration(SumSubQuestionnaireValueMapModel declaration) {
            this.declaration = declaration;
            return this;
        }

        public SumSubQuestionnaireValueMapModel getNolongerpep() {
            return nolongerpep;
        }

        public Builder setNolongerpep(SumSubQuestionnaireValueMapModel nolongerpep) {
            this.nolongerpep = nolongerpep;
            return this;
        }

        public SumSubQuestionnaireValueMapModel getNolongerrca() {
            return nolongerrca;
        }

        public Builder setNolongerrca(SumSubQuestionnaireValueMapModel nolongerrca) {
            this.nolongerrca = nolongerrca;
            return this;
        }

        public SumSubQuestionnaireValueMapModel getPepcategory() {
            return pepcategory;
        }

        public Builder setPepcategory(SumSubQuestionnaireValueMapModel pepcategory) {
            this.pepcategory = pepcategory;
            return this;
        }

        public SumSubQuestionnaireValueMapModel getRcacategory() {
            return rcacategory;
        }

        public Builder setRcacategory(SumSubQuestionnaireValueMapModel rcacategory) {
            this.rcacategory = rcacategory;
            return this;
        }

        public SumSubQuestionnairePepItems build() {
            return new SumSubQuestionnairePepItems(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder noPepOrRca() {

        return new Builder()
                .setDeclaration(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("true")
                        .setValues(new ArrayList<>())
                        .build())
                .setNolongerpep(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValues(new ArrayList<>())
                        .build())
                .setNolongerrca(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("no")
                        .setValues(new ArrayList<>())
                        .build())
                .setPepcategory(SumSubQuestionnaireValueMapModel
                        .notAPep()
                        .build())
                .setRcacategory(SumSubQuestionnaireValueMapModel
                        .notAnRca()
                        .build());
    }

    public static Builder pepAndRca() {

        return new Builder()
                .setDeclaration(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("true")
                        .setValues(new ArrayList<>())
                        .build())
                .setNolongerpep(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("yes")
                        .setValues(new ArrayList<>())
                        .build())
                .setNolongerrca(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("yes")
                        .setValues(new ArrayList<>())
                        .build())
                .setPepcategory(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("ARMED_FORCES")
                        .setValues(new ArrayList<>())
                        .build())
                .setRcacategory(SumSubQuestionnaireValueMapModel
                        .builder()
                        .setValue("ASCENDANTS")
                        .setValues(new ArrayList<>())
                        .build());
    }
}
