package opc.models.innovator;

import java.util.List;

public class GetFeesModel {

    private List<String> transactionId;

    public GetFeesModel(List<String> transactionId) {
        this.transactionId = transactionId;
    }

    public List<String> getTransactionId() {
        return transactionId;
    }

    public GetFeesModel setTransactionId(List<String> transactionId) {
        this.transactionId = transactionId;
        return this;
    }
}
