package opc.models.multi.authfactors;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuthenticationFactorsResponseModel {

    private List<FactorsResponseModel> factors;
}
