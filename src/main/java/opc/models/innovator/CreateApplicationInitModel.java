package opc.models.innovator;

public class CreateApplicationInitModel {

    private final String programmeCode;
    private final String programmeName;
    private final String type;

    public CreateApplicationInitModel(final Builder builder) {
        this.programmeCode = builder.programmeCode;
        this.programmeName = builder.programmeName;
        this.type = builder.type;
    }

    public String getProgrammeCode() {
        return programmeCode;
    }

    public String getProgrammeName() {
        return programmeName;
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        private String programmeCode;
        private String programmeName;
        private String type;

        public Builder setProgrammeCode(String programmeCode) {
            this.programmeCode = programmeCode;
            return this;
        }

        public Builder setProgrammeName(String programmeName) {
            this.programmeName = programmeName;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public CreateApplicationInitModel build() { return new CreateApplicationInitModel(this); }
    }

    public static CreateApplicationInitModel createConsumerPaymentsModel(final String applicationName) {
        return new Builder()
                .setProgrammeCode(applicationName)
                .setProgrammeName(applicationName)
                .setType("CONSUMER_PAYMENTS")
                .build();
    }

    public static CreateApplicationInitModel createBusinessPurchasingModel(final String applicationName) {
        return new Builder()
                .setProgrammeCode(applicationName)
                .setProgrammeName(applicationName)
                .setType("BUSINESS_PURCHASING")
                .build();
    }

    public static CreateApplicationInitModel createBusinessPayoutsModel(final String applicationName) {
        return new Builder()
                .setProgrammeCode(applicationName)
                .setProgrammeName(applicationName)
                .setType("BUSINESS_PAYOUTS")
                .build();
    }
}
