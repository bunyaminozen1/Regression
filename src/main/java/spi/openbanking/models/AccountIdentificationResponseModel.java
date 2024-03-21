package spi.openbanking.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountIdentificationResponseModel {

    private String accountNumber;
    private String sortCode;
}
