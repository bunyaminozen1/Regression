package opc.models.multi.sends;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkSendFundsModel {
  private List<SendFundsModel> sends;
}
