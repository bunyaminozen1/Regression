package opc.models.multi.managedcards;

import opc.enums.opc.IdentityType;

public class OwnerModel {
    private final IdentityType type;
    private final String id;

    public OwnerModel(IdentityType type, String id) {
        this.type = type;
        this.id = id;
    }

    public IdentityType getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
