package opc.models.innovator;

import opc.enums.opc.Industry;
import opc.enums.opc.InnovatorSourceOfFunds;
import opc.models.shared.AddressModel;
import opc.models.shared.PasswordModel;
import org.apache.commons.lang3.RandomStringUtils;

public class InnovatorRegistrationModel {

    final private String name;
    final private String surname;
    final private String email;
    final private PasswordModel password;
    final private String innovatorName;
    final private String kybProviderKey;
    final private String amlProviderKey;
    final private AddressModel registrationAddress;
    final private AddressModel businessAddress;
    final private String industry;
    final private String sourceOfFunds;

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public PasswordModel getPassword() {
        return password;
    }

    public String getInnovatorName() {
        return innovatorName;
    }

    public String getKybProviderKey() {
        return kybProviderKey;
    }

    public String getAmlProviderKey() {
        return amlProviderKey;
    }

    public AddressModel getRegistrationAddress() {
        return registrationAddress;
    }

    public AddressModel getBusinessAddress() {
        return businessAddress;
    }

    public String getIndustry() {
        return industry;
    }

    public String getSourceOfFunds() {
        return sourceOfFunds;
    }

    public InnovatorRegistrationModel(final Builder builder) {
        name = builder.name;
        surname = builder.surname;
        email = builder.email;
        password = builder.password;
        innovatorName = builder.innovatorName;
        kybProviderKey = builder.kybProviderKey;
        amlProviderKey = builder.amlProviderKey;
        registrationAddress = builder.registrationAddress;
        businessAddress = builder.businessAddress;
        industry = builder.industry.toString();
        sourceOfFunds = builder.sourceOfFunds.toString();
    }

    public static class Builder {

        private String name;
        private String surname;
        private String email;
        private PasswordModel password;
        private String innovatorName;
        private String kybProviderKey;
        private String amlProviderKey;
        private AddressModel registrationAddress;
        private AddressModel businessAddress;
        private Industry industry;
        private InnovatorSourceOfFunds sourceOfFunds;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setSurname(final String surname) {
            this.surname = surname;
            return this;
        }

        public Builder setEmail(final String email) {
            this.email = email;
            return this;
        }

        public Builder setPassword(final PasswordModel password) {
            this.password = password;
            return this;
        }

        public Builder setInnovatorName(final String innovatorName) {
            this.innovatorName = innovatorName;
            return this;
        }

        public Builder setKybProviderKey(final String kybProviderKey) {
            this.kybProviderKey = kybProviderKey;
            return this;
        }

        public Builder setAmlProviderKey(final String amlProviderKey) {
            this.amlProviderKey = amlProviderKey;
            return this;
        }

        public Builder setRegistrationAddress(final AddressModel registrationAddress) {
            this.registrationAddress = registrationAddress;
            return this;
        }

        public Builder setBusinessAddress(final AddressModel businessAddress) {
            this.businessAddress = businessAddress;
            return this;
        }

        public Builder setIndustry(final Industry industry) {
            this.industry = industry;
            return this;
        }

        public Builder setSourceOfFunds(final InnovatorSourceOfFunds sourceOfFunds) {
            this.sourceOfFunds = sourceOfFunds;
            return this;
        }

        public InnovatorRegistrationModel build() {
            return new InnovatorRegistrationModel(this);
        }
    }

    public static Builder RandomInnovatorRegistrationModel() {
        return new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(5))
                .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(10)))
                .setPassword(new PasswordModel("Pass1234!"))
                .setInnovatorName(RandomStringUtils.randomAlphabetic(10))
                .setKybProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setRegistrationAddress(AddressModel.RandomAddressModel())
                .setBusinessAddress(AddressModel.RandomAddressModel())
                .setIndustry(Industry.getRandomIndustry())
                .setSourceOfFunds(InnovatorSourceOfFunds.LABOUR_CONTRACT);
    }
}