package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AdminSemiToggleModel {
    private String tenantId;
    private Boolean enabled;

    public  AdminSemiToggleModel(String tenantId, Boolean enabled){
        this.tenantId = tenantId;
        this.enabled = enabled;
    }
}
