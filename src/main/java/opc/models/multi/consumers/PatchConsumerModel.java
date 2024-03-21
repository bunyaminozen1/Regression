package opc.models.multi.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.models.DateOfBirthModel;
import lombok.SneakyThrows;
import opc.enums.opc.ConsumerSourceOfFunds;
import commons.enums.Currency;
import opc.enums.opc.Occupation;
import opc.models.shared.AddressModel;
import commons.models.MobileNumberModel;
import org.apache.commons.lang3.RandomStringUtils;

public class PatchConsumerModel {
    private final String tag;
    private final String name;
    private final String surname;
    private final String email;
    private final MobileNumberModel mobile;
    private final DateOfBirthModel dateOfBirth;
    private final Occupation occupation;
    private final AddressModel address;
    private final String baseCurrency;
    private final ConsumerSourceOfFunds sourceOfFunds;
    private final String sourceOfFundsOther;
    private final String feeGroup;
    private final String placeOfBirth;
    private final String nationality;

    public PatchConsumerModel(final Builder builder) {
        this.tag = builder.tag;
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobile = builder.mobile;
        this.dateOfBirth = builder.dateOfBirth;
        this.occupation = builder.occupation;
        this.address = builder.address;
        this.baseCurrency = builder.baseCurrency;
        this.sourceOfFunds = builder.sourceOfFunds;
        this.sourceOfFundsOther = builder.sourceOfFundsOther;
        this.feeGroup = builder.feeGroup;
        this.placeOfBirth = builder.placeOfBirth;
        this.nationality = builder.nationality;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public MobileNumberModel getMobile() {
        return mobile;
    }

    public DateOfBirthModel getDateOfBirth() {
        return dateOfBirth;
    }

    public Occupation getOccupation() {
        return occupation;
    }

    public AddressModel getAddress() {
        return address;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public ConsumerSourceOfFunds getSourceOfFunds() {
        return sourceOfFunds;
    }

    public String getSourceOfFundsOther() {
        return sourceOfFundsOther;
    }

    public String getFeeGroup() {
        return feeGroup;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public static class Builder {
        private String tag;
        private String name;
        private String surname;
        private String email;
        private MobileNumberModel mobile;
        private DateOfBirthModel dateOfBirth;
        private Occupation occupation;
        private AddressModel address;
        private String baseCurrency;
        private ConsumerSourceOfFunds sourceOfFunds;
        private String sourceOfFundsOther;
        private String feeGroup;
        private String placeOfBirth;
        private String nationality;

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setMobile(MobileNumberModel mobile) {
            this.mobile = mobile;
            return this;
        }

        public Builder setDateOfBirth(DateOfBirthModel dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder setOccupation(Occupation occupation) {
            this.occupation = occupation;
            return this;
        }

        public Builder setAddress(AddressModel address) {
            this.address = address;
            return this;
        }

        public Builder setBaseCurrency(String baseCurrency) {
            this.baseCurrency = baseCurrency;
            return this;
        }

        public Builder setSourceOfFunds(ConsumerSourceOfFunds sourceOfFunds) {
            this.sourceOfFunds = sourceOfFunds;
            return this;
        }

        public Builder setSourceOfFundsOther(String sourceOfFundsOther) {
            this.sourceOfFundsOther = sourceOfFundsOther;
            return this;
        }

        public Builder setFeeGroup(String feeGroup) {
            this.feeGroup = feeGroup;
            return this;
        }

        public Builder setPlaceOfBirth(String placeOfBirth) {
            this.placeOfBirth = placeOfBirth;
            return this;
        }

        public Builder setNationality(String nationality) {
            this.nationality = nationality;
            return this;
        }

        public PatchConsumerModel build(){ return new PatchConsumerModel(this);}
    }



    public static Builder newBuilder(){
        return new Builder();
    }

    public static Builder DefaultPatchConsumerModel(){
        return new Builder()
                .setTag(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6)))
                .setDateOfBirth(new DateOfBirthModel(1980, 01, 02))
                .setOccupation(Occupation.getRandomOccupation())
                .setBaseCurrency(Currency.getRandomCurrency().toString())
                .setSourceOfFunds(ConsumerSourceOfFunds.getRandomSourceOfFunds())
                .setSourceOfFundsOther(ConsumerSourceOfFunds.getRandomSourceOfFunds().toString())
                .setAddress(AddressModel.RandomAddressModel())
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setMobile(MobileNumberModel.randomUK())
                .setFeeGroup("DEFAULT")
                .setPlaceOfBirth("Italy")
                .setNationality("MT");
    }

    @SneakyThrows
    public static String patchConsumerString() {
        return new ObjectMapper().writeValueAsString(DefaultPatchConsumerModel().setEmail(null).setMobile(null).build());
    }
}