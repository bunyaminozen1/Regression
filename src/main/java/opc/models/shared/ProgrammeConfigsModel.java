package opc.models.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProgrammeConfigsModel {

    private Boolean authForwardingEnabled;
    private String programmeCode;
    private String programmeName;
    private ScaConfigProgrammeModel scaConfig;
    private Map<String, Boolean> securityModelConfig;
    private String state;
    private List<String> supportedFeeGroups;
    private Boolean webhookDisabled;
    private Boolean authyEnabled;
    private Boolean biometricEnabled;
    private Boolean scaMaEnabled;
    private Boolean scaMcEnabled;
    private Boolean txmEnabled;
    private Boolean scaEnrolEnabled;
    private Boolean trustBiometrics;
    private InstrumentProfiles corporateManagedAccountProfiles;
    private InstrumentProfiles consumerManagedAccountProfiles;
    private String authenticationType;
    private String paymentModel;
    private InstrumentProfiles zeroBalanceManagedAccountProfiles;
    private InstrumentProfiles corporateManagedCardProfiles;
    private InstrumentProfiles consumerManagedCardProfiles;
    private ProfileFactors accountInformationFactors;
    private ProfileFactors paymentInitiationFactors;
    private ProfileFactors beneficiaryManagementFactors;
    private String jurisdiction;
}
