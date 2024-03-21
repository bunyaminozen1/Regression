package opc.models.admin;

import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.List;

public class UpdateKybModel {

    private final List<LimitDetailsModel> allowedLimits;
    private final String basicCompanyChecksVerified;
    private final String enhancedCompanyChecksVerified;
    private final String fullCompanyChecksVerified;
    private final String ongoingStatus;
    private final List<LimitDetailsModel> remainingLimits;
    private final boolean rootEmailVerified;
    private final boolean rootMobileVerified;

    public UpdateKybModel(final Builder builder) {
        this.allowedLimits = builder.allowedLimits;
        this.basicCompanyChecksVerified = builder.basicCompanyChecksVerified;
        this.enhancedCompanyChecksVerified = builder.enhancedCompanyChecksVerified;
        this.fullCompanyChecksVerified = builder.fullCompanyChecksVerified;
        this.ongoingStatus = builder.ongoingStatus;
        this.remainingLimits = builder.remainingLimits;
        this.rootEmailVerified = builder.rootEmailVerified;
        this.rootMobileVerified = builder.rootMobileVerified;
    }

    public List<LimitDetailsModel> getAllowedLimits() {
        return allowedLimits;
    }

    public String getBasicCompanyChecksVerified() {
        return basicCompanyChecksVerified;
    }

    public String getEnhancedCompanyChecksVerified() {
        return enhancedCompanyChecksVerified;
    }

    public String getFullCompanyChecksVerified() {
        return fullCompanyChecksVerified;
    }

    public String getOngoingStatus() {
        return ongoingStatus;
    }

    public List<LimitDetailsModel> getRemainingLimits() {
        return remainingLimits;
    }

    public boolean isRootEmailVerified() {
        return rootEmailVerified;
    }

    public boolean isRootMobileVerified() {
        return rootMobileVerified;
    }

    public static class Builder {
        private List<LimitDetailsModel> allowedLimits;
        private String basicCompanyChecksVerified;
        private String enhancedCompanyChecksVerified;
        private String fullCompanyChecksVerified;
        private String ongoingStatus;
        private List<LimitDetailsModel> remainingLimits;
        private boolean rootEmailVerified;
        private boolean rootMobileVerified;

        public Builder setAllowedLimits(List<LimitDetailsModel> allowedLimits) {
            this.allowedLimits = allowedLimits;
            return this;
        }

        public Builder setBasicCompanyChecksVerified(String basicCompanyChecksVerified) {
            this.basicCompanyChecksVerified = basicCompanyChecksVerified;
            return this;
        }

        public Builder setEnhancedCompanyChecksVerified(String enhancedCompanyChecksVerified) {
            this.enhancedCompanyChecksVerified = enhancedCompanyChecksVerified;
            return this;
        }

        public Builder setFullCompanyChecksVerified(String fullCompanyChecksVerified) {
            this.fullCompanyChecksVerified = fullCompanyChecksVerified;
            return this;
        }

        public Builder setOngoingStatus(String ongoingStatus) {
            this.ongoingStatus = ongoingStatus;
            return this;
        }

        public Builder setRemainingLimits(List<LimitDetailsModel> remainingLimits) {
            this.remainingLimits = remainingLimits;
            return this;
        }

        public Builder setRootEmailVerified(boolean rootEmailVerified) {
            this.rootEmailVerified = rootEmailVerified;
            return this;
        }

        public Builder setRootMobileVerified(boolean rootMobileVerified) {
            this.rootMobileVerified = rootMobileVerified;
            return this;
        }

        public UpdateKybModel build() { return new UpdateKybModel(this);}
    }

    public static Builder builder() { return new Builder();}

    public static Builder approveKybBuilder() {
        return new Builder()
                .setAllowedLimits(Arrays.asList(LimitDetailsModel.builder()
                                .setLimit(new CurrencyAmount("GBP", 0L))
                                .setCounterValue("0")
                                .setName("Velocity Limit")
                                .build(),
                        LimitDetailsModel.builder()
                                .setLimit(new CurrencyAmount("GBP", 0L))
                                .setCounterValue("0")
                                .setName("Cumulative Limit")
                                .build()))
                .setBasicCompanyChecksVerified("APPROVED")
                .setEnhancedCompanyChecksVerified("NOT_VERIFIED")
                .setFullCompanyChecksVerified("APPROVED")
                .setOngoingStatus("NOT_STARTED")
                .setRemainingLimits(Arrays.asList(LimitDetailsModel.builder()
                                .setLimit(new CurrencyAmount("GBP", 0L))
                                .setCounterValue("0")
                                .setName("Velocity Limit")
                                .build(),
                        LimitDetailsModel.builder()
                                .setLimit(new CurrencyAmount("GBP", 0L))
                                .setCounterValue("0")
                                .setName("Cumulative Limit")
                                .build()))
                .setRootEmailVerified(true)
                .setRootMobileVerified(true);
    }
}
