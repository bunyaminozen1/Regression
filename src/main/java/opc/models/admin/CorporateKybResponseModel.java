package opc.models.admin;

import java.util.List;

public class CorporateKybResponseModel {
    private boolean rootEmailVerified;
    private boolean rootMobileVerified;
    private String basicCompanyChecksVerified;
    private String fullCompanyChecksVerified;
    private String enhancedCompanyChecksVerified;

    private List<AllowedLimitsResponseModel> allowedLimits;
    private List<RemainingLimitsResponseModel> remainingLimits;
    private String ongoingStatus;

    public boolean isRootEmailVerified() {
        return rootEmailVerified;
    }

    public void setRootEmailVerified(boolean rootEmailVerified) {
        this.rootEmailVerified = rootEmailVerified;
    }

    public boolean isRootMobileVerified() {
        return rootMobileVerified;
    }

    public void setRootMobileVerified(boolean rootMobileVerified) {
        this.rootMobileVerified = rootMobileVerified;
    }

    public String getBasicCompanyChecksVerified() {
        return basicCompanyChecksVerified;
    }

    public void setBasicCompanyChecksVerified(String basicCompanyChecksVerified) {
        this.basicCompanyChecksVerified = basicCompanyChecksVerified;
    }


    public String getFullCompanyChecksVerified() {
        return fullCompanyChecksVerified;
    }

    public void setFullCompanyChecksVerified(String fullCompanyChecksVerified) {
        this.fullCompanyChecksVerified = fullCompanyChecksVerified;
    }

    public String getEnhancedCompanyChecksVerified() {
        return enhancedCompanyChecksVerified;
    }

    public void setEnhancedCompanyChecksVerified(String enhancedCompanyChecksVerified) {
        this.enhancedCompanyChecksVerified = enhancedCompanyChecksVerified;
    }

    public List<AllowedLimitsResponseModel> getAllowedLimits() {
        return allowedLimits;
    }

    public void setAllowedLimits(List<AllowedLimitsResponseModel> allowedLimits) {
        this.allowedLimits = allowedLimits;
    }

    public List<RemainingLimitsResponseModel> getRemainingLimits() {
        return remainingLimits;
    }

    public void setRemainingLimits(List<RemainingLimitsResponseModel> remainingLimits) {
        this.remainingLimits = remainingLimits;
    }

    public String getOngoingStatus() {
        return ongoingStatus;
    }

    public void setOngoingStatus(String ongoingStatus) {
        this.ongoingStatus = ongoingStatus;
    }
}
