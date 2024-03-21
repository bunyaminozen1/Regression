package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import opc.enums.opc.KycLevel;

import java.util.Arrays;
import java.util.List;

public class LimitDimensionModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String key;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Boolean matchAny;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String value;

    public LimitDimensionModel(final Builder builder) {
        this.key = builder.key;
        this.matchAny = builder.matchAny;
        this.value = builder.value;
    }

    public String getKey() {
        return key;
    }

    public Boolean isMatchAny() {
        return matchAny;
    }

    public String getValue() {
        return value;
    }

    public static class Builder {
        private String key;
        private Boolean matchAny;
        private String value;

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setMatchAny(Boolean matchAny) {
            this.matchAny = matchAny;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public LimitDimensionModel build() { return new LimitDimensionModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static List<LimitDimensionModel> defaultLimitDimensionModel(){
        return Arrays.asList(LimitDimensionModel.builder().setKey("UserId").setMatchAny(true).build(),
                LimitDimensionModel.builder().setKey("FullDueDiligenceCheck").setValue("APPROVED").setMatchAny(null).build());
    }

    public static List<LimitDimensionModel> defaultLimitDimensionModel(final KycLevel kycLevel){
        return Arrays.asList(LimitDimensionModel.builder().setKey("UserId").setMatchAny(true).build(),
                LimitDimensionModel.builder().setKey("FullDueDiligenceCheck").setValue("APPROVED").setMatchAny(null).build(),
                LimitDimensionModel.builder().setKey("KycLevel").setValue(kycLevel.name()).setMatchAny(null).build());
    }

    public static List<LimitDimensionModel> userSpecificLimitDimensionModel(final String userId, final KycLevel kycLevel){
        return Arrays.asList(LimitDimensionModel.builder().setKey("UserId").setMatchAny(null).setValue(userId).build(),
                LimitDimensionModel.builder().setKey("FullDueDiligenceCheck").setValue("APPROVED").setMatchAny(null).build(),
                LimitDimensionModel.builder().setKey("KycLevel").setValue(kycLevel.name()).setMatchAny(null).build());
    }
}
