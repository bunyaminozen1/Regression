package opc.enums.sumsub;

public enum CallbackType {

    PENDING("applicantPending"),
    REVIEWED("applicantReviewed"),
    WORKFLOW_COMPLETED("applicantWorkflowCompleted");

    private final String value;

    CallbackType(final String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}
