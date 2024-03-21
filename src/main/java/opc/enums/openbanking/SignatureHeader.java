package opc.enums.openbanking;

public enum SignatureHeader {

    DATE("date"),
    DIGEST("digest"),
    TPP_SIGNATURE("TPP-Signature"),
    TPP_CONSENT_ID("tpp-consent-id");

    private final String name;

    SignatureHeader(final String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
