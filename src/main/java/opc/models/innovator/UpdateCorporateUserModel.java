package opc.models.innovator;

public class UpdateCorporateUserModel {

    private final String title;
    private final String name;
    private final String surname;
    private final String email;
    private final String mobileNumber;
    private final String mobileCountryCode;
    private final String companyPosition;

    public UpdateCorporateUserModel(final Builder builder) {
        this.title = builder.title;
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobileNumber = builder.mobileNumber;
        this.mobileCountryCode = builder.mobileCountryCode;
        this.companyPosition = builder.companyPosition;
    }

    public String getTitle() {
        return title;
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

    public String getMobileCountryCode() {
        return mobileCountryCode;
    }

    public String getCompanyPosition() {
        return companyPosition;
    }

    public static class Builder {
        private String title;
        private String name;
        private String surname;
        private String email;
        private String mobileNumber;
        private String mobileCountryCode;
        private String companyPosition;

        public Builder setTitle(String title) {
            this.title = title;
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

        public Builder setMobileCountryCode(String mobileCountryCode) {
            this.mobileCountryCode = mobileCountryCode;
            return this;
        }

        public Builder setCompanyPosition(String companyPosition) {
            this.companyPosition = companyPosition;
            return this;
        }

        public UpdateCorporateUserModel build() { return new UpdateCorporateUserModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
