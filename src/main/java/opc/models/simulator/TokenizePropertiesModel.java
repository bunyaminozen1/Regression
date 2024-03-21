package opc.models.simulator;

public class TokenizePropertiesModel {

    private AdditionalPropertiesModel additionalProp1;

    public TokenizePropertiesModel(final AdditionalPropertiesModel additionalProp1) {
        this.additionalProp1 = additionalProp1;
    }

    public AdditionalPropertiesModel getAdditionalProp1() {
        return additionalProp1;
    }

    public TokenizePropertiesModel setAdditionalProp1(AdditionalPropertiesModel additionalProp1) {
        this.additionalProp1 = additionalProp1;
        return this;
    }
}
