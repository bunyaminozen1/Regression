package opc.models.innovator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import opc.models.shared.PasswordModel;
@Getter
@Data
@AllArgsConstructor
@Builder
public class ConsumeInviteInnovatorModel {
    private final String nonce;
    private final String inviteeEmail;
    private final PasswordModel password;
}
