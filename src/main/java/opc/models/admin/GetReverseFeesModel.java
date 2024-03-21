package opc.models.admin;

import java.util.List;

public class GetReverseFeesModel {

    private List<String> originalTransactionId;

    public GetReverseFeesModel(List<String> originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public List<String> getOriginalTransactionId() {
        return originalTransactionId;
    }

    public GetReverseFeesModel setOriginalTransactionId(List<String> originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
        return this;
    }
}
