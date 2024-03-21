package opc.models.multi.sends;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import opc.models.shared.CancellationModel;

@Data
@Builder
public class CancelScheduledModel {
  private List<CancellationModel> cancellations;
}