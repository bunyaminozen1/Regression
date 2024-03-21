package opc.models.multi.outgoingwiretransfers;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkOutgoingWireTransfersResponseModel {

    private List<BulkOutgoingWireTransferResponseModel> response;
}
