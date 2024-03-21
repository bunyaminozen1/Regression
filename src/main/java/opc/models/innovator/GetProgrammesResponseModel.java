package opc.models.innovator;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GetProgrammesResponseModel {
    private String id;
    private String modelId;
    private String code;
    private String name;
    private String ownerInnovatorId;
    private String state;
    private boolean draftModel;
    private int lastPushProduction;
    private Map<String, Integer> connectorUsage;
    private List<String> country;
    private List<String> supportedFeeGroups;
    private String webhookUrl;
    private String webhookDisabled;
    private Map<String, Boolean> securityModelConfig;
    private ScaConfigProgrammeResponseModel scaConfig;
    private boolean authForwardingEnabled;
    private String authForwardingUrl;
    private String tenantExternalId;
    private List<String> jurisdictions;
    private boolean otpLimitVerifyEnabled;
    private int otpNumberOfVerifyAttempts;
    private boolean enableScaAuthUser;
}
