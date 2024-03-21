package spi.openbanking.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PayeeModel {

    private String sortCode;
    private String accountNumber;
    private String name;
}
