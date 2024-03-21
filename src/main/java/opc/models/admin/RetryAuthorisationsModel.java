package opc.models.admin;

import java.util.List;

public class RetryAuthorisationsModel {

    private List<String> authorisationId;
    private String retryType;
    private String note;

    public List<String> getAuthorisationId() {
        return authorisationId;
    }

    public RetryAuthorisationsModel setAuthorisationId(List<String> authorisationId) {
        this.authorisationId = authorisationId;
        return this;
    }

    public String getRetryType() {
        return retryType;
    }

    public RetryAuthorisationsModel setRetryType(String retryType) {
        this.retryType = retryType;
        return this;
    }

    public String getNote() {
        return note;
    }

    public RetryAuthorisationsModel setNote(String note) {
        this.note = note;
        return this;
    }

}
