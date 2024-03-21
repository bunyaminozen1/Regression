package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetRolesResponseModel {
    private List<RoleResponseModel> roles;
    private Long count;
    private Long responseCount;
}

