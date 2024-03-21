package opc.models.sumsub.questionnaire;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.models.innovator.ContextDimensionPartModel;

import java.util.Arrays;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SumSubQuestionnaireModel {

  private SumSubQuestionnaireContext context;
  private ContextDimensionPartModel value;

  public static SumSubQuestionnaireModel createSumSubQuestionnaire(final String corporateQuestionnaireId,
                                                             final String itemId,
                                                             final String value) {
    return SumSubQuestionnaireModel.builder()
            .context(new SumSubQuestionnaireContext(Arrays.asList(
                    new SumSubQuestionnaireDimensionKeyValueModel("QuestionnaireId", corporateQuestionnaireId),
                    new SumSubQuestionnaireDimensionKeyValueModel("QuestionnaireItemId", itemId))))
            .value(new ContextDimensionPartModel(Arrays.asList(value)))
            .build();
  }
}
