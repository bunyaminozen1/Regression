package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeletePropertiesModel {

    @JsonProperty("TENANT_ID")
    private String additionalProperty;
}
