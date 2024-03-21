package opc.models.secure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class AnonTokenizeModel {

    private final TokenizePropertiesModel values;

    public AnonTokenizeModel(final Builder builder) {
        this.values = builder.values;
    }

    public TokenizePropertiesModel getValues() {
        return values;
    }

    public static class Builder {
        private TokenizePropertiesModel values;

        public Builder setValues(TokenizePropertiesModel values) {
            this.values = values;
            return this;
        }

        public AnonTokenizeModel build() { return new AnonTokenizeModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    @SneakyThrows
    public static String anon() {
        return new ObjectMapper().writeValueAsString(AnonTokenizeModel.builder()
                .setValues(TokenizePropertiesModel.builder()
                        .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                .setPermanent(false)
                                .setValue("Pass1234!")
                                .build())
                        .build()).build());
    }
}
