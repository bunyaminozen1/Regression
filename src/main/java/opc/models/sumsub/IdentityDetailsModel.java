package opc.models.sumsub;

public class IdentityDetailsModel {

    private String identityType;
    private String externalUserId;
    private String verificationFlow;
    private String accessToken;
    private String kycProviderKey;
    private String kybProviderKey;

    public String getIdentityType() {
        return identityType;
    }

    public IdentityDetailsModel setIdentityType(String identityType) {
        this.identityType = identityType;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public IdentityDetailsModel setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        return this;
    }

    public String getVerificationFlow() {
        return verificationFlow;
    }

    public IdentityDetailsModel setVerificationFlow(String verificationFlow) {
        this.verificationFlow = verificationFlow;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public IdentityDetailsModel setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getKycProviderKey() {
        return kycProviderKey;
    }

    public IdentityDetailsModel setKycProviderKey(String kycProviderKey) {
        this.kycProviderKey = kycProviderKey;
        return this;
    }

    public String getKybProviderKey() {
        return kybProviderKey;
    }

    public IdentityDetailsModel setKybProviderKey(String kybProviderKey) {
        this.kybProviderKey = kybProviderKey;
        return this;
    }
}
