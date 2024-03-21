package opc.models.innovator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class GetProgrammeKeysModel {

    private boolean active;

    public static GetProgrammeKeysModel getActiveKeysModel() {
        return GetProgrammeKeysModel.builder().active(true).build();
    }
}
