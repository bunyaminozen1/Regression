package opc.models.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public class GetCorporateBeneficiariesModel {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<GetBeneficiaryResponseModel> ubo;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<GetBeneficiaryResponseModel> director;

    public List<GetBeneficiaryResponseModel> getUbo() {
        return ubo;
    }

    public GetCorporateBeneficiariesModel setUbo(List<GetBeneficiaryResponseModel> ubo) {
        this.ubo = ubo;
        return this;
    }

    public List<GetBeneficiaryResponseModel> getDirector() {
        return director;
    }

    public GetCorporateBeneficiariesModel setDirector(List<GetBeneficiaryResponseModel> director) {
        this.director = director;
        return this;
    }
}
