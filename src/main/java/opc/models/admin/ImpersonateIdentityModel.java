package opc.models.admin;

public class ImpersonateIdentityModel {

    private String programmeId;
    private String identityId;

    public ImpersonateIdentityModel(String programmeId, String identityId) {
        this.programmeId = programmeId;
        this.identityId = identityId;
    }

    public String getProgrammeId() {
        return programmeId;
    }

    public ImpersonateIdentityModel setProgrammeId(String programmeId) {
        this.programmeId = programmeId;
        return this;
    }

    public String getIdentityId() {
        return identityId;
    }

    public ImpersonateIdentityModel setIdentityId(String identityId) {
        this.identityId = identityId;
        return this;
    }
}
