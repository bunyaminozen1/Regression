package opc.models.admin;

import opc.models.shared.TypeIdResponseModel;

public class ConsumersRootUserResponseModel {
    private String id;
    private TypeIdResponseModel identity;
    private String title;
    private String name;
    private String surname;
    private String email;
    private boolean active;
    private DateOfBirthResponseModel dateOfBirth;
    private String mobileCountryCode;
    private String mobileNumber;
    private String placeOfBirth;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TypeIdResponseModel getIdentity() {
        return identity;
    }

    public void setIdentity(TypeIdResponseModel identity) {
        this.identity = identity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public DateOfBirthResponseModel getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(DateOfBirthResponseModel dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getMobileCountryCode() {
        return mobileCountryCode;
    }

    public void setMobileCountryCode(String mobileCountryCode) {
        this.mobileCountryCode = mobileCountryCode;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }
}
