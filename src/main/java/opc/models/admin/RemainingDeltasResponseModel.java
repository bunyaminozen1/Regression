package opc.models.admin;

import java.util.List;

public class RemainingDeltasResponseModel {

    private List<RemainingDeltaModel> remainingDelta;

    public List<RemainingDeltaModel> getRemainingDelta() {
        return remainingDelta;
    }

    public RemainingDeltasResponseModel setRemainingDelta(List<RemainingDeltaModel> remainingDelta) {
        this.remainingDelta = remainingDelta;
        return this;
    }
}
