package opc.models.admin;

public class ScaConfigModel {

    private String enableScaSends;
    private String enableLowValueExemption;

    public ScaConfigModel(boolean enableScaSends, boolean enableLowValueExemption) {
        this.enableScaSends = String.valueOf(enableScaSends).toUpperCase();
        this.enableLowValueExemption = String.valueOf(enableLowValueExemption).toUpperCase();
    }

    public ScaConfigModel(final Builder builder) {
        this.enableScaSends = String.valueOf(builder.enableScaSends).toUpperCase();
        this.enableLowValueExemption = String.valueOf(builder.enableLowValueExemption).toUpperCase();
    }

    public String getEnableScaSends() {
        return enableScaSends;
    }

    public String getEnableLowValueExemption() {
        return enableLowValueExemption;
    }

    public static class Builder{
        private String enableScaSends;
        private String enableLowValueExemption;

        public ScaConfigModel.Builder setEnableScaSends(boolean enableScaSends) {
            this.enableScaSends = String.valueOf(enableScaSends).toUpperCase();
            return this;
        }

        public ScaConfigModel.Builder setEnableLowValueExemption(boolean enableLowValueExemption) {
            this.enableLowValueExemption = String.valueOf(enableLowValueExemption).toUpperCase();
            return this;
        }

        public ScaConfigModel build(){return new ScaConfigModel(this);}
    }

    public static Builder builder() { return new Builder(); }
}
