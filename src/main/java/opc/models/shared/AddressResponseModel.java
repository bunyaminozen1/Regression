package opc.models.shared;

public class AddressResponseModel {

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String country;
    private String postCode;
    private String state;

    public String getAddressLine1() {
        return addressLine1;
    }

    public AddressResponseModel setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
        return this;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public AddressResponseModel setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
        return this;
    }

    public String getCity() {
        return city;
    }

    public AddressResponseModel setCity(String city) {
        this.city = city;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public AddressResponseModel setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getPostCode() {
        return postCode;
    }

    public AddressResponseModel setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }

    public String getState() {
        return state;
    }

    public AddressResponseModel setState(String state) {
        this.state = state;
        return this;
    }
}
