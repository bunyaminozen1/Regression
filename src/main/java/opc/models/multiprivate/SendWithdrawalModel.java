package opc.models.multiprivate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.ManagedInstrumentType;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.DestinationBankAccountDetailsModel;
import opc.models.shared.ManagedInstrumentTypeId;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class SendWithdrawalModel {
    private final String profileId;
    private final ManagedInstrumentTypeId sourceInstrument;
    private final CurrencyAmount transferAmount;
    private final String description;
    private final String destinationBeneficiaryName;
    private final DestinationBankAccountDetailsModel destinationBankAccountDetails;

    public static SendWithdrawalModel.SendWithdrawalModelBuilder DefaultWithdrawalModel(final String profileId,
                                                                                       final String managedAccountId,
                                                                                       final String currency,
                                                                                       final Long amount){
        return SendWithdrawalModel.builder()
                .profileId(profileId)
                .description(RandomStringUtils.randomAlphabetic(5))
                .destinationBeneficiaryName(RandomStringUtils.randomAlphabetic(5))
                .destinationBankAccountDetails(DestinationBankAccountDetailsModel.DefaultDestinationBankAccountDetails().build())
                .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .transferAmount(new CurrencyAmount(currency, amount));
    }
}
