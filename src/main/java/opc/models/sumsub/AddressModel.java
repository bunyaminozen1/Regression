package opc.models.sumsub;

public class AddressModel {

    private String subStreet;
    private String street;
    private String state;
    private String town;
    private String postCode;
    private String country;
    private String formattedAddress;

    public String getSubStreet() {
        return subStreet;
    }

    public AddressModel setSubStreet(String subStreet) {
        this.subStreet = subStreet;
        return this;
    }

    public String getStreet() {
        return street;
    }

    public AddressModel setStreet(String street) {
        this.street = street;
        return this;
    }

    public String getState() {
        return state;
    }

    public AddressModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getTown() {
        return town;
    }

    public AddressModel setTown(String town) {
        this.town = town;
        return this;
    }

    public String getPostCode() {
        return postCode;
    }

    public AddressModel setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public AddressModel setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public AddressModel setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
        return this;
    }
}
