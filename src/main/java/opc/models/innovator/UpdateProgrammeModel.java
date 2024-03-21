package opc.models.innovator;

import opc.models.admin.ScaConfigModel;

import java.util.Collections;
import java.util.List;

public class UpdateProgrammeModel {

    private final String id;
    private final String name;
    private final String webhookDisabled;
    private final String webhookUrl;
    private final ScaConfigModel scaConfig;
    private final List<String> supportedFeeGroups;
    private final String authForwardingEnabled;
    private final String authForwardingUrl;
    private final List<String> jurisdictions;


    public UpdateProgrammeModel(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.webhookDisabled = builder.webhookDisabled;
        this.webhookUrl = builder.webhookUrl;
        this.scaConfig = builder.scaConfig;
        this.supportedFeeGroups = builder.supportedFeeGroups;
        this.authForwardingEnabled = builder.authForwardingEnabled;
        this.authForwardingUrl = builder.authForwardingUrl;
        this.jurisdictions = builder.jurisdictions;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public String getWebhookDisabled() {
        return webhookDisabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public ScaConfigModel getScaConfig() {
        return scaConfig;
    }

    public List<String> getSupportedFeeGroups() {
        return supportedFeeGroups;
    }

    public String getAuthForwardingEnabled() { return authForwardingEnabled;}

    public String getAuthForwardingUrl() { return authForwardingUrl;}
    public List<String> getJurisdictions() { return jurisdictions;}

    public static class Builder {
        private String id;
        private String name;
        private String webhookDisabled;
        private String webhookUrl;
        private ScaConfigModel scaConfig;
        private List<String> supportedFeeGroups;
        private String authForwardingEnabled;
        private String authForwardingUrl;
        private List<String> jurisdictions;


        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setWebhookDisabled(boolean webhookDisabled) {
            this.webhookDisabled = String.valueOf(webhookDisabled).toUpperCase();
            return this;
        }

        public Builder setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public Builder setScaConfig(ScaConfigModel scaConfig) {
            this.scaConfig = scaConfig;
            return this;
        }

        public Builder setSupportedFeeGroups(List<String> supportedFeeGroups) {
            this.supportedFeeGroups = supportedFeeGroups;
            return this;
        }

        public Builder setAuthForwardingEnabled(boolean authForwardingEnabled) {
            this.authForwardingEnabled = String.valueOf(authForwardingEnabled).toUpperCase();
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

        public UpdateProgrammeModel build(){ return new UpdateProgrammeModel(this); }
    }

    public static Builder builder(){
        return new Builder()
                .setSupportedFeeGroups(Collections.singletonList("DEFAULT"));
    }

    public static UpdateProgrammeModel WebHookUrlSetup(final String programmeId, final boolean webhookDisabled, final String webhookUrl){
        return builder()
                .setId(programmeId)
                .setWebhookDisabled(webhookDisabled)
                .setWebhookUrl(webhookUrl)
                .build();
    }

    public static UpdateProgrammeModel AuthForwardingUrlSetup(final String programmeId,
                                                              final boolean authForwardingEnabled,
                                                              final String authForwardingUrl){
        return builder()
                .setId(programmeId)
                .setAuthForwardingEnabled(authForwardingEnabled)
                .setAuthForwardingUrl(authForwardingUrl)
                .build();
    }
}
