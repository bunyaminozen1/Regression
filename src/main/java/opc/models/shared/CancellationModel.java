package opc.models.shared;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CancellationModel {
  private String id;
  private String cancellationReason;
}
