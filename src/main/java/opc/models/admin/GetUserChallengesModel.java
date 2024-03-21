package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.PagingModel;

@Data
@Builder
public class GetUserChallengesModel {
  private PagingModel paging;
  private String challenge_status;
  private String activity;
  private String auth_method;
  private String transaction_id;
  private Long issued_date;

}
