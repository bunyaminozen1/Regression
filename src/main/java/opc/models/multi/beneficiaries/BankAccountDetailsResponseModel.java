package opc.models.multi.beneficiaries;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountDetailsResponseModel {

    private String bankIdentifierCode;
    private String iban;
}
