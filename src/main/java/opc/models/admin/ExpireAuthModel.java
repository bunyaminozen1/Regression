package opc.models.admin;

import java.util.List;

public class ExpireAuthModel {

    private List<String> providerLinkIds;
    private String note;

    public ExpireAuthModel(List<String> providerLinkIds, String note) {
        this.providerLinkIds = providerLinkIds;
        this.note = note;
    }

    public List<String> getProviderLinkIds() {
        return providerLinkIds;
    }

    public ExpireAuthModel setProviderLinkIds(List<String> providerLinkIds) {
        this.providerLinkIds = providerLinkIds;
        return this;
    }

    public String getNote() {
        return note;
    }

    public ExpireAuthModel setNote(String note) {
        this.note = note;
        return this;
    }
}
