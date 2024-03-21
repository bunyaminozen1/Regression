package opc.models.shared;

import opc.models.backoffice.IdentityModel;

public class Identity {

    private IdentityModel identity;
    public Identity (IdentityModel identity){
        this.identity = identity;
    }

    public IdentityModel getIdentity(){;
        return this.identity;
    }
    public  Identity setIdentity(IdentityModel identity) {
        this.identity = identity;
        return this;
    }
}
