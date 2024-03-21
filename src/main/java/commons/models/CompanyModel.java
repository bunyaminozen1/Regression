package commons.models;

import com.github.javafaker.Faker;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CountryCode;
import opc.models.shared.AddressModel;
import org.apache.commons.lang3.RandomStringUtils;

public class CompanyModel {
    private final String type;
    private final AddressModel businessAddress;
    private final String name;
    private final String registrationNumber;
    private final String registrationCountry;

    public CompanyModel(final Builder builder) {
        this.type = builder.type;
        this.businessAddress = builder.businessAddress;
        this.name = builder.name;
        this.registrationNumber = builder.registrationNumber;
        this.registrationCountry = builder.registrationCountry;
    }

    public String getType() {
        return type;
    }

    public AddressModel getBusinessAddress() {
        return businessAddress;
    }

    public String getName() {
        return name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getRegistrationCountry() {
        return registrationCountry;
    }

    public static class Builder {
        private String type;
        private AddressModel businessAddress;
        private String name;
        private String registrationNumber;
        private String registrationCountry;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setBusinessAddress(AddressModel businessAddress) {
            this.businessAddress = businessAddress;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setRegistrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder setRegistrationCountry(String registrationCountry) {
            this.registrationCountry = registrationCountry;
            return this;
        }

        public CompanyModel build(){ return new CompanyModel(this);}
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public static Builder defaultCompanyModel(){
        return new CompanyModel.Builder()
                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setBusinessAddress(AddressModel.RandomAddressModel())
                .setRegistrationNumber(RandomStringUtils.randomAlphanumeric(10))
                // TODO List of accepted country codes at random
                .setRegistrationCountry(CountryCode.MT.name());
    }

    public static Builder gbCompanyModel(){
        return new CompanyModel.Builder()
                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setBusinessAddress(AddressModel.RandomAddressModel())
                .setRegistrationNumber(RandomStringUtils.randomAlphanumeric(10))
                .setRegistrationCountry(CountryCode.GB.name());
    }

    public static Builder dataCompanyModel(){

        final Faker faker = new Faker();

        return new CompanyModel.Builder()
                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                .setName(faker.company().name())
                .setBusinessAddress(AddressModel.dataAddressModel().build())
                .setRegistrationNumber(RandomStringUtils.randomAlphanumeric(10))
                // TODO List of accepted country codes at random
                .setRegistrationCountry(CountryCode.MT.name());
    }
}
