package opc.models.sumsub;

public class SumSubAuthenticatedUserInfoModel {

    private String firstName;
    private String firstNameEn;
    private String lastName;
    private String lastNameEn;

    public String getFirstName() {
        return firstName;
    }

    public SumSubAuthenticatedUserInfoModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getFirstNameEn() {
        return firstNameEn;
    }

    public SumSubAuthenticatedUserInfoModel setFirstNameEn(String firstNameEn) {
        this.firstNameEn = firstNameEn;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public SumSubAuthenticatedUserInfoModel setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getLastNameEn() {
        return lastNameEn;
    }

    public SumSubAuthenticatedUserInfoModel setLastNameEn(String lastNameEn) {
        this.lastNameEn = lastNameEn;
        return this;
    }
}
