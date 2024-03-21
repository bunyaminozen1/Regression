package opc.models.innovator;

import opc.enums.opc.KycState;

public class ConsumersFilterModel {
    private final String name;
    private final String surname;
    private final String email;
    private final String mobileNumber;
    private final KycState fullDueDiligence;
    private final String addressLine1;
    private final String addressCountry;
    private final String active;
    private final Long programmeId;
    private final String permanentlyClosed;

    public ConsumersFilterModel(final Builder builder) {
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobileNumber = builder.mobileNumber;
        this.fullDueDiligence = builder.fullDueDiligence;
        this.addressLine1 = builder.addressLine1;
        this.addressCountry = builder.addressCountry;
        this.active = builder.active;
        this.programmeId = builder.programmeId;
        this.permanentlyClosed = builder.permanentlyClosed;
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

    public KycState getFullDueDiligence() {
        return fullDueDiligence;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public String isActive() {
        return active;
    }

    public Long getProgrammeId() {
        return programmeId;
    }
    public String getPermanentlyClosed() {return permanentlyClosed;}

    public static class Builder {
        private String name;
        private String surname;
        private String email;
        private String mobileNumber;
        private KycState fullDueDiligence;
        private String addressLine1;
        private String addressCountry;
        private String active;
        private Long programmeId;
        private String permanentlyClosed;

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

        public Builder setFullDueDiligence(KycState fullDueDiligence) {
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

        public Builder setActive(String active) {
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

        public ConsumersFilterModel build() { return new ConsumersFilterModel(this); }
    }

    public static Builder builder(){ return new Builder(); }
}
