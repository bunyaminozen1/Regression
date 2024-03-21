package opc.models.backoffice;

import opc.enums.opc.IdentityType;

public class ImpersonateIdentityModel {

    private IdentityModel identity;

    public ImpersonateIdentityModel(final String identityId, final IdentityType identityType) {
        this.identity = new IdentityModel(identityId, identityType);
    }

    public IdentityModel getIdentity() {
        return identity;
    }

    public ImpersonateIdentityModel setIdentity(IdentityModel identity) {
        this.identity = identity;
        return this;
    }
}
