package opc.models.admin;

import opc.models.shared.TypeIdResponseModel;

public class ConsumerResponseModel {

    private TypeIdResponseModel id;
    private String profileId;
    private String tag;
    private ConsumersRootUserResponseModel rootUser;
    private boolean active;
    private String creationTimestamp;
    private String baseCurrency;
    private AddressResponseModel address;
    private String feeGroup;
    private String occupation;
    private String sourceOfFunds;
    private String sourceOfFundsOther;
    private String nationality;
    private String ipAddress;
    private boolean acceptedTerms;
    private Long tenantId;
    private String programmeId;
    public boolean permanentlyClosed;

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

    public ConsumersRootUserResponseModel getRootUser() {
        return rootUser;
    }

    public void setRootUser(ConsumersRootUserResponseModel rootUser) {
        this.rootUser = rootUser;
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

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public AddressResponseModel getAddress() {
        return address;
    }

    public void setAddress(AddressResponseModel address) {
        this.address = address;
    }

    public String getFeeGroup() {
        return feeGroup;
    }

    public void setFeeGroup(String feeGroup) {
        this.feeGroup = feeGroup;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
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

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(String programmeId) {
        this.programmeId = programmeId;
    }

    public boolean getPermanentlyClosed() {
        return permanentlyClosed;
    }

    public void setPermanentlyClosed(boolean permanentlyClosed) {
        this.permanentlyClosed = permanentlyClosed;
    }
}
