package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentsSupplierResponseModel {
    private String name;
    private String address;
    private String bankName;
    private String bankAddress;
    private String bankCountry;
    private BankAccountDetailsResponseModel bankAccountDetails;
}
