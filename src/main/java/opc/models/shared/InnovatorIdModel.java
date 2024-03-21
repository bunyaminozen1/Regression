package opc.models.shared;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InnovatorIdModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("TENANT_ID")
    private final String tenantId;

}
