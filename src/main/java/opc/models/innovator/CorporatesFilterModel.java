package opc.models.innovator;

import opc.enums.opc.KycState;

public class CorporatesFilterModel {

    private final Long profileId;
    private final String companyName;
    private final Long createdFrom;
    private final Long createdTo;
    private final String rootName;
    private final String rootSurname;
    private final String rootEmail;
    private final String rootMobileNumber;
    private final String companyRegistrationNumber;
    private final String companyRegistrationAddress;
    private final String companyBusinessAddress ;
    private final String registrationCountry;
    private final KycState basicCompanyChecksVerified;
    private final KycState fullCompanyChecksVerified;
    private final KycState enhancedCompanyChecksVerified;
    private final Boolean active;
    private final Long programmeId;
    private final String permanentlyClosed;

    public CorporatesFilterModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.companyName = builder.companyName;
        this.createdFrom = builder.createdFrom;
        this.createdTo = builder.createdTo;
        this.rootName = builder.rootName;
        this.rootSurname = builder.rootSurname;
        this.rootEmail = builder.rootEmail;
        this.rootMobileNumber = builder.rootMobileNumber;
        this.companyRegistrationNumber = builder.companyRegistrationNumber;
        this.companyRegistrationAddress = builder.companyRegistrationAddress;
        this.companyBusinessAddress = builder.companyBusinessAddress;
        this.registrationCountry = builder.registrationCountry;
        this.basicCompanyChecksVerified = builder.basicCompanyChecksVerified;
        this.fullCompanyChecksVerified = builder.fullCompanyChecksVerified;
        this.enhancedCompanyChecksVerified = builder.enhancedCompanyChecksVerified;
        this.active = builder.active;
        this.programmeId = builder.programmeId;
        this.permanentlyClosed = builder.permanentlyClosed;
    }

    public Long getProfileId() {
        return profileId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Long getCreatedFrom() {
        return createdFrom;
    }

    public Long getCreatedTo() {
        return createdTo;
    }

    public String getRootName() {
        return rootName;
    }

    public String getRootSurname() {
        return rootSurname;
    }

    public String getRootEmail() {
        return rootEmail;
    }

    public String getRootMobileNumber() {
        return rootMobileNumber;
    }

    public String getCompanyRegistrationNumber() {
        return companyRegistrationNumber;
    }

    public String getCompanyRegistrationAddress() {
        return companyRegistrationAddress;
    }

    public String getCompanyBusinessAddress() {
        return companyBusinessAddress;
    }

    public String getRegistrationCountry() {
        return registrationCountry;
    }

    public KycState getBasicCompanyChecksVerified() {
        return basicCompanyChecksVerified;
    }

    public KycState getFullCompanyChecksVerified() {
        return fullCompanyChecksVerified;
    }

    public KycState getEnhancedCompanyChecksVerified() {
        return enhancedCompanyChecksVerified;
    }

    public Boolean getActive() {
        return active;
    }

    public Long getProgrammeId() {
        return programmeId;
    }
    public String getPermanentlyClosed() {return permanentlyClosed;}

    public static class Builder {
        private Long profileId;
        private String companyName;
        private Long createdFrom;
        private Long createdTo;
        private String rootName;
        private String rootSurname;
        private String rootEmail;
        private String rootMobileNumber;
        private String companyRegistrationNumber;
        private String companyRegistrationAddress;
        private String companyBusinessAddress ;
        private String registrationCountry;
        private KycState basicCompanyChecksVerified;
        private KycState fullCompanyChecksVerified;
        private KycState enhancedCompanyChecksVerified;
        private Boolean active;
        private Long programmeId;
        private String permanentlyClosed;

        public Builder setProfileId(Long profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setCompanyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder setCreatedFrom(Long createdFrom) {
            this.createdFrom = createdFrom;
            return this;
        }

        public Builder setCreatedTo(Long createdTo) {
            this.createdTo = createdTo;
            return this;
        }

        public Builder setRootName(String rootName) {
            this.rootName = rootName;
            return this;
        }

        public Builder setRootSurname(String rootSurname) {
            this.rootSurname = rootSurname;
            return this;
        }

        public Builder setRootEmail(String rootEmail) {
            this.rootEmail = rootEmail;
            return this;
        }

        public Builder setRootMobileNumber(String rootMobileNumber) {
            this.rootMobileNumber = rootMobileNumber;
            return this;
        }

        public Builder setCompanyRegistrationNumber(String companyRegistrationNumber) {
            this.companyRegistrationNumber = companyRegistrationNumber;
            return this;
        }

        public Builder setCompanyRegistrationAddress(String companyRegistrationAddress) {
            this.companyRegistrationAddress = companyRegistrationAddress;
            return this;
        }

        public Builder setCompanyBusinessAddress(String companyBusinessAddress) {
            this.companyBusinessAddress = companyBusinessAddress;
            return this;
        }

        public Builder setRegistrationCountry(String registrationCountry) {
            this.registrationCountry = registrationCountry;
            return this;
        }

        public Builder setBasicCompanyChecksVerified(KycState basicCompanyChecksVerified) {
            this.basicCompanyChecksVerified = basicCompanyChecksVerified;
            return this;
        }

        public Builder setFullCompanyChecksVerified(KycState fullCompanyChecksVerified) {
            this.fullCompanyChecksVerified = fullCompanyChecksVerified;
            return this;
        }

        public Builder setEnhancedCompanyChecksVerified(KycState enhancedCompanyChecksVerified) {
            this.enhancedCompanyChecksVerified = enhancedCompanyChecksVerified;
            return this;
        }

        public Builder setActive(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder setProgrammeId(Long programmeId) {
            this.programmeId = programmeId;
            return this;
        }

        public Builder setPermanentlyClosed(String permanentlyClosed) {
            this.permanentlyClosed = permanentlyClosed;
            return this;
        }

        public CorporatesFilterModel build() { return new CorporatesFilterModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
