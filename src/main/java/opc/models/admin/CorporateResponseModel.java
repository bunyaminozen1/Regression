package opc.models.admin;

import opc.models.shared.TypeIdResponseModel;

public class CorporateResponseModel {
    private TypeIdResponseModel id;
    private String profileId;
    private String tag;
    private String name;
    private String companyType;
    private String supportEmail;
    private boolean active;
    private String creationTimestamp;
    private String companyRegistrationNumber;
    private String registrationCountry;
    private boolean acceptedTerms;
    private CorporateKybResponseModel kyb;
    private String baseCurrency;
    private String feeGroup;
    private DateOfBirthResponseModel incorporatedOn;
    private String industry;
    private String sourceOfFunds;
    private String sourceOfFundsOther;
    private String ipAddress;
    private String companyRegistrationAddress;
    private String companyBusinessAddress;
    private AddressResponseModel registrationAddress;
    private AddressResponseModel businessAddress;
    private CorporateRootUserResponseModel rootUser;
    private Long tenantId;
    private Long programmeId;
    private boolean passwordAlreadySet;
    private boolean permanentlyClosed;


    public TypeIdResponseModel getId() {
        return id;
    }

    public void setId(TypeIdResponseModel id) {
        this.id = id;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getCompanyRegistrationNumber() {
        return companyRegistrationNumber;
    }

    public void setCompanyRegistrationNumber(String companyRegistrationNumber) {
        this.companyRegistrationNumber = companyRegistrationNumber;
    }

    public String getRegistrationCountry() {
        return registrationCountry;
    }

    public void setRegistrationCountry(String registrationCountry) {
        this.registrationCountry = registrationCountry;
    }

    public boolean getAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public CorporateKybResponseModel getKyb() {
        return kyb;
    }

    public void setKyb(CorporateKybResponseModel kyb) {
        this.kyb = kyb;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getFeeGroup() {
        return feeGroup;
    }

    public void setFeeGroup(String feeGroup) {
        this.feeGroup = feeGroup;
    }

    public DateOfBirthResponseModel getIncorporatedOn() {
        return incorporatedOn;
    }

    public void setIncorporatedOn(DateOfBirthResponseModel incorporatedOn) {
        this.incorporatedOn = incorporatedOn;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getSourceOfFunds() {
        return sourceOfFunds;
    }

    public void setSourceOfFunds(String sourceOfFunds) {
        this.sourceOfFunds = sourceOfFunds;
    }

    public String getSourceOfFundsOther() {
        return sourceOfFundsOther;
    }

    public void setSourceOfFundsOther(String sourceOfFundsOther) {
        this.sourceOfFundsOther = sourceOfFundsOther;
    }

    public AddressResponseModel getRegistrationAddress() {
        return registrationAddress;
    }

    public void setRegistrationAddress(AddressResponseModel registrationAddress) {
        this.registrationAddress = registrationAddress;
    }

    public AddressResponseModel getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(AddressResponseModel businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getCompanyRegistrationAddress() {
        return companyRegistrationAddress;
    }

    public void setRegistrationAddress(String companyRegistrationAddress) {
        this.companyRegistrationAddress = companyRegistrationAddress;
    }

    public String getCompanyBusinessAddress() {
        return companyBusinessAddress;
    }

    public void setCompanyBusinessAddress(String businessAddress) {
        this.companyBusinessAddress = companyBusinessAddress;
    }

    public CorporateRootUserResponseModel getRootUser() {
        return rootUser;
    }

    public void setRootUser(CorporateRootUserResponseModel rootUser) {
        this.rootUser = rootUser;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(Long programmeId) {
        this.programmeId = programmeId;
    }

    public boolean isPasswordAlreadySet() {
        return passwordAlreadySet;
    }

    public void setPasswordAlreadySet(boolean passwordAlreadySet) {
        this.passwordAlreadySet = passwordAlreadySet;
    }

    public boolean getPermanentlyClosed() {
        return permanentlyClosed;
    }

    public void setPermanentlyClosed(boolean permanentlyClosed) {
        this.permanentlyClosed = permanentlyClosed;
    }


}
