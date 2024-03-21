package opc.models.admin;

import java.util.List;

public class RetryTransfersModel {
    private List<String> transferId;
    private String note;
    public RetryTransfersModel(List<String> transferId, String note) {
        this.transferId = transferId;
        this.note = note;
    }

    public List<String> getTransferId() {
        return transferId;
    }

    public void setTransferId(List<String> transferId) {
        this.transferId = transferId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}
