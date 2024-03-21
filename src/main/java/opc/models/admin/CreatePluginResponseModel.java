package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePluginResponseModel {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String status;
    private String icon;
    private String fpiKey;
    private String webhookUrl;
    private String modelId;
    private String adminUrl;
}