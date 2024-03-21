package opc.models.admin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UpdateProgrammeModel {
    private final String name;
    private final boolean hasCountry;
    private final List<String> country;
    private final List<String> supportedFeeGroups;
    private final String webhookUrl;
    private final boolean webhookDisabled;
    private final Map<String, Boolean> securityModelConfig;
    private final String state;

    private final ScaConfigModel scaConfig;
    private final boolean authForwardingEnabled;
    private final String authForwardingUrl;

    private final List<String> jurisdictions;

    public UpdateProgrammeModel(final Builder builder) {
        this.name = builder.name;
        this.hasCountry = builder.hasCountry;
        this.country = builder.country;
        this.supportedFeeGroups = builder.supportedFeeGroups;
        this.webhookUrl = builder.webhookUrl;
        this.webhookDisabled = builder.webhookDisabled;
        this.securityModelConfig = builder.securityModelConfig;
        this.state = builder.state;
        this.scaConfig = builder.scaConfig;
        this.authForwardingEnabled = builder.authForwardingEnabled;
        this.authForwardingUrl = builder.authForwardingUrl;
        this.jurisdictions = builder.jurisdictions;
    }

    public String getName() {
        return name;
    }

    public boolean isHasCountry() {
        return hasCountry;
    }

    public List<String> getCountry() {
        return country;
    }

    public List<String> getSupportedFeeGroups() {
        return supportedFeeGroups;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean isWebhookDisabled() {
        return webhookDisabled;
    }

    public Map<String, Boolean> getSecurityModelConfig() {
        return securityModelConfig;
    }

    public String getState() {
        return state;
    }

    public ScaConfigModel getScaConfig() {
        return scaConfig;
    }

    public boolean isAuthForwardingEnabled() { return authForwardingEnabled;}

    public String getAuthForwardingUrl() { return authForwardingUrl;}
    public List<String> getJurisdictions() { return jurisdictions;}

    public static class Builder {
        private String name;
        private boolean hasCountry;
        private List<String> country;
        private List<String> supportedFeeGroups;
        private String webhookUrl;
        private boolean webhookDisabled;
        private Map<String, Boolean> securityModelConfig;
        private String state;
        private ScaConfigModel scaConfig;
        private boolean authForwardingEnabled;
        private String authForwardingUrl;
        private List<String> jurisdictions;


        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setHasCountry(boolean hasCountry) {
            this.hasCountry = hasCountry;
            return this;
        }

        public Builder setCountry(List<String> country) {
            this.country = country;
            return this;
        }

        public Builder setSupportedFeeGroups(List<String> supportedFeeGroups) {
            this.supportedFeeGroups = supportedFeeGroups;
            return this;
        }

        public Builder setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public Builder setWebhookDisabled(boolean webhookDisabled) {
            this.webhookDisabled = webhookDisabled;
            return this;
        }

        public Builder setSecurityModelConfig(Map<String, Boolean> securityModelConfig) {
            this.securityModelConfig = securityModelConfig;
            return this;
        }

        public Builder setState(String state) {
            this.state = state;
            return this;
        }

        public Builder setScaConfig(ScaConfigModel scaConfig) {
            this.scaConfig = scaConfig;
            return this;
        }

        public Builder setAuthForwardingEnabled(boolean authForwardingEnabled) {
            this.authForwardingEnabled = authForwardingEnabled;
            return this;
        }

        public Builder setAuthForwardingUrl(String authForwardingUrl) {
            this.authForwardingUrl = authForwardingUrl;
            return this;
        }

        public Builder setJurisdictions(List<String> jurisdictions) {
            this.jurisdictions = jurisdictions;
            return this;
        }

        public UpdateProgrammeModel build() { return new UpdateProgrammeModel(this); }
    }

    public static Builder builder() { return new Builder().setSupportedFeeGroups(Collections.singletonList("DEFAULT")); }
}
