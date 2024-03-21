package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class GetThirdPartyRegistriesResponseModel {
    private int count;
    private List<GetThirdPartyRegistryResponseModel> providers;
    private int responseCount;
}
