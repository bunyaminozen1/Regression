package opc.models.sumsub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DocumentSetsModel {

    private final List<DocumentSetsTypeModel> docSets;
    private final List<String> includedCountries;
    private final List<String> excludedCountries;

    public DocumentSetsModel(final Builder builder) {
        this.docSets = builder.docSets;
        this.includedCountries = builder.includedCountries;
        this.excludedCountries = builder.excludedCountries;
    }

    public List<DocumentSetsTypeModel> getDocSets() {
        return docSets;
    }

    public List<String> getIncludedCountries() {
        return includedCountries;
    }

    public List<String> getExcludedCountries() {
        return excludedCountries;
    }

    public static class Builder {
        private List<DocumentSetsTypeModel> docSets;
        private List<String> includedCountries;
        private List<String> excludedCountries;

        public Builder setDocSets(List<DocumentSetsTypeModel> docSets) {
            this.docSets = docSets;
            return this;
        }

        public Builder setIncludedCountries(List<String> includedCountries) {
            this.includedCountries = includedCountries;
            return this;
        }

        public Builder setExcludedCountries(List<String> excludedCountries) {
            this.excludedCountries = excludedCountries;
            return this;
        }

        public DocumentSetsModel build() { return new DocumentSetsModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static DocumentSetsModel defaultDocumentSets(){
        return new Builder()
                .setDocSets(Arrays.asList(
                        DocumentSetsTypeModel.builder().setIdDocSetType("APPLICANT_DATA")
                                .setFields(Arrays.asList(
                                        new DocumentSetsFieldsModel("firstName", true),
                                        new DocumentSetsFieldsModel("lastName", true),
                                        new DocumentSetsFieldsModel("email", false),
                                        new DocumentSetsFieldsModel("phone", false),
                                        new DocumentSetsFieldsModel("dob", false),
                                        new DocumentSetsFieldsModel("country", false)))
                                .build(),
                        DocumentSetsTypeModel.builder()
                                .setIdDocSetType("IDENTITY")
                                .setTypes(Arrays.asList("ID_CARD", "PASSPORT"))
                                .setSubTypes(Arrays.asList("FRONT_SIDE", "BACK_SIDE")).build(),
                        DocumentSetsTypeModel.builder()
                                .setIdDocSetType("SELFIE")
                                .setTypes(Collections.singletonList("SELFIE")).build()))
                .setIncludedCountries(new ArrayList<>())
                .setExcludedCountries(new ArrayList<>())
                .build();
    }
}
