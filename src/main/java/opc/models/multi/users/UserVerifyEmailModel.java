package opc.models.multi.users;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVerifyEmailModel {
  private String email;
  private String verificationCode;
}
