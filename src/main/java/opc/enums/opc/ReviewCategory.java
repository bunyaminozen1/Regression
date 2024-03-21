package opc.enums.opc;

public enum ReviewCategory {

    ACCESS_CONTROL("access_control");

    private final String value;

    ReviewCategory(final String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
