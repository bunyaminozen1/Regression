package opc.models.multi.corporates;

import commons.models.CompanyPosition;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import commons.models.PersonalDetailsModel;
import opc.helpers.ModelHelper;
import org.apache.commons.lang3.RandomStringUtils;

public class CorporateRootUserModel {
    private final String name;
    private final String surname;
    private final String email;
    private final MobileNumberModel mobile;
    private final CompanyPosition companyPosition;
    private final DateOfBirthModel dateOfBirth;

    public CorporateRootUserModel(final Builder builder) {
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobile = builder.mobile;
        this.companyPosition = builder.companyPosition;
        this.dateOfBirth = builder.dateOfBirth;
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

    public CompanyPosition getCompanyPosition() {
        return companyPosition;
    }

    public DateOfBirthModel getDateOfBirth() {
        return dateOfBirth;
    }

    public static class Builder {

        private String name;
        private String surname;
        private String email;
        private MobileNumberModel mobile;
        private CompanyPosition companyPosition;
        private DateOfBirthModel dateOfBirth;

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

        public Builder setCompanyPosition(CompanyPosition companyPosition) {
            this.companyPosition = companyPosition;
            return this;
        }

        public Builder setDateOfBirth(DateOfBirthModel dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public String getEmail() {
            return email;
        }

        public CorporateRootUserModel build(){ return new CorporateRootUserModel(this);}
    }

    public static Builder DefaultRootUserModel() {
        return new CorporateRootUserModel.Builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                        RandomStringUtils.randomAlphabetic(5)))
                // TODO List of accepted country codes
                .setMobile(MobileNumberModel.random())
                .setCompanyPosition(CompanyPosition.getRandomCompanyPosition())
                .setDateOfBirth(new DateOfBirthModel(1990, 1, 1));
    }
    public static Builder DefaultRootUserModelNameWithSpaces() {
        return new CorporateRootUserModel.Builder()
            .setName("   "+RandomStringUtils.randomAlphabetic(6).toLowerCase()+"   ")
            .setSurname("   "+RandomStringUtils.randomAlphabetic(6).toLowerCase()+"   ")
            .setEmail(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                RandomStringUtils.randomAlphabetic(5)))
            .setMobile(MobileNumberModel.random())
            .setCompanyPosition(CompanyPosition.getRandomCompanyPosition())
            .setDateOfBirth(new DateOfBirthModel(1990, 1, 1));
    }

    public static Builder dataRootUserModel() {

        final PersonalDetailsModel personalDetails = ModelHelper.getPersonalDetails();

        return new CorporateRootUserModel.Builder()
                .setName(personalDetails.getName())
                .setSurname(personalDetails.getSurname())
                .setEmail(personalDetails.getEmail())
                // TODO List of accepted country codes
                .setMobile(MobileNumberModel.random())
                .setDateOfBirth(new DateOfBirthModel(1990, 1, 1))
                .setCompanyPosition(CompanyPosition.getRandomCompanyPosition());
    }
}
