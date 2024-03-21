package opc.models.simulator;

public class TokenizeModel {
    private TokenizePropertiesModel values;

    public TokenizeModel(final TokenizePropertiesModel values) {
        this.values = values;
    }

    public TokenizePropertiesModel getValues() {
        return values;
    }

    public TokenizeModel setValues(TokenizePropertiesModel values) {
        this.values = values;
        return this;
    }
}
