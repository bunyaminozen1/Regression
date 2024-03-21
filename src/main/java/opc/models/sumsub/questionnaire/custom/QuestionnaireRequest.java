package opc.models.sumsub.questionnaire.custom;


import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Builder
@Data
public class QuestionnaireRequest {
    private final String id;
    private final List<Questionnaires> questionnaires;
    @Builder
    @Data
    public static class Questionnaires {
        private final String id;
        private final Map<String, Items> sections;

    }
    @Data
    @Builder
    public static class Items {
        private final Map<String, Value> items;
    }

    @Data
    @Builder
    public static class Value {
        private final String value;
        private final List<String> values;
    }

}
