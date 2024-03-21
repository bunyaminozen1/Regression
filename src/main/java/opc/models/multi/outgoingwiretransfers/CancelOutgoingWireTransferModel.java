package opc.models.multi.outgoingwiretransfers;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.CancellationModel;

import java.util.List;

@Data
@Builder
public class CancelOutgoingWireTransferModel {
  private List<CancellationModel> cancellations;
}
