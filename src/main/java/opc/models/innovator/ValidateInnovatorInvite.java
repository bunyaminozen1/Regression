package opc.models.innovator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
@AllArgsConstructor
public class ValidateInnovatorInvite {
    private final String nonce;
    private final String inviteeEmail;
}
