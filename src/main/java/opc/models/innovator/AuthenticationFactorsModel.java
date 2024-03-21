package opc.models.innovator;

import java.util.List;

public class AuthenticationFactorsModel {

    private List<FactorModel> factors;

    public AuthenticationFactorsModel(List<FactorModel> factors) {
        this.factors = factors;
    }

    public List<FactorModel> getFactors() {
        return factors;
    }

    public AuthenticationFactorsModel setFactors(List<FactorModel> factors) {
        this.factors = factors;
        return this;
    }
}
