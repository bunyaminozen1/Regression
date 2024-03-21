package opc.models.shared;

public class GetBeneficiaryResponseModel {
    private String id;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private DateOfBirthResponseModel dateOfBirth;
    private String phone;
    private String status;
    private String nationality;
    private String ongoingStatus;
    private String placeOfBirth;
    private String identificationNumber;
    private AddressResponseModel address;
    private String rootUser;

    public String getId() {
        return id;
    }

    public GetBeneficiaryResponseModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public GetBeneficiaryResponseModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public GetBeneficiaryResponseModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getMiddleName() {
        return middleName;
    }

    public GetBeneficiaryResponseModel setMiddleName(String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public GetBeneficiaryResponseModel setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public DateOfBirthResponseModel getDateOfBirth() {
        return dateOfBirth;
    }

    public GetBeneficiaryResponseModel setDateOfBirth(DateOfBirthResponseModel dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public GetBeneficiaryResponseModel setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public GetBeneficiaryResponseModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getNationality() {
        return nationality;
    }

    public GetBeneficiaryResponseModel setNationality(String nationality) {
        this.nationality = nationality;
        return this;
    }

    public String getOngoingStatus() {
        return ongoingStatus;
    }

    public GetBeneficiaryResponseModel setOngoingStatus(String ongoingStatus) {
        this.ongoingStatus = ongoingStatus;
        return this;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public GetBeneficiaryResponseModel setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
        return this;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public GetBeneficiaryResponseModel setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
        return this;
    }

    public AddressResponseModel getAddress() {
        return address;
    }

    public GetBeneficiaryResponseModel setAddress(AddressResponseModel address) {
        this.address = address;
        return this;
    }

    public String getRootUser() {
        return rootUser;
    }

    public GetBeneficiaryResponseModel setRootUser(String rootUser) {
        this.rootUser = rootUser;
        return this;
    }
}