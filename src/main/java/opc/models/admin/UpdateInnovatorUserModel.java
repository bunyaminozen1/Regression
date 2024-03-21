package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateInnovatorUserModel {

  private String name;
  private String surname;
  private String email;
  private String active;

}
