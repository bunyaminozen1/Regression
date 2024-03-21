package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AisUxInstitutionResponseModel {
    private List<AisUxInstitutionsResponseModel> institutions;

}
