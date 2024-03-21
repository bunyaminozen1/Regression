package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Builder
@Getter
@Setter
public class AssignAdminsToRoleModel {
    private List<String> admins;

    public static AssignAdminsToRoleModel CreateAssignAdminsToRoleModelBuilder(final String... id) {

        return AssignAdminsToRoleModel
                .builder()
                .admins(Arrays.asList(id))
                .build();
    }
}
