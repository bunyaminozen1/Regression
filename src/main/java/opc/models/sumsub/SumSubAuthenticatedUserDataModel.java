package opc.models.sumsub;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;

public class SumSubAuthenticatedUserDataModel {

    private String id;
    private String createdAt;
    private String key;
    private String clientId;
    private String inspectionId;
    private String externalUserId;
    private SumSubFixedInfoModel fixedInfo;
    private SumSubAuthenticatedUserInfoModel info;
    private String email;
    @JsonIgnore
    private HashMap<String, String> agreement;
    private SumSubApplicantReviewModel review;
    private String type;
    @JsonIgnore
    private HashMap<String, String> requiredIdDocs;
    @JsonIgnore
    private String lang;
    @JsonIgnore
    private HashMap<String, String> memberOf;
    private String sourceKey;
    @JsonIgnore
    private String applicantPlatform;

    public String getId() {
        return id;
    }

    public SumSubAuthenticatedUserDataModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public SumSubAuthenticatedUserDataModel setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getKey() {
        return key;
    }

    public SumSubAuthenticatedUserDataModel setKey(String key) {
        this.key = key;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public SumSubAuthenticatedUserDataModel setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getInspectionId() {
        return inspectionId;
    }

    public SumSubAuthenticatedUserDataModel setInspectionId(String inspectionId) {
        this.inspectionId = inspectionId;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public SumSubAuthenticatedUserDataModel setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        return this;
    }

    public SumSubFixedInfoModel getFixedInfo() {
        return fixedInfo;
    }

    public SumSubAuthenticatedUserDataModel setFixedInfo(SumSubFixedInfoModel fixedInfo) {
        this.fixedInfo = fixedInfo;
        return this;
    }

    public SumSubAuthenticatedUserInfoModel getInfo() {
        return info;
    }

    public SumSubAuthenticatedUserDataModel setInfo(SumSubAuthenticatedUserInfoModel info) {
        this.info = info;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public SumSubAuthenticatedUserDataModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public HashMap<String, String> getAgreement() {
        return agreement;
    }

    public SumSubAuthenticatedUserDataModel setAgreement(HashMap<String, String> agreement) {
        this.agreement = agreement;
        return this;
    }

    public SumSubApplicantReviewModel getReview() {
        return review;
    }

    public SumSubAuthenticatedUserDataModel setReview(SumSubApplicantReviewModel review) {
        this.review = review;
        return this;
    }

    public String getType() {
        return type;
    }

    public SumSubAuthenticatedUserDataModel setType(String type) {
        this.type = type;
        return this;
    }

    public HashMap<String, String> getRequiredIdDocs() {
        return requiredIdDocs;
    }

    public SumSubAuthenticatedUserDataModel setRequiredIdDocs(HashMap<String, String> requiredIdDocs) {
        this.requiredIdDocs = requiredIdDocs;
        return this;
    }

    public String getLang() {
        return lang;
    }

    public SumSubAuthenticatedUserDataModel setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public HashMap<String, String> getMemberOf() {
        return memberOf;
    }

    public SumSubAuthenticatedUserDataModel setMemberOf(HashMap<String, String> memberOf) {
        this.memberOf = memberOf;
        return this;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public SumSubAuthenticatedUserDataModel setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
        return this;
    }

    public String getApplicantPlatform() {
        return applicantPlatform;
    }

    public SumSubAuthenticatedUserDataModel setApplicantPlatform(String applicantPlatform) {
        this.applicantPlatform = applicantPlatform;
        return this;
    }
}
