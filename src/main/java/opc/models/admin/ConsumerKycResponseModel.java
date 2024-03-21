package opc.models.admin;

import java.util.List;

public class ConsumerKycResponseModel {
    private boolean emailVerified;
    private boolean mobileVerified;
    private String pep;
    private String sanctioned;
    private String fullDueDiligence;
    private String fullDueDiligenceAddressMatched;
    private List<String> allowedLimits;
    private List<String> remainingLimits;
    private String kycLevel;
    private String ongoingFullDueDiligence;
    private String ongoingKycLevel;

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public String getPep() {
        return pep;
    }

    public void setPep(String pep) {
        this.pep = pep;
    }

    public String getSanctioned() {
        return sanctioned;
    }

    public void setSanctioned(String sanctioned) {
        this.sanctioned = sanctioned;
    }

    public String getFullDueDiligence() {
        return fullDueDiligence;
    }

    public void setFullDueDiligence(String fullDueDiligence) {
        this.fullDueDiligence = fullDueDiligence;
    }

    public String getFullDueDiligenceAddressMatched() {
        return fullDueDiligenceAddressMatched;
    }

    public void setFullDueDiligenceAddressMatched(String fullDueDiligenceAddressMatched) {
        this.fullDueDiligenceAddressMatched = fullDueDiligenceAddressMatched;
    }

    public List<String> getAllowedLimits() {
        return allowedLimits;
    }

    public void setAllowedLimits(List<String> allowedLimits) {
        this.allowedLimits = allowedLimits;
    }

    public List<String> getRemainingLimits() {
        return remainingLimits;
    }

    public void setRemainingLimits(List<String> remainingLimits) {
        this.remainingLimits = remainingLimits;
    }

    public String getKycLevel() {
        return kycLevel;
    }

    public void setKycLevel(String kycLevel) {
        this.kycLevel = kycLevel;
    }

    public String getOngoingFullDueDiligence() {
        return ongoingFullDueDiligence;
    }

    public void setOngoingFullDueDiligence(String ongoingFullDueDiligence) {
        this.ongoingFullDueDiligence = ongoingFullDueDiligence;
    }

    public String getOngoingKycLevel() {
        return ongoingKycLevel;
    }

    public void setOngoingKycLevel(String ongoingKycLevel) {
        this.ongoingKycLevel = ongoingKycLevel;
    }
}
