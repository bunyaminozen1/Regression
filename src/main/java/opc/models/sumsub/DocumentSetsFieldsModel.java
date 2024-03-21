package opc.models.sumsub;

public class DocumentSetsFieldsModel {

    private String name;
    private boolean required;

    public DocumentSetsFieldsModel(final String name, final boolean required) {
        this.name = name;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public DocumentSetsFieldsModel setName(String name) {
        this.name = name;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public DocumentSetsFieldsModel setRequired(boolean required) {
        this.required = required;
        return this;
    }
}
