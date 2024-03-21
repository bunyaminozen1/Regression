package opc.models.simulator;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import opc.models.shared.CurrencyAmount;

@Data
@Builder
public class SimulatePendingDepositModel {
  private String senderName;
  private String senderIban;
  private CurrencyAmount depositAmount;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String txId;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private boolean webhook;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String reference;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private boolean immediateMonitorReplyExpected;
}
