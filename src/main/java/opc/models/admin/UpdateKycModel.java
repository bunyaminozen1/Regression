package opc.models.admin;

import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.List;

public class UpdateKycModel {

    private final List<LimitDetailsModel> allowedLimits;
    private final String fullDueDiligence;
    private final String fullDueDiligenceAddressMatched;
    private final String pep;
    private final String sanctioned;
    private final List<LimitDetailsModel> remainingLimits;
    private final boolean emailVerified;
    private final boolean mobileVerified;

    public UpdateKycModel(final Builder builder) {
        this.allowedLimits = builder.allowedLimits;
        this.fullDueDiligence = builder.fullDueDiligence;
        this.fullDueDiligenceAddressMatched = builder.fullDueDiligenceAddressMatched;
        this.pep = builder.pep;
        this.sanctioned = builder.sanctioned;
        this.remainingLimits = builder.remainingLimits;
        this.emailVerified = builder.emailVerified;
        this.mobileVerified = builder.mobileVerified;
    }

    public List<LimitDetailsModel> getAllowedLimits() {
        return allowedLimits;
    }

    public String getFullDueDiligence() {
        return fullDueDiligence;
    }

    public String getFullDueDiligenceAddressMatched() {
        return fullDueDiligenceAddressMatched;
    }

    public String getPep() {
        return pep;
    }

    public String getSanctioned() {
        return sanctioned;
    }

    public List<LimitDetailsModel> getRemainingLimits() {
        return remainingLimits;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public static class Builder {
        private List<LimitDetailsModel> allowedLimits;
        private String fullDueDiligence;
        private String fullDueDiligenceAddressMatched;
        private String pep;
        private String sanctioned;
        private List<LimitDetailsModel> remainingLimits;
        private boolean emailVerified;
        private boolean mobileVerified;

        public Builder setAllowedLimits(List<LimitDetailsModel> allowedLimits) {
            this.allowedLimits = allowedLimits;
            return this;
        }

        public Builder setFullDueDiligence(String fullDueDiligence) {
            this.fullDueDiligence = fullDueDiligence;
            return this;
        }

        public Builder setFullDueDiligenceAddressMatched(String fullDueDiligenceAddressMatched) {
            this.fullDueDiligenceAddressMatched = fullDueDiligenceAddressMatched;
            return this;
        }

        public Builder setPep(String pep) {
            this.pep = pep;
            return this;
        }

        public Builder setSanctioned(String sanctioned) {
            this.sanctioned = sanctioned;
            return this;
        }

        public Builder setRemainingLimits(List<LimitDetailsModel> remainingLimits) {
            this.remainingLimits = remainingLimits;
            return this;
        }

        public Builder setEmailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Builder setMobileVerified(boolean mobileVerified) {
            this.mobileVerified = mobileVerified;
            return this;
        }

        public UpdateKycModel build() { return new UpdateKycModel(this);}
    }

    public static Builder builder() { return new Builder();}

    public static Builder approveKycBuilder() {
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
                .setFullDueDiligence("APPROVED")
                .setFullDueDiligenceAddressMatched("NOT_VERIFIED")
                .setPep("APPROVED")
                .setSanctioned("NOT_STARTED")
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
                .setEmailVerified(true)
                .setMobileVerified(true);
    }
}
