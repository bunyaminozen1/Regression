package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class GetPermissionListResponseModel {
    private String count;
    private String responseCount;
    private List<PermissionsResponseBody> permissions;

}
