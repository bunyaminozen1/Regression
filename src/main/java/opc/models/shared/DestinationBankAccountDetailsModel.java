package opc.models.shared;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.CannedIbanState;
import opc.helpers.ModelHelper;

@Builder
@Getter
@Setter
public class DestinationBankAccountDetailsModel {
    private final String iban;
    private final String bankIdentifierCode;

    public static DestinationBankAccountDetailsModel.DestinationBankAccountDetailsModelBuilder DefaultDestinationBankAccountDetails(){
        return DestinationBankAccountDetailsModel.builder()
                .iban(CannedIbanState.UNKNOWN_SUCCESS.getIban())
                .bankIdentifierCode(ModelHelper.generateRandomValidBankIdentifierNumber());
    }

}
