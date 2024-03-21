package opc.models.multiprivate;

import lombok.Getter;
import lombok.Setter;
import opc.models.multi.beneficiaries.BankAccountDetailsModel;

@Getter
@Setter
public class RegisterLinkedAccountResponseModel {
    private String id;
    private String profileId;
    private String tag;
    private String friendlyName;
    private String currency;
    private String country;
    private BankAccountDetailsModel accountDetails;
}
