package opc.models.sumsub;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class FixedInfoModel {

    private final String firstName;
    private final String lastName;
    private final String dob;
    private final String country;
    private final String nationality;
    private final String phone;
    private final List<SumSubAddressModel> addresses;
    private final String placeOfBirth;

    private final CompanyInfoModel companyInfo;

    public CompanyInfoModel getCompanyInfo() {
        return companyInfo;
    }

    public FixedInfoModel(final Builder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.dob = builder.dob;
        this.country = builder.country;
        this.nationality = builder.nationality;
        this.phone = builder.phone;
        this.addresses = builder.addresses;
        this.placeOfBirth = builder.placeOfBirth;
        this.companyInfo = builder.companyInfo;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDob() {
        return dob;
    }

    public String getCountry() {
        return country;
    }

    public String getNationality() {
        return nationality;
    }

    public String getPhone() {
        return phone;
    }

    public List<SumSubAddressModel> getAddresses() {
        return addresses;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public static class Builder {
        public CompanyInfoModel companyInfo;
        private String firstName;
        private String lastName;
        private String dob;
        private String country;
        private String nationality;
        private String phone;
        private List<SumSubAddressModel> addresses;
        private String placeOfBirth;

        public Builder setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder setDob(String dob) {
            this.dob = dob;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setNationality(String nationality) {
            this.nationality = nationality;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setAddresses(List<SumSubAddressModel> addresses) {
            this.addresses = addresses;
            return this;
        }

        public Builder setPlaceOfBirth(String placeOfBirth) {
            this.placeOfBirth = placeOfBirth;
            return this;
        }

        public Builder setCompanyInfo(CompanyInfoModel companyInfo) {
            this.companyInfo = companyInfo;
            return this;
        }

        public FixedInfoModel build() { return new FixedInfoModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static Builder defaultFixedInfoModel(final SumSubFixedInfoModel sumSubFixedInfoModel){
        return new Builder()
                .setFirstName(sumSubFixedInfoModel.getFirstName())
                .setLastName(sumSubFixedInfoModel.getLastName())
                .setDob(sumSubFixedInfoModel.getDob())
                .setNationality(sumSubFixedInfoModel.getNationality() == null ? "MLT" : sumSubFixedInfoModel.getNationality())
                .setCountry("MLT")
                .setPlaceOfBirth(sumSubFixedInfoModel.getPlaceOfBirth() == null ? "Italy" : sumSubFixedInfoModel.getPlaceOfBirth())
                .setAddresses(Collections.singletonList(SumSubAddressModel.addressModelBuilder(sumSubFixedInfoModel.getAddresses().get(0)).build()))
                .setPhone(sumSubFixedInfoModel.getPhone());
    }

    public static Builder randomFixedInfoModel(){
        return new Builder()
                .setFirstName(RandomStringUtils.randomAlphabetic(5))
                .setLastName(RandomStringUtils.randomAlphabetic(5))
                .setDob(LocalDate.now().minusYears(20).toString())
                .setNationality("MLT")
                .setCountry("MLT")
                .setPlaceOfBirth("MALTA")
                .setAddresses(Collections.singletonList(SumSubAddressModel.randomAddressModelBuilder().build()));
    }
}
