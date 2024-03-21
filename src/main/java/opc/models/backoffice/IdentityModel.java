package opc.models.backoffice;

import opc.enums.opc.IdentityType;

public class IdentityModel {
    private String id;
    private String type;

    public IdentityModel(final String id, final IdentityType type) {
        this.id = id;
        this.type = type.name();
    }

    public String getId() {
        return id;
    }

    public IdentityModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public IdentityModel setType(String type) {
        this.type = type;
        return this;
    }
}
