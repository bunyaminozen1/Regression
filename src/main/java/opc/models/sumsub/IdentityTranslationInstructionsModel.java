package opc.models.sumsub;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IdentityTranslationInstructionsModel {

    @JsonProperty("COMPANY_DOC")
    private String companyDoc;

    public String getCompanyDoc() {
        return companyDoc;
    }

    public void setCompanyDoc(String companyDoc) {
        this.companyDoc = companyDoc;
    }
}
