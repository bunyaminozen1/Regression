package opc.models.sumsub;

import com.fasterxml.jackson.annotation.JsonIgnore;
import opc.models.sumsub.questionnaire.SumSubQuestionnairesModel;

import java.util.HashMap;

public class SumSubApplicantDataModel {

    private String id;
    private String createdAt;
    private String key;
    private String clientId;
    private String inspectionId;
    private String externalUserId;
    private SumSubFixedInfoModel fixedInfo;
    private SumSubInfoModel info;
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
    @JsonIgnore
    private SumSubQuestionnairesModel questionnaires;

    public String getId() {
        return id;
    }

    public SumSubApplicantDataModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public SumSubApplicantDataModel setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getKey() {
        return key;
    }

    public SumSubApplicantDataModel setKey(String key) {
        this.key = key;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public SumSubApplicantDataModel setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getInspectionId() {
        return inspectionId;
    }

    public SumSubApplicantDataModel setInspectionId(String inspectionId) {
        this.inspectionId = inspectionId;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public SumSubApplicantDataModel setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        return this;
    }

    public SumSubFixedInfoModel getFixedInfo() {
        return fixedInfo;
    }

    public SumSubApplicantDataModel setFixedInfo(SumSubFixedInfoModel fixedInfo) {
        this.fixedInfo = fixedInfo;
        return this;
    }

    public SumSubInfoModel getInfo() {
        return info;
    }

    public SumSubApplicantDataModel setInfo(SumSubInfoModel info) {
        this.info = info;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public SumSubApplicantDataModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public HashMap<String, String> getAgreement() {
        return agreement;
    }

    public SumSubApplicantDataModel setAgreement(HashMap<String, String> agreement) {
        this.agreement = agreement;
        return this;
    }

    public SumSubApplicantReviewModel getReview() {
        return review;
    }

    public SumSubApplicantDataModel setReview(SumSubApplicantReviewModel review) {
        this.review = review;
        return this;
    }

    public String getType() {
        return type;
    }

    public SumSubApplicantDataModel setType(String type) {
        this.type = type;
        return this;
    }

    public HashMap<String, String> getRequiredIdDocs() {
        return requiredIdDocs;
    }

    public SumSubApplicantDataModel setRequiredIdDocs(HashMap<String, String> requiredIdDocs) {
        this.requiredIdDocs = requiredIdDocs;
        return this;
    }

    public String getLang() {
        return lang;
    }

    public SumSubApplicantDataModel setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public HashMap<String, String> getMemberOf() {
        return memberOf;
    }

    public SumSubApplicantDataModel setMemberOf(HashMap<String, String> memberOf) {
        this.memberOf = memberOf;
        return this;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public SumSubApplicantDataModel setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
        return this;
    }

    public String getApplicantPlatform() {
        return applicantPlatform;
    }

    public SumSubQuestionnairesModel getQuestionnaires() {
        return questionnaires;
    }

    public SumSubApplicantDataModel setQuestionnaires(SumSubQuestionnairesModel questionnaires) {
        this.questionnaires = questionnaires;
        return this;
    }

    public SumSubApplicantDataModel setApplicantPlatform(String applicantPlatform) {
        this.applicantPlatform = applicantPlatform;
        return this;
    }
}
