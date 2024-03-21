package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkedAccountResponseModel {
    private String id;
    private BankAccountDetailsResponseModel accountIdentification;
    private String currency;
    private InstitutionResponseModel institution;
    private ConsentResponseModel consent;
}
