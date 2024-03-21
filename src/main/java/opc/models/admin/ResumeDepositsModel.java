package opc.models.admin;

import java.util.List;

public class ResumeDepositsModel {

    private List<String> depositId;
    private String note;

    public ResumeDepositsModel(List<String> depositId, String note) {
        this.depositId = depositId;
        this.note = note;
    }

    public List<String> getDepositId() {
        return depositId;
    }

    public ResumeDepositsModel setDepositId(List<String> depositId) {
        this.depositId = depositId;
        return this;
    }

    public String getNote() {
        return note;
    }

    public ResumeDepositsModel setNote(String note) {
        this.note = note;
        return this;
    }
}
