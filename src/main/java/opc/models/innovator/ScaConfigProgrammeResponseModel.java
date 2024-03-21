package opc.models.innovator;

public class ScaConfigProgrammeResponseModel {
    private Boolean enableScaSends;
    private Boolean enableLowValueExemption;
    private Boolean enableScaAuthUser;

    public Boolean getEnableScaSends() {
        return enableScaSends;
    }

    public ScaConfigProgrammeResponseModel setEnableScaSends(Boolean enableScaSends) {
        this.enableScaSends = enableScaSends;
        return this;
    }

    public Boolean getEnableLowValueExemption() {
        return enableLowValueExemption;
    }

    public ScaConfigProgrammeResponseModel setEnableLowValueExemption(Boolean enableLowValueExemption) {
        this.enableLowValueExemption = enableLowValueExemption;
        return this;
    }

    public Boolean getEnableScaAuthUser() {
        return enableScaAuthUser;
    }

    public ScaConfigProgrammeResponseModel setEnableScaAuthUser(Boolean enableScaAuthUser) {
        this.enableScaAuthUser = enableScaAuthUser;
        return this;
    }
}
