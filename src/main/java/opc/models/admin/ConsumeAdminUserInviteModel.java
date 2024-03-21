package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.PasswordModel;

@Data
@Builder
public class ConsumeAdminUserInviteModel {

  private String nonce;
  private String email;
  private PasswordModel password;
}
