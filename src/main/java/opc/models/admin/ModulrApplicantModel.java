package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import opc.models.shared.AddressModel;

@Data
@Builder
@Getter
@Setter
public class ModulrApplicantModel {

    private String name;
    private String surname;
    private String mobileCountryCode;
    private String mobileNumber;
    private DateOfBirthModel dateOfBirth;
    private String nationality;
    private AddressModel address;
    private String type;
    private String emailAddress;
    private String phoneCountryCode;
    private String phoneNumber;
    private String ownership;
}
