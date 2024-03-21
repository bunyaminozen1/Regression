package opc.enums.opc;

public enum AcceptedResponse {
    CSV("text/csv"),
    JSON("application/json"),
    PDF("application/pdf");

    private final String accept;

    AcceptedResponse(final String accept){
        this.accept = accept;
    }

    public String getAccept(){
        return accept;
    }
}
