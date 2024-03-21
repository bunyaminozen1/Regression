package opc.models.multi.transfers;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.CancellationModel;

import java.util.List;

@Data
@Builder
public class CancelScheduledModel {
    private List<CancellationModel> cancellations;
}
