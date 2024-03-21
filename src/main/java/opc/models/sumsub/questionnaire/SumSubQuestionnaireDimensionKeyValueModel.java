package opc.models.sumsub.questionnaire;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SumSubQuestionnaireDimensionKeyValueModel {
  private String key;
  private String value;
}
