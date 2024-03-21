package opc.models.admin;

import java.util.List;

public class ResumeSendsModel {

    private List<String> sendId;
    private String note;

    public ResumeSendsModel(List<String> sendId, String note) {
        this.sendId = sendId;
        this.note = note;
    }

    public List<String> getSendId() {
        return sendId;
    }

    public ResumeSendsModel setSendId(List<String> sendId) {
        this.sendId = sendId;
        return this;
    }

    public String getNote() {
        return note;
    }

    public ResumeSendsModel setNote(String note) {
        this.note = note;
        return this;
    }
}
