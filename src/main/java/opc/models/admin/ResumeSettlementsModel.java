package opc.models.admin;

import java.util.List;

public class ResumeSettlementsModel {

    private List<String> settlementId;
    private String note;

    public ResumeSettlementsModel(List<String> settlementId, String note) {
        this.settlementId = settlementId;
        this.note = note;
    }

    public List<String> getSettlementId() {
        return settlementId;
    }

    public ResumeSettlementsModel setSettlementId(List<String> settlementId) {
        this.settlementId = settlementId;
        return this;
    }

    public String getNote() {
        return note;
    }

    public ResumeSettlementsModel setNote(String note) {
        this.note = note;
        return this;
    }
}
