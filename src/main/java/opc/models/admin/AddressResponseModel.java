package opc.models.admin;


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

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getPostCode() {
        return postCode;
    }

    public String getState() {
        return state;
    }

    public void setAddressLine1(final String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;

    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public void setState(final String state) {
        this.state = state;
    }
}
