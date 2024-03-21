package opc.models.multi.beneficiaries;

import lombok.Getter;
import lombok.Setter;
import opc.models.shared.TypeIdResponseModel;

@Getter
@Setter
public class BeneficiaryDetailsResponseModel {

    private String address;
    private BankAccountDetailsResponseModel bankAccountDetails;
    private String bankAddress;
    private String bankCountry;
    private String bankName;
    private TypeIdResponseModel instrument;
}
