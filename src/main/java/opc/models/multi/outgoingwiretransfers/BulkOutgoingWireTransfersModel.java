package opc.models.multi.outgoingwiretransfers;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkOutgoingWireTransfersModel {
  private List<OutgoingWireTransfersModel> outgoingWireTransfers;
}
