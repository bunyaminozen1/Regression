package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionsResponseBody {
    private String permissionId;
    private String permissionName;
    private String permissionDescription;
}
