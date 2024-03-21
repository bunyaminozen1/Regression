package opc.models.shared;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import opc.enums.opc.CountryCode;
import org.apache.commons.lang3.RandomStringUtils;

public class AddressModel {

    final private String addressLine1;
    final private String addressLine2;
    final private String city;
    final private String country;
    final private String postCode;
    final private String state;

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

    public AddressModel(final Builder builder) {
        addressLine1 = builder.addressLine1;
        addressLine2 = builder.addressLine2;
        city = builder.city;
        country = builder.country == null ? null : builder.country.toString();
        postCode = builder.postCode;
        state = builder.state;
    }

    public static class Builder {

        private String addressLine1;
        private String addressLine2;
        private String city;
        private CountryCode country;
        private String postCode;
        private String state;

        public Builder setAddressLine1(final String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public Builder setAddressLine2(String addressLine2) {
            this.addressLine2 = addressLine2;
            return this;
        }

        public Builder setCity(String city) {
            this.city = city;
            return this;
        }

        public Builder setCountry(CountryCode country) {
            this.country = country;
            return this;
        }

        public Builder setPostCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Builder setState(final String state) {
            this.state = state;
            return this;
        }

        public AddressModel build() {
            return new AddressModel(this);
        }
    }

    public static AddressModel RandomAddressModel() {
        return new Builder()
                .setAddressLine1(RandomStringUtils.randomAlphabetic(5))
                .setAddressLine2(RandomStringUtils.randomAlphabetic(5))
                .setCity(RandomStringUtils.randomAlphabetic(5))
                .setCountry(CountryCode.getRandomEeaCountry())
                .setPostCode(RandomStringUtils.randomAlphanumeric(6))
                .setState(RandomStringUtils.randomAlphabetic(5))
                .build();
    }

    public static Builder DefaultAddressModel() {
        return new Builder()
                .setAddressLine1(RandomStringUtils.randomAlphabetic(5))
                .setAddressLine2(RandomStringUtils.randomAlphabetic(5))
                .setCity(RandomStringUtils.randomAlphabetic(5))
                .setCountry(CountryCode.getRandomEeaCountry())
                .setPostCode(RandomStringUtils.randomAlphanumeric(6))
                .setState(RandomStringUtils.randomAlphabetic(5));
    }

    public static Builder dataAddressModel() {

        final Address address = new Faker().address();

        return new Builder()
                .setAddressLine1(address.streetAddress())
                .setAddressLine2(address.secondaryAddress())
                .setCity(address.city())
                .setPostCode(address.zipCode())
                .setCountry(CountryCode.getRandomEeaCountry())
                .setState(null);
    }

    public static Builder builder() {
        return new Builder();
    }
}
