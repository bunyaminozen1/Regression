package opc.models.multi.users;

import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import org.apache.commons.lang3.RandomStringUtils;

public class UsersModel {
    private final String name;
    private final String surname;
    private final String email;
    private final DateOfBirthModel dateOfBirth;
    private final MobileNumberModel mobile;
    private final String tag;

    public UsersModel(final Builder builder) {
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.dateOfBirth = builder.dateOfBirth;
        this.mobile = builder.mobile;
        this.tag = builder.tag;
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

    public DateOfBirthModel getDateOfBirth() {
        return dateOfBirth;
    }

    public MobileNumberModel getMobile() {
        return mobile;
    }
    public String getTag() {return tag;}

    public static class Builder {
        private String name;
        private String surname;
        private String email;
        private DateOfBirthModel dateOfBirth;
        private MobileNumberModel mobile;
        private String tag;

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

        public Builder setDateOfBirth(final DateOfBirthModel dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder setMobile(final MobileNumberModel mobile) {
            this.mobile = mobile;
            return this;
        }

        public Builder setTag(final String tag) {
            this.tag = tag;
            return this;
        }

        public UsersModel build() {return new UsersModel(this);}
    }

    public static Builder DefaultUsersModel() {
        return new Builder()
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10)))
                .setDateOfBirth(new DateOfBirthModel(1992, 3, 3))
                .setMobile(new MobileNumberModel("+356", String.format("21%s", RandomStringUtils.randomNumeric(6))));
    }

    public static Builder builder() {
        return new Builder();
    }
}
