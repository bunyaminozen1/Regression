package opc.models.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProfileFactors {

    private List<FactorNameModel> primaryFactors;
    private List<FactorNameModel> secondaryFactors;
}
