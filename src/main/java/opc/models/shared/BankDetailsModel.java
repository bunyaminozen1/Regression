package opc.models.shared;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class BankDetailsModel {

    private String beneficiary;
    private String address;
    private String beneficiaryBank;
    private String bankIdentifierCode;
    private String iban;
    private String accountNumber;
    private String sortCode;
    private String paymentReference;
}
