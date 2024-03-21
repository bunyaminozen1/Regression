package commons.models;

import lombok.Builder;
import lombok.Getter;
import opc.models.shared.AddressModel;

@Getter
@Builder
public class PersonalDetailsModel {

    private String name;
    private String surname;
    private String email;
    private AddressModel address;
}
