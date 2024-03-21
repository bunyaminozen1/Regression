package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UpdateServiceTypeModel {

    private boolean active;

    public static UpdateServiceTypeModel.UpdateServiceTypeModelBuilder defaultUpdateServiceTypeModel() {
        return UpdateServiceTypeModel
                .builder()
                .active(false);
    }

}
