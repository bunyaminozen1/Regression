package opc.models.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InstrumentProfiles {

    private List<FinInstitutionProfileModel> profiles;

}
