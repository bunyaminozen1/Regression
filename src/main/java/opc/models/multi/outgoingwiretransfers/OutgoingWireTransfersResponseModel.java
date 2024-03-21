package opc.models.multi.outgoingwiretransfers;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OutgoingWireTransfersResponseModel {

    private int count;
    private int responseCount;
    private List<OutgoingWireTransferResponseModel> transfer;
}
