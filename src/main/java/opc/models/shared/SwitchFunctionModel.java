package opc.models.shared;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SwitchFunctionModel {
    private Boolean enabled;

    public SwitchFunctionModel(final Boolean enabled){
        this.enabled = enabled;
    }
}
