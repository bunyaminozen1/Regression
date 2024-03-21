package opc.models.multi.outgoingwiretransfers;

import lombok.Getter;
import lombok.Setter;
import opc.models.shared.CurrencyAmountResponse;

@Getter
@Setter
public class BulkOutgoingWireTransferResponseModel {

    private String profileId;
    private String tag;
    private CurrencyAmountResponse transferAmount;
    private String description;
    private Object destinationBeneficiary;
    private String id;
    private String scheduledTimestamp;
}
