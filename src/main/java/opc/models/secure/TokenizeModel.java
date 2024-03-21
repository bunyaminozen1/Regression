package opc.models.secure;

public class TokenizeModel {

    private final String random;
    private final TokenizePropertiesModel values;

    public TokenizeModel(final Builder builder) {
        this.random = builder.random;
        this.values = builder.values;
    }

    public String getRandom() {
        return random;
    }

    public TokenizePropertiesModel getValues() {
        return values;
    }

    public static class Builder {
        private String random;
        private TokenizePropertiesModel values;

        public Builder setRandom(String random) {
            this.random = random;
            return this;
        }

        public Builder setValues(TokenizePropertiesModel values) {
            this.values = values;
            return this;
        }

        public TokenizeModel build() { return new TokenizeModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
