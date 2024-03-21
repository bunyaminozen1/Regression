package opc.models.admin;

public class CorporatesLimitModel {
    private final PagingLimitModel paging;
    private final Long profileId;
    private final String companyName;
    private final String createdFrom;
    private final String createdTo;
    private final String rootName;
    private final String rootSurname;
    private final String rootEmail;
    private final String rootMobileNumber;
    private final String companyRegistrationNumber;
    private final String companyRegistrationAddress;
    private final String companyBusinessAddress;
    private final String registrationCountry;
    private final String fullCompanyChecksVerified;
    private final boolean active;
    private final Long programmeId;

    public PagingLimitModel getPaging() {
        return paging;
    }

    public Long getProfileId() {
        return profileId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCreatedFrom() {
        return createdFrom;
    }

    public String getCreatedTo() {
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

    public String isFullCompanyChecksVerified() {
        return fullCompanyChecksVerified;
    }

    public boolean isActive() {
        return active;
    }

    public Long getProgrammeId() {
        return programmeId;
    }

    public CorporatesLimitModel(final CorporatesLimitModel.Builder builder) {
        this.paging = builder.paging;
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
        this.fullCompanyChecksVerified = builder.fullCompanyChecksVerified;
        this.active = builder.active;
        this.programmeId = builder.programmeId;

    }

    public static class Builder {
        private PagingLimitModel paging;
        private Long profileId;
        private String companyName;
        private String createdFrom;
        private String createdTo;
        private String rootName;
        private String rootSurname;
        private String rootEmail;
        private String rootMobileNumber;
        private String companyRegistrationNumber;
        private String companyRegistrationAddress;
        private String companyBusinessAddress;
        private String registrationCountry;
        private String fullCompanyChecksVerified;
        private boolean active;
        private Long programmeId;

        public Builder setPaging(PagingLimitModel paging) {
            this.paging = paging;
            return this;
        }

        public Builder setProfileId(Long profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setCompanyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder setCreatedFrom(String createdFrom) {
            this.createdFrom = createdFrom;
            return this;
        }

        public Builder setCreatedTo(String createdTo) {
            this.createdTo = createdTo;
            return this;
        }

        public Builder setRootName(String rootName) {
            this.rootName = rootName;
            return this;
        }

        public Builder setSurname(String rootSurname) {
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

        public Builder setCompanyBusinessAddress(String companyBusinessAddress) {
            this.companyBusinessAddress = companyBusinessAddress;
            return this;
        }

        public Builder setCompanyRegistrationAddress(String companyRegistrationAddress) {
            this.companyRegistrationAddress = companyRegistrationAddress;
            return this;
        }

        public Builder setCompanyRegistrationNumber(String companyRegistrationNumber) {
            this.companyRegistrationNumber = companyRegistrationNumber;
            return this;
        }

        public Builder setRegistrationCountry(String registrationCountry) {
            this.registrationCountry = registrationCountry;
            return this;
        }

        public Builder setFullCompanyChecksVerified(String fullCompanyChecksVerified) {
            this.fullCompanyChecksVerified = fullCompanyChecksVerified;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder setProgrammeId(Long programmeId) {
            this.programmeId = programmeId;
            return this;
        }

        public CorporatesLimitModel build() {
            return new CorporatesLimitModel(this);
        }
    }

    public static CorporatesLimitModel.Builder builder() {
        return new CorporatesLimitModel.Builder();
    }

    public static CorporatesLimitModel getSortedCorporates(int limit, final Long profileId) {
        PagingLimitModel pagingLimitModel = new PagingLimitModel();
        return CorporatesLimitModel.builder()
                .setProfileId(profileId)
                .setActive(true)
                .setPaging(pagingLimitModel.setLimit(limit))
                .setFullCompanyChecksVerified("APPROVED")
                .build();
    }

    public static CorporatesLimitModel getSortedSemiCorporates(int limit, final Long profileId) {
        PagingLimitModel pagingLimitModel = new PagingLimitModel();
        return CorporatesLimitModel.builder()
                .setProfileId(profileId)
                .setActive(true)
                .setPaging(pagingLimitModel.setLimit(limit))
                .setFullCompanyChecksVerified("APPROVED")
                .setCreatedFrom("1676377819479")
                .setCreatedTo("1676450571051")
                .build();
    }


}
