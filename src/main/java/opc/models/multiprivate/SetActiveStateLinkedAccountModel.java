package opc.models.multiprivate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SetActiveStateLinkedAccountModel {
    private final boolean active;

    public static SetActiveStateLinkedAccountModel.SetActiveStateLinkedAccountModelBuilder DefaultSetActiveStateLinkedAccountModel(final boolean state){
        return SetActiveStateLinkedAccountModel.builder()
                .active(state);
    }
}
