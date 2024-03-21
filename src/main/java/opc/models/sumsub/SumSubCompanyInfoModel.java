package opc.models.sumsub;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class SumSubCompanyInfoModel {

    private String companyName;
    private String registrationNumber;
    private String country;
    private String phone;
    private String legalAddress;
    @JsonIgnore
    private CompanyAddressModel address;
    private String incorporatedOn;
    private List<SumSubBeneficiariesModel> beneficiaries;

    public String getCompanyName() {
        return companyName;
    }

    public SumSubCompanyInfoModel setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public SumSubCompanyInfoModel setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public SumSubCompanyInfoModel setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public SumSubCompanyInfoModel setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getLegalAddress() {
        return legalAddress;
    }

    public SumSubCompanyInfoModel setLegalAddress(String legalAddress) {
        this.legalAddress = legalAddress;
        return this;
    }

    public CompanyAddressModel getAddress() {
        return address;
    }

    public SumSubCompanyInfoModel setAddress(CompanyAddressModel address) {
        this.address = address;
        return this;
    }

    public String getIncorporatedOn() {
        return incorporatedOn;
    }

    public SumSubCompanyInfoModel setIncorporatedOn(String incorporatedOn) {
        this.incorporatedOn = incorporatedOn;
        return this;
    }

    public List<SumSubBeneficiariesModel> getBeneficiaries() {
        return beneficiaries;
    }

    public SumSubCompanyInfoModel setBeneficiaries(List<SumSubBeneficiariesModel> beneficiaries) {
        this.beneficiaries = beneficiaries;
        return this;
    }
}
