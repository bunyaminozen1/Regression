package opc.models.admin;

import commons.models.innovator.IdentityProfileAuthenticationModel;

import java.util.List;


public class UpdateCorporateProfileModel {

    private final IdentityProfileAuthenticationModel accountInformationFactors;
    private final List<String> allowedCountries;
    private final boolean hasAllowedCountries;

    public UpdateCorporateProfileModel(final Builder builder) {
        this.accountInformationFactors = builder.accountInformationFactors;
        this.allowedCountries = builder.allowedCountries;
        this.hasAllowedCountries = builder.hasAllowedCountries;
    }

    public IdentityProfileAuthenticationModel getAccountInformationFactors() {
        return accountInformationFactors;
    }
    public List<String> getAllowedCountries() { return allowedCountries; }
    public boolean getHasAllowedCountries() { return hasAllowedCountries; }

    public static class Builder {
        private IdentityProfileAuthenticationModel accountInformationFactors;
        private List<String> allowedCountries;
        private boolean hasAllowedCountries;

        public Builder setAccountInformationFactors(final IdentityProfileAuthenticationModel accountInformationFactors) {
            this.accountInformationFactors = accountInformationFactors;
            return this;
        }

        public Builder setAllowedCountries(final List<String> allowedCountries) {
            this.allowedCountries = allowedCountries;
            return this;
        }

        public Builder setHasAllowedCountries(final boolean hasAllowedCountries) {
            this.hasAllowedCountries = hasAllowedCountries;
            return this;
        }

        public UpdateCorporateProfileModel build() {
            return new UpdateCorporateProfileModel(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
