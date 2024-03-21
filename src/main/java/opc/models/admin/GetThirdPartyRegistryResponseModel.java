package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class GetThirdPartyRegistryResponseModel {
    private String providerKey;
    private String providerName;
    private List<String> services;
}
