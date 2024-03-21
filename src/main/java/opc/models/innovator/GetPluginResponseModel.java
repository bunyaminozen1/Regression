package opc.models.innovator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetPluginResponseModel {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String fpiKey;
    private String webhookUrl;
    private String modelId;
}
