package opc.models.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Builder
@Getter
@Setter
public class AssignRoleModel {
    private Long roleId;

    public AssignRoleModel(Long roleId) {
        this.roleId = roleId;
    }

    @SneakyThrows
    public static String assignRoleModelString(final String roleId) {
        return new ObjectMapper().writeValueAsString(new AssignRoleModel(Long.valueOf(roleId)));
    }
}
