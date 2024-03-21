package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateRoleModel {
    private String description;
    private String name;
}
