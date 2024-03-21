package opc.models.innovator;

import commons.config.ConfigHelper;
import commons.enums.PaymentModel;
import opc.enums.opc.CountryCode;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class CreateProgrammeModel {

    private final String code;
    private final String name;
    private List<CountryCode> country;
    private List<String> supportedFeeGroups;
    private Long modelId;
    private Boolean webhookDisabled;
    private String webhookUrl;
    private String tenantExternalId;
    private List<String> jurisdictions;

    public CreateProgrammeModel(final String code, final String name){
        this.code = code;
        this.name = name;
    }

    public CreateProgrammeModel(final Builder builder) {
        this.code = builder.code;
        this.name = builder.name;
        this.country = builder.country;
        this.supportedFeeGroups = builder.supportedFeeGroups;
        this.modelId = builder.modelId;
        this.tenantExternalId = builder.tenantExternalId;
        this.jurisdictions = builder.jurisdictions;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<CountryCode> getCountry() {
        return country;
    }

    public List<String> getSupportedFeeGroups() {
        return supportedFeeGroups;
    }

    public Long getModelId() {
        return modelId;
    }

    public String getTenantExternalId() {
        return tenantExternalId;
    }

    public List<String> getJurisdictions() {
        return jurisdictions;
    }

    public static class Builder {

        private String code;
        private String name;
        private List<CountryCode> country;
        private List<String> supportedFeeGroups;
        private long modelId;
        private String tenantExternalId;
        private List<String> jurisdictions;

        public Builder setTenantExternalId(String tenantExternalId) {
            this.tenantExternalId = tenantExternalId;
            return this;
        }

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setCountry(List<CountryCode> country) {
            this.country = country;
            return this;
        }

        public Builder setSupportedFeeGroups(List<String> supportedFeeGroups) {
            this.supportedFeeGroups = supportedFeeGroups;
            return this;
        }

        public Builder setModelId(long modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder setJurisdictions(List<String> jurisdictions) {
            this.jurisdictions = jurisdictions;
            return this;
        }

        public CreateProgrammeModel build() {
            return new CreateProgrammeModel(this);
        }
    }

    public static CreateProgrammeModel InitialProgrammeModel() {
        final String programmeName = RandomStringUtils.randomAlphabetic(5);
        return new Builder()
                .setCode(programmeName)
                .setName(programmeName)
                .setModelId(ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment().equals("dev") ? 21 : 32)
                .build();
    }

    public static CreateProgrammeModel InitialProgrammeModel(final String programmeName,
                                                             final PaymentModel paymentModel,
                                                             final String jurisdiction) {
        return new Builder()
                .setCode(programmeName)
                .setName(programmeName)
                .setModelId(ConfigHelper.getEnvironmentConfiguration().getPaymentModelId(paymentModel))
                .setJurisdictions(List.of(jurisdiction))
                .build();
    }

    public static CreateProgrammeModel ProgrammeWithExternalIdModel() {
        final String programmeName = RandomStringUtils.randomAlphabetic(5);
        return new Builder()
                .setCode(programmeName)
                .setName(programmeName)
                .setModelId(ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment().equals("dev") ? 21 : 32)
                .setTenantExternalId(RandomStringUtils.randomAlphabetic(8))
                .build();
    }

    public static CreateProgrammeModel ProgrammeWithExternalIdModel(String tenantExternalId) {
        final String programmeName = RandomStringUtils.randomAlphabetic(5);
        return new Builder()
                .setCode(programmeName)
                .setName(programmeName)
                .setModelId(ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment().equals("dev") ? 21 : 32)
                .setTenantExternalId(tenantExternalId)
                .build();
    }

    public static CreateProgrammeModel ProgrammeWithoutModelId() {
        final String programmeName = RandomStringUtils.randomAlphabetic(5);
        return new Builder()
                .setCode(programmeName)
                .setName(programmeName)
                .build();
    }
}
