package opc.models.innovator;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.AddressModel;
import opc.models.shared.DateOfBirthResponseModel;

@Data
@Builder
public class UpdateConsumerModel {

    private String title;
    private String name;
    private String surname;
    private DateOfBirthResponseModel dateOfBirth;
    private String email;
    private String mobileCountryCode;
    private String mobileNumber;
    private Boolean resetMobileCounter;
    private String placeOfBirth;
    private AddressModel address;
    private String feeGroup;
    private String baseCurrency;
    private String tag;
    private String nationality;
}
