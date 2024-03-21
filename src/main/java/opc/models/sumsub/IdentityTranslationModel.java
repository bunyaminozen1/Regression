package opc.models.sumsub;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IdentityTranslationModel {

    private IdentityTranslationDocumentModel document;
    @JsonIgnore
    private Object idSubTypes;

    public IdentityTranslationDocumentModel getDocument() {
        return document;
    }

    public void setDocument(IdentityTranslationDocumentModel document) {
        this.document = document;
    }

    public Object getIdSubTypes() {
        return idSubTypes;
    }

    public void setIdSubTypes(Object idSubTypes) {
        this.idSubTypes = idSubTypes;
    }
}
