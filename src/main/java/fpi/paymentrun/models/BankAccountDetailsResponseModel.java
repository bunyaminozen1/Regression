package fpi.paymentrun.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountDetailsResponseModel {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("iban")
    private String iban;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("bankIdentifierCode")
    private String bankIdentifierCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("accountNumber")
    private String accountNumber;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("sortCode")
    private String sortCode;
}