package opc.models.admin;

public class UboResponseModel {
    private String id;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private DateOfBirthResponseModel dateOfBirth;
    private String phone;
    private boolean status;
    private String nationality;
    private boolean ongoingStatus;
    private String placeOfBirth;
    private String identificationNumber;
    private AddressResponseModel address;
    private boolean rootUser;
    private String shareSize;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public DateOfBirthResponseModel getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(DateOfBirthResponseModel dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public boolean isOngoingStatus() {
        return ongoingStatus;
    }

    public void setOngoingStatus(boolean ongoingStatus) {
        this.ongoingStatus = ongoingStatus;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public AddressResponseModel getAddress() {
        return address;
    }

    public void setAddress(AddressResponseModel address) {
        this.address = address;
    }

    public boolean isRootUser() {
        return rootUser;
    }

    public void setRootUser(boolean rootUser) {
        this.rootUser = rootUser;
    }

    public String getShareSize() {
        return shareSize;
    }

    public void setShareSize(String shareSize) {
        this.shareSize = shareSize;
    }
}
