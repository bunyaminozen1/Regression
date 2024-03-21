package opc.models.shared;

public class EnrolRootModel {

    private String factorType;

    public EnrolRootModel(final String factorType) {
        this.factorType = factorType;
    }

    public String getFactorType() {
        return factorType;
    }

    public EnrolRootModel setFactorType(final String factorType) {
        this.factorType = factorType;
        return this;
    }
}
