package opc.models.sumsub;

import java.util.List;

public class DocumentSetsTypeModel {

    private final String idDocSetType;
    private final List<DocumentSetsFieldsModel> fields;
    private final List<String> types;
    private final List<String> subTypes;

    public DocumentSetsTypeModel(final Builder builder) {
        this.idDocSetType = builder.idDocSetType;
        this.fields = builder.fields;
        this.types = builder.types;
        this.subTypes = builder.subTypes;
    }

    public String getIdDocSetType() {
        return idDocSetType;
    }

    public List<DocumentSetsFieldsModel> getFields() {
        return fields;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<String> getSubTypes() {
        return subTypes;
    }

    public static class Builder {
        private String idDocSetType;
        private List<DocumentSetsFieldsModel> fields;
        private List<String> types;
        private List<String> subTypes;

        public Builder setIdDocSetType(String idDocSetType) {
            this.idDocSetType = idDocSetType;
            return this;
        }

        public Builder setFields(List<DocumentSetsFieldsModel> fields) {
            this.fields = fields;
            return this;
        }

        public Builder setTypes(List<String> types) {
            this.types = types;
            return this;
        }

        public Builder setSubTypes(List<String> subTypes) {
            this.subTypes = subTypes;
            return this;
        }

        public DocumentSetsTypeModel build() { return new DocumentSetsTypeModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
