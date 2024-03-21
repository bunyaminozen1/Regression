package opc.models.sumsub;

import org.apache.commons.lang3.RandomStringUtils;

public class SumSubAddressModel {

    private final String subStreet;
    private final String street;
    private final String state;
    private final String town;
    private final String postCode;
    private final String country;

    public SumSubAddressModel(final Builder builder) {
        this.subStreet = builder.subStreet;
        this.street = builder.street;
        this.state = builder.state;
        this.town = builder.town;
        this.postCode = builder.postCode;
        this.country = builder.country;
    }

    public String getSubStreet() {
        return subStreet;
    }

    public String getStreet() {
        return street;
    }

    public String getState() {
        return state;
    }

    public String getTown() {
        return town;
    }

    public String getPostCode() {
        return postCode;
    }

    public String getCountry() {
        return country;
    }

    public static class Builder {
        private String subStreet;
        private String street;
        private String state;
        private String town;
        private String postCode;
        private String country;

        public Builder setSubStreet(String subStreet) {
            this.subStreet = subStreet;
            return this;
        }

        public Builder setStreet(String street) {
            this.street = street;
            return this;
        }

        public Builder setState(String state) {
            this.state = state;
            return this;
        }

        public Builder setTown(String town) {
            this.town = town;
            return this;
        }

        public Builder setPostCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public SumSubAddressModel build() { return new SumSubAddressModel(this); }
    }

    public static Builder builder() { return new Builder(); }

    public static Builder addressModelBuilder(final AddressModel addressModel) {
        return new Builder()
                .setStreet(addressModel.getStreet())
                .setSubStreet(addressModel.getSubStreet())
                .setTown(addressModel.getTown())
                .setPostCode(addressModel.getPostCode())
                .setCountry(addressModel.getCountry())
                .setState(addressModel.getState());
    }

    public static Builder randomAddressModelBuilder() {
        return new Builder()
                .setStreet(RandomStringUtils.randomAlphabetic(5))
                .setSubStreet(RandomStringUtils.randomAlphabetic(5))
                .setTown(RandomStringUtils.randomAlphabetic(5))
                .setPostCode(RandomStringUtils.randomAlphabetic(5))
                .setCountry("MLT")
                .setState(RandomStringUtils.randomAlphabetic(5));
    }
}
