package opc.models.innovator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import opc.enums.opc.InnovatorRole;
import org.apache.commons.lang3.RandomStringUtils;

@Data
@Builder
@Getter
@AllArgsConstructor

public class InviteInnovatorUserModel {
  private String name;
  private String surname;
  private String email;
  private String role;

  public static InviteInnovatorUserModel defaultInviteUserModel (){
    return InviteInnovatorUserModel.builder()
        .name(RandomStringUtils.randomAlphabetic(5))
        .surname(RandomStringUtils.randomAlphabetic(5))
        .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphabetic(10)))
        .role("INNOVATOR_OPERATOR")
        .build();
  }
  public static InviteInnovatorUserModel defaultInviteUserModel (final InnovatorRole innovatorRole){
    return InviteInnovatorUserModel.builder()
            .name(RandomStringUtils.randomAlphabetic(5))
            .surname(RandomStringUtils.randomAlphabetic(5))
            .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphabetic(10)))
            .role(innovatorRole.getInnovatorRole())
            .build();
  }
}
