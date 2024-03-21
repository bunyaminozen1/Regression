package opc.models.sumsub;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;

public class CompanyInfoModel {

    private final String companyName;
    private final String registrationNumber;
    private final String country;
    private final String legalAddress;
    private final String phone;
    private final String incorporatedOn;
    private final CompanyAddressModel address;
    private final String type;
    private final String taxId;
    private final String website;

    public CompanyInfoModel(final Builder builder) {
        this.companyName = builder.companyName;
        this.registrationNumber = builder.registrationNumber;
        this.country = builder.country;
        this.legalAddress = builder.legalAddress;
        this.phone = builder.phone;
        this.incorporatedOn = builder.incorporatedOn;
        this.address = builder.address;
        this.type = builder.type;
        this.taxId = builder.taxId;
        this.website = builder.website;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getCountry() {
        return country;
    }

    public String getLegalAddress() {
        return legalAddress;
    }

    public String getPhone() {
        return phone;
    }

    public String getIncorporatedOn() {
        return incorporatedOn;
    }

    public CompanyAddressModel getAddress() {
        return address;
    }

    public String getType() {
        return type;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getWebsite() {
        return website;
    }

    public static class Builder {
        private String companyName;
        private String registrationNumber;
        private String country;
        private String legalAddress;
        private String phone;
        private String incorporatedOn;
        private CompanyAddressModel address;
        private String type;
        private String taxId;
        private String website;

        public Builder setCompanyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder setRegistrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setLegalAddress(String legalAddress) {
            this.legalAddress = legalAddress;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setIncorporatedOn(String incorporatedOn) {
            this.incorporatedOn = incorporatedOn;
            return this;
        }

        public Builder setAddress(CompanyAddressModel address) {
            this.address = address;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setTaxId(String taxId) {
            this.taxId = taxId;
            return this;
        }

        public Builder setWebsite(String website) {
            this.website = website;
            return this;
        }

        public CompanyInfoModel build(){ return new CompanyInfoModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static Builder defaultCompanyInfoModel(final SumSubCompanyInfoModel sumSubCompanyInfoModel){
        return new Builder()
                .setCompanyName(sumSubCompanyInfoModel.getCompanyName())
                .setRegistrationNumber(sumSubCompanyInfoModel.getRegistrationNumber())
                .setCountry(sumSubCompanyInfoModel.getCountry())
                .setLegalAddress("MLT")
                .setPhone(sumSubCompanyInfoModel.getPhone())
                .setIncorporatedOn("2021-01-01")
                .setAddress(new CompanyAddressModel("Test", "DEU", "MLT01"));
    }

    public static Builder randomCompanyInfoModel(){
        return new Builder()
                .setCompanyName(RandomStringUtils.randomAlphabetic(5))
                .setRegistrationNumber(RandomStringUtils.randomAlphanumeric(5))
                .setCountry("MLT")
                .setLegalAddress("MLT")
                .setPhone(String.format("+35679%s", RandomStringUtils.randomNumeric(6)))
                .setIncorporatedOn(LocalDate.now().minusDays(1).toString())
                .setAddress(new CompanyAddressModel(RandomStringUtils.randomAlphabetic(5), "DEU", "MLT01"));
    }
}
