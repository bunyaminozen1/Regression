package opc.models.admin;

public class ResumeTransactionModel {

    private String note;

    public ResumeTransactionModel(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public ResumeTransactionModel setNote(String note) {
        this.note = note;
        return this;
    }
}
