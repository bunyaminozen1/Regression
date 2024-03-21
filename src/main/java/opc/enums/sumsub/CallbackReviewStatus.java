package opc.enums.sumsub;

public enum CallbackReviewStatus {

    PENDING("pending"),
    COMPLETED("completed");

    private final String value;

    CallbackReviewStatus(final String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}
