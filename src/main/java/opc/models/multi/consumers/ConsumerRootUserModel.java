package opc.models.multi.consumers;

import commons.models.DateOfBirthModel;
import commons.models.PersonalDetailsModel;
import opc.enums.opc.Occupation;
import opc.helpers.ModelHelper;
import opc.models.shared.AddressModel;
import commons.models.MobileNumberModel;
import org.apache.commons.lang3.RandomStringUtils;

public class ConsumerRootUserModel {
    private final String name;
    private final String surname;
    private final String email;
    private final MobileNumberModel mobile;
    private final DateOfBirthModel dateOfBirth;
    private final Occupation occupation;
    private final AddressModel address;
    private final String placeOfBirth;
    private final String nationality;

    public ConsumerRootUserModel(final ConsumerRootUserModel.Builder builder) {
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobile = builder.mobile;
        this.dateOfBirth = builder.dateOfBirth;
        this.occupation = builder.occupation;
        this.address = builder.address;
        this.placeOfBirth = builder.placeOfBirth;
        this.nationality = builder.nationality;
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

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public static class Builder {

        private String name;
        private String surname;
        private String email;
        private MobileNumberModel mobile;
        private DateOfBirthModel dateOfBirth;
        private Occupation occupation;
        private AddressModel address;
        private String placeOfBirth;
        private String nationality;

        public ConsumerRootUserModel.Builder setName(String name) {
            this.name = name;
            return this;
        }

        public ConsumerRootUserModel.Builder setSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public ConsumerRootUserModel.Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public ConsumerRootUserModel.Builder setMobile(MobileNumberModel mobile) {
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

        public Builder setPlaceOfBirth(String placeOfBirth) {
            this.placeOfBirth = placeOfBirth;
            return this;
        }

        public Builder setNationality(String nationality) {
            this.nationality = nationality;
            return this;
        }

        public ConsumerRootUserModel build(){ return new ConsumerRootUserModel(this);}
    }

    public static Builder DefaultRootUserModel() {
        return new ConsumerRootUserModel.Builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                        RandomStringUtils.randomAlphabetic(5)))
                // TODO List of accepted country codes
                .setMobile(MobileNumberModel.random())
                .setDateOfBirth(new DateOfBirthModel(1990, 1, 1))
                .setOccupation(Occupation.getRandomOccupation())
                .setAddress(AddressModel.RandomAddressModel())
                .setPlaceOfBirth("Malta")
                .setNationality("IT");
    }

    public static Builder RootUserModelNoOccupation() {
        return new ConsumerRootUserModel.Builder()
            .setName(RandomStringUtils.randomAlphabetic(6))
            .setSurname(RandomStringUtils.randomAlphabetic(6))
            .setEmail(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                RandomStringUtils.randomAlphabetic(5)))
            .setMobile(MobileNumberModel.random())
            .setDateOfBirth(new DateOfBirthModel(1990, 1, 1))
            .setAddress(AddressModel.RandomAddressModel())
            .setPlaceOfBirth("Malta")
            .setNationality("IT");
    }

    public static Builder dataRootUserModel() {

        final PersonalDetailsModel personalDetails = ModelHelper.getPersonalDetails();

        return new ConsumerRootUserModel.Builder()
                .setName(personalDetails.getName())
                .setSurname(personalDetails.getSurname())
                .setEmail(personalDetails.getEmail())
                // TODO List of accepted country codes
                .setMobile(MobileNumberModel.random())
                .setDateOfBirth(new DateOfBirthModel(1990, 1, 1))
                .setOccupation(Occupation.getRandomOccupation())
                .setAddress(personalDetails.getAddress())
                .setPlaceOfBirth("Malta")
                .setNationality("IT");
    }
}