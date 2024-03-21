package opc.models.multi.authfactors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FactorsResponseModel {

    private String channel;
    private String status;
    private String type;
}
