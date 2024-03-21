package opc.models.sumsub.questionnaire;
import java.util.ArrayList;
import java.util.List;

public class AddSumSubQuestionnaireModel {
    private final String id;
    private final List<SumSubQuestionnairesModel> questionnaires;

    public String getId() {
        return id;
    }

    public List<SumSubQuestionnairesModel> getQuestionnaires() {
        return questionnaires;
    }

    public AddSumSubQuestionnaireModel(final Builder builder) {
        this.id = builder.id;
        this.questionnaires = builder.questionnaires;
    }
    public static class Builder {

        private String id;
        private List<SumSubQuestionnairesModel> questionnaires;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public String getId() {
            return id;
        }

        public List<SumSubQuestionnairesModel> getQuestionnaires() {
            return questionnaires;
        }

        public Builder setQuestionnaires(List<SumSubQuestionnairesModel> questionnaires) {
            this.questionnaires = questionnaires;
            return this;
        }

        public Builder addToQuestionnaire(SumSubQuestionnairesModel questionnairesModel) {
            this.questionnaires = new ArrayList<>();
            this.questionnaires.add(questionnairesModel);
            return this;
        }

        public AddSumSubQuestionnaireModel build() { return new AddSumSubQuestionnaireModel(this);}
    }

    public static Builder builder() { return new Builder(); }

    public static Builder defaultNonPepQuestionnaire(final String applicantId, final String questionnaireId) {
        return new Builder()
                .setId(applicantId)
                .addToQuestionnaire(SumSubQuestionnairesModel.builder()
                        .setId(questionnaireId)
                        .setSections(SumSubQuestionnaireSections.builder()
                                .setDeclarationSection(SumSubQuestionnaireDeclarationSection.builder()
                                        .setItems(SumSubQuestionnaireDeclarationSectionItems.defaultDeclarationSection().build())
                                        .build())
                                .setPep(SumSubQuestionnairePep.builder()
                                        .setItems(SumSubQuestionnairePepItems.noPepOrRca().build())
                                        .build())
                                .setSection1(SumSubQuestionnaireSectionOne.builder()
                                        .setItems(SumSubQuestionnaireSectionOneItems.defaultsection1Model().build())
                                        .build())
                                .build())
                        .build());
    }


    public static Builder defaultNonPepQuestionnaireCorp(final String applicantId, final String questionnaireId) {
        return new Builder()
                .setId(applicantId)
                .addToQuestionnaire(SumSubQuestionnairesModel.builder()
                        .setId(questionnaireId)
                        .setSections(SumSubQuestionnaireSections.builder()
                                .setDeclarationSection(SumSubQuestionnaireDeclarationSection.builder()
                                        .setItems(SumSubQuestionnaireDeclarationSectionItems.defaultDeclarationSection().build())
                                        .build())
                                .setPep(SumSubQuestionnairePep.builder()
                                        .setItems(SumSubQuestionnairePepItems.noPepOrRca().build())
                                        .build())
                                .setSection1(SumSubQuestionnaireSectionOne.builder()
                                        .setItems(SumSubQuestionnaireSectionOneItems.defaultsection1ModelCorp().build())
                                        .build())
                                .build())
                        .build());
    }

    public static Builder pepAndRcaQuestionnaire(final String applicantId, final String questionnaireId) {
        return new Builder()
                .setId(applicantId)
                .addToQuestionnaire(SumSubQuestionnairesModel.builder()
                        .setId(questionnaireId)
                        .setSections(SumSubQuestionnaireSections.builder()
                                .setDeclarationSection(SumSubQuestionnaireDeclarationSection.builder()
                                        .setItems(SumSubQuestionnaireDeclarationSectionItems.defaultDeclarationSection().build())
                                        .build())
                                .setPep(SumSubQuestionnairePep.builder()
                                        .setItems(SumSubQuestionnairePepItems.pepAndRca().build())
                                        .build())
                                .setSection1(SumSubQuestionnaireSectionOne.builder()
                                        .setItems(SumSubQuestionnaireSectionOneItems.defaultsection1Model().build())
                                        .build())
                                .build())
                        .build());
    }

}
