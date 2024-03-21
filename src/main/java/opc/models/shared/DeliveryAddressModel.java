package opc.models.shared;

import opc.enums.opc.CountryCode;
import org.apache.commons.lang3.RandomStringUtils;

public class DeliveryAddressModel {
    private final String name;
    private final String surname;
    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String country;
    private final String postCode;
    private final String state;

    public DeliveryAddressModel(final Builder builder) {
        this.name = builder.name;
        this.surname = builder.surname;
        this.addressLine1 = builder.addressLine1;
        this.addressLine2 = builder.addressLine2;
        this.city = builder.city;
        this.country = builder.country;
        this.postCode = builder.postCode;
        this.state = builder.state;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

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

    public static class Builder {
        private String name;
        private String surname;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String country;
        private String postCode;
        private String state;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder setAddressLine1(String addressLine1) {
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

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setPostCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Builder setState(String state) {
            this.state = state;
            return this;
        }

        public DeliveryAddressModel build() { return new DeliveryAddressModel(this); }
    }

    public static Builder DefaultDeliveryAddressModel() {
        return new Builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setAddressLine1(RandomStringUtils.randomAlphabetic(5))
                .setAddressLine2(RandomStringUtils.randomAlphabetic(5))
                .setCity(RandomStringUtils.randomAlphabetic(5))
                .setCountry(CountryCode.getRandomEeaCountry().toString())
                .setPostCode(RandomStringUtils.randomAlphanumeric(6))
                .setState(RandomStringUtils.randomAlphabetic(5));
    }
}
