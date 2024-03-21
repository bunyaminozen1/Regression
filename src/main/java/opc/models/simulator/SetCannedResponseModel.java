package opc.models.simulator;

import opc.enums.opc.CannedResponseType;

public class SetCannedResponseModel {

    private CannedResponseType type;

    public SetCannedResponseModel(CannedResponseType type) {
        this.type = type;
    }

    public CannedResponseType getType() {
        return type;
    }

    public SetCannedResponseModel setType(CannedResponseType type) {
        this.type = type;
        return this;
    }
}
