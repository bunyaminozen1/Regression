package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Builder
@Setter
@Getter
public class AddPermissionsToRoleModel {
    private List<String> permissions;

    public static AddPermissionsToRoleModel CreateAddPermissionsToRoleModelBuilder(final String... permissionIds) {

        return AddPermissionsToRoleModel
                .builder()
                .permissions(Arrays.asList(permissionIds))
                .build();
    }
}


