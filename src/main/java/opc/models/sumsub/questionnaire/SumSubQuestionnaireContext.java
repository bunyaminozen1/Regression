package opc.models.sumsub.questionnaire;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SumSubQuestionnaireContext {

  private List<SumSubQuestionnaireDimensionKeyValueModel> dimension;

  public SumSubQuestionnaireContext setDimension(List<SumSubQuestionnaireDimensionKeyValueModel> dimension) {
    this.dimension = dimension;
    return this;
  }
}
