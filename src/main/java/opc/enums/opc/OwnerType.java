package opc.enums.opc;

public enum OwnerType {

    CORPORATE("corporates"),
    CONSUMER("consumers");

    private final String value;

    OwnerType(final String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
