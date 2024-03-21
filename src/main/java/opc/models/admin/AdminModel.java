package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class AdminModel {
    private String id;
    private String friendlyName;
    private String email;

    public static AdminModel DefaultAdminModelBuilder (final String id) {
        return AdminModel.builder()
                .id(id)
                .friendlyName(RandomStringUtils.randomAlphabetic(5))
                .build();

    }
}
