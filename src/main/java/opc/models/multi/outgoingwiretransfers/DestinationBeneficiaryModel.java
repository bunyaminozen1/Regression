package opc.models.multi.outgoingwiretransfers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DestinationBeneficiaryModel {

    private String name;
    private String address;
    private String bankName;
    private String bankAddress;
    private String bankCountry;
    private Object bankAccountDetails;
}
