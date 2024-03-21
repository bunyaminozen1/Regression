package opc.models.sumsub;

import opc.enums.sumsub.IdDocType;

public class AddDocumentModel {

    private final String idDocType;
    private final String idDocSubType;
    private final String country;
    private final String number;
    private final String validUntil;

    public AddDocumentModel(final Builder builder) {
        this.idDocType = builder.idDocType;
        this.idDocSubType = builder.idDocSubType;
        this.country = builder.country;
        this.number = builder.number;
        this.validUntil = builder.validUntil;
    }

    public String getIdDocType() {
        return idDocType;
    }

    public String getIdDocSubType() {
        return idDocSubType;
    }

    public String getCountry() {
        return country;
    }
    public String getNumber() {
        return number;
    }
    public String getValidUntil() {return validUntil;}

    public static class Builder {
        private String idDocType;
        private String idDocSubType;
        private String country;
        private String number;
        private String validUntil;

        public Builder setIdDocType(String idDocType) {
            this.idDocType = idDocType;
            return this;
        }

        public Builder setIdDocSubType(String idDocSubType) {
            this.idDocSubType = idDocSubType;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setNumber(String number) {
            this.number = number;
            return this;
        }

        public Builder setValidUntil(String validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        public AddDocumentModel build() { return new AddDocumentModel(this); }
    }

    public static Builder defaultDocumentModel(final IdDocType docType, final String applicantId){
        return new Builder()
                .setCountry(docType.getCountry() == null ? "MLT" : docType.getCountry())
                .setIdDocType(docType.getType())
                .setNumber(String.format("Id - %s", applicantId))
                .setIdDocSubType(docType.getSubType())
                .setValidUntil("2030-01-01");
    }
}
