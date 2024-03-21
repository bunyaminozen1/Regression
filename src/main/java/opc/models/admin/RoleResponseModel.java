package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class RoleResponseModel {
    private String id;
    private String name;
    private String description;
    private List<PermissionsResponseBody> permissions=null;
}
