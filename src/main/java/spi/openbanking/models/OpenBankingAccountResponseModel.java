package spi.openbanking.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpenBankingAccountResponseModel {

    private String id;
    private String consentId;
    private String institutionId;
    private String displayName;
    private String currency;
    private Double balance;
    private AccountIdentificationResponseModel accountIdentification;
    private List<String> accountNames;
    private String type;
    private String usageType;
}
