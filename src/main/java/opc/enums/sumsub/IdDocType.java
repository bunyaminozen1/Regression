package opc.enums.sumsub;

public enum IdDocType {
    ID_CARD_FRONT("ID_CARD", "FRONT_SIDE", "sample_id_front2", null),
    ID_CARD_BACK("ID_CARD", "BACK_SIDE", "sample_id_back2", null),
    ID_CARD_FRONT_USER_IP("ID_CARD", "FRONT_SIDE", "sample_id_front_germany", "DEU"),
    ID_CARD_BACK_USER_IP("ID_CARD", "BACK_SIDE", "sample_id_back_germany", "DEU"),
    DRIVERS_FRONT("DRIVERS", "FRONT_SIDE", "drivers_id_front", "GBR"),
    DRIVERS_BACK("DRIVERS", "BACK_SIDE", "drivers_id_back", "GBR"),
    GERMAN_DRIVER_LICENCE("DRIVERS", null, "german_driving_license", "DEU"),
    PASSPORT("PASSPORT", null, "passport", null),
    PASSPORT_USER_IP("PASSPORT", null, "passport_germany", "DEU"),
    SELFIE("SELFIE", null, "sample_id3", null),
    SELFIE_USER_IP("SELFIE", null, "sample_id_germany", "DEU"),
    UTILITY_BILL("UTILITY_BILL", null, "sample_id", null),
    UTILITY_BILL2("UTILITY_BILL2", null, "sample_id", null),
    UTILITY_BILL_USER_IP("UTILITY_BILL", null, "sample_id_germany", "DEU"),
    COMPANY_POA("COMPANY_DOC", "PROOF_OF_ADDRESS", "sample_id", null),
    CERTIFICATE_OF_INCORPORATION("COMPANY_DOC", "INCORPORATION_CERT", "sample_id", null),
    ARTICLES_OF_INCORPORATION("COMPANY_DOC", "INCORPORATION_ARTICLES", "sample_id", null),
    SHAREHOLDER_REGISTRY("COMPANY_DOC", "SHAREHOLDER_REGISTRY", "sample_id", null),
    INFORMATION_STATEMENT("COMPANY_DOC", "INFORMATION_STATEMENT", "sample_id", null);

    private final String type;
    private final String subType;
    private final String fileName;
    private final String country;

    IdDocType(final String type, final String subType, final String fileName, final String country){
        this.type = type;
        this.subType = subType;
        this.fileName = fileName;
        this.country = country;
    }

    public String getType(){
        return type;
    }

    public String getSubType(){
        return subType;
    }

    public String getFileName(){
        return fileName;
    }

    public String getCountry(){
        return country;
    }
}
