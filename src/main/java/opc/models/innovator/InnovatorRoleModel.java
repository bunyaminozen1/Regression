package opc.models.innovator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import opc.enums.opc.InnovatorRole;
@AllArgsConstructor
@Data
@Getter
@Builder
public class InnovatorRoleModel {
    private final String role;
}
