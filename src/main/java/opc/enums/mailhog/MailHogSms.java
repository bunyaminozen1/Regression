package opc.enums.mailhog;

public enum MailHogSms {

    SCA_SMS_ENROL("noreply-qa@weavr.io", "%s verification code is %s. It will expire in 1 minutes."),
    SCA_INITIATE_PAYMENT ("noreply-qa@weavr.io", "%s verification code for payment of %s%s to %s is %s. It will expire in 1 minutes."),
    SCA_CHANGE_SMS("noreply-qa@weavr.io", "Your account's mobile number ending in %s has been updated with %s. If you haven't requested this change, please contact support."),
    SCA_NEW_ENROLMENT_ALERT("noreply-qa@weavr.io", "You have successfully enrolled for biometrics on another device. If you haven't requested this change, please contact support."),
    SCA_VARIABLE_RECURRING_PAYMENTS("noreply-qa@weavr.io", "Your verification code is %s. Use this code to confirm your recurring payment order.");

    private final String from;
    private final String smsText;

    MailHogSms(final String from,
               final String smsText){
        this.from = from;
        this.smsText = smsText;
    }

    public String getFrom(){
        return from;
    }

    public String getSmsText(){
        return smsText;
    }
}
