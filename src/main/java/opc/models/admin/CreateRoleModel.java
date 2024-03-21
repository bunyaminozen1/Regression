package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateRoleModel {
    private String description;
    private String name;
    private List<String> permissions;

    public static CreateRoleModel DefaultCreateRoleModel(final String... permissions) {
        return CreateRoleModel
                .builder()
                .description(RandomStringUtils.randomAlphabetic(8))
                .name(RandomStringUtils.randomAlphabetic(8))
                .permissions(Arrays.asList(permissions))
                .build();

    }
}
