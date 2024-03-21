package opc.models.multi.outgoingwiretransfers;

import lombok.Getter;
import lombok.Setter;
import opc.models.shared.CurrencyAmountResponse;
import opc.models.shared.TypeIdResponseModel;

@Getter
@Setter
public class OutgoingWireTransferResponseModel {

    private String profileId;
    private String tag;
    private CurrencyAmountResponse transferAmount;
    private String description;
    private String id;
    private String creationTimestamp;
    private String type;
    private String state;
    private TypeIdResponseModel sourceInstrument;
    private Object destination;
}
