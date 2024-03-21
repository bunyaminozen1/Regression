package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;

@Data
@Builder
public class SendAdminUserInviteModel {

    private String friendlyName;
    private String email;
    private String role;

    public static SendAdminUserInviteModel defaultUserInviteModel (){
      return SendAdminUserInviteModel.builder()
          .friendlyName(RandomStringUtils.randomAlphabetic(5))
          .email(String.format("%sadmin@weavrtest.com", RandomStringUtils.randomAlphabetic(5)))
          .role("ADMIN_OPERATOR")
          .build();
    }
}
