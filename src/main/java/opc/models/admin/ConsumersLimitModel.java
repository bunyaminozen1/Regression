package opc.models.admin;

public class ConsumersLimitModel {
    private final PagingLimitModel paging;
    private final String name;
    private final String surname;
    private final String email;
    private final String mobileNumber;
    private final String fullDueDiligence;
    private final String addressLine1;
    private final String addressCountry;
    private final boolean active;
    private final Long programmeId;

    public ConsumersLimitModel(final Builder builder) {
        this.paging = builder.paging;
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobileNumber = builder.mobileNumber;
        this.fullDueDiligence = builder.fullDueDiligence;
        this.addressLine1 = builder.addressLine1;
        this.addressCountry = builder.addressCountry;
        this.active = builder.active;
        this.programmeId = builder.programmeId;

    }

    public PagingLimitModel getPaging() {
        return paging;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getFullDueDiligence() {
        return fullDueDiligence;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public boolean getActive() {
        return active;
    }

    public Long getProgrammeId() {
        return programmeId;
    }

    public static class Builder {
        private PagingLimitModel paging;
        private String name;
        private String surname;
        private String email;
        private String mobileNumber;
        private String fullDueDiligence;
        private String addressLine1;
        private String addressCountry;
        private boolean active;
        private Long programmeId;

        public Builder setPaging(PagingLimitModel paging) {
            this.paging = paging;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public Builder setFullDueDiligence(String fullDueDiligence) {
            this.fullDueDiligence = fullDueDiligence;
            return this;
        }

        public Builder setAddressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public Builder setAddressCountry(String addressCountry) {
            this.addressCountry = addressCountry;
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

        public ConsumersLimitModel build() {
            return new ConsumersLimitModel(this);
        }
    }

    public static ConsumersLimitModel.Builder builder() {
        return new ConsumersLimitModel.Builder();
    }

    public static ConsumersLimitModel getSortedConsumers(int limit, final Long programmeId) {
        PagingLimitModel pagingLimitModel = new PagingLimitModel();
        return ConsumersLimitModel.builder()
                .setProgrammeId(programmeId)
                .setActive(builder().active)
                .setPaging(pagingLimitModel.setLimit(limit))
                .build();
    }
}
