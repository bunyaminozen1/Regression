package opc.enums.authy;

public enum AuthyMessage {

    ENROL("Please confirm that you want to use this device to receive login and transaction verification requests for your TEST - %s account."),
    PAYMENT_INITIATION("Payment initiation request for TEST - %s account with the following details."),
    PUSH_STEPUP("Account information request for TEST - %s account with the following details.");

    private final String message;

    AuthyMessage(final String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }
}
