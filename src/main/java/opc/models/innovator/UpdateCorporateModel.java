package opc.models.innovator;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.AddressModel;

@Data
@Builder
public class UpdateCorporateModel {
    private String tag;
    private String feeGroup;
    private AddressModel registrationAddress;
    private AddressModel businessAddress;
    private String baseCurrency;
    private String mobileCountryCode;
    private String mobileNumber;
    private Boolean resetMobileCounter;
}
