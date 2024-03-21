package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SwitchModulrSubscriptionFeatureModel {

    private boolean value;

    public SwitchModulrSubscriptionFeatureModel(final boolean value){
        this.value = value;
    }
}
