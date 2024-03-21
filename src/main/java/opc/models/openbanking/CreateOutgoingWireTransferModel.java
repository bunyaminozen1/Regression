package opc.models.openbanking;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import org.apache.commons.lang3.RandomStringUtils;

@Data
@Builder
@Getter
public class CreateOutgoingWireTransferModel {

    private final String profileId;
    private final String tag;
    private final ManagedInstrumentTypeId sourceInstrument;
    private final CurrencyAmount transferAmount;
    private final String description;
    private final Beneficiary destinationBeneficiary;

    public static CreateOutgoingWireTransferModelBuilder DefaultOutgoingWireTransfersModel(final String profileId,
                                                                                           final String managedAccountId,
                                                                                           final String currency,
                                                                                           final Long amount,
                                                                                           final OwtType owtType){
        return CreateOutgoingWireTransferModel.builder()
                .profileId(profileId)
                .tag(RandomStringUtils.randomAlphabetic(5))
                .description(RandomStringUtils.randomAlphabetic(5))
                .destinationBeneficiary(owtType.equals(OwtType.SEPA) ?
                        Beneficiary.DefaultBeneficiaryWithSepa().build() :
                        Beneficiary.DefaultBeneficiaryWithFasterPayments().build())
                .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .transferAmount(new CurrencyAmount(currency, amount));
    }
}
