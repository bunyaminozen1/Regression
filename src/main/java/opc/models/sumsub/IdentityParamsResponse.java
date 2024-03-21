package opc.models.sumsub;

public class IdentityParamsResponse {

    private IdentityDetailsModel params;
    private IdentityTranslationModel translation;

    public IdentityDetailsModel getParams() {
        return params;
    }

    public void setParams(IdentityDetailsModel params) {
        this.params = params;
    }

    public IdentityTranslationModel getTranslation() {
        return translation;
    }

    public void setTranslation(IdentityTranslationModel translation) {
        this.translation = translation;
    }
}
