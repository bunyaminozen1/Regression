package opc.models.secure;

public class TokenizePropertiesModel {

    private final AdditionalPropertiesModel additionalProp1;

    public TokenizePropertiesModel(final Builder builder) {
        this.additionalProp1 = builder.additionalProp1;
    }

    public AdditionalPropertiesModel getAdditionalProp1() {
        return additionalProp1;
    }

    public static class Builder {

        private AdditionalPropertiesModel additionalProp1;

        public Builder setAdditionalProp1(AdditionalPropertiesModel additionalProp1) {
            this.additionalProp1 = additionalProp1;
            return this;
        }

        public TokenizePropertiesModel build() { return new TokenizePropertiesModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
