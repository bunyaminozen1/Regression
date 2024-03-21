package opc.models.sumsub;

import java.util.List;

public class SumSubFixedInfoModel {

    private String firstName;
    private String lastName;
    private String dob;
    private String phone;
    private List<AddressModel> addresses;
    private String placeOfBirth;
    private String nationality;
    private String country;

    public String getFirstName() {
        return firstName;
    }

    public SumSubFixedInfoModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public SumSubFixedInfoModel setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getDob() {
        return dob;
    }

    public SumSubFixedInfoModel setDob(String dob) {
        this.dob = dob;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public SumSubFixedInfoModel setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public List<AddressModel> getAddresses() {
        return addresses;
    }

    public SumSubFixedInfoModel setAddresses(List<AddressModel> addresses) {
        this.addresses = addresses;
        return this;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public SumSubFixedInfoModel setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
        return this;
    }

    public String getNationality() {
        return nationality;
    }

    public SumSubFixedInfoModel setNationality(String nationality) {
        this.nationality = nationality;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public SumSubFixedInfoModel setCountry(String country) {
        this.country = country;
        return this;
    }
}
