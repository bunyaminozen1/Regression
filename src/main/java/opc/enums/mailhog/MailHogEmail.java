package opc.enums.mailhog;

import commons.config.ConfigHelper;

public enum MailHogEmail {

    CORPORATE_EMAIL_VERIFICATION("Weavr <noreply@weavr.io>",
        "Email Verification",
        "Email Verification Verify your email so that you can keep using Weavr. Verify email You can also copy and paste this URL into your web browser: https://www.fake.com/verify?email=%s&cons=&nonce=" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + " The link will remain valid for 1 hour(s)."),
    CONSUMER_EMAIL_VERIFICATION("Weavr <noreply@weavr.io>",
        "Email Verification",
        "Email Verification Verify your email so that you can keep using Weavr. Verify email You can also copy and paste this URL into your web browser: https://www.fakeconsumer.com/verify?email=%s&cons=%s&nonce=" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + " The link will remain valid for 1 hour(s)."),
    CORPORATE_USER_INVITE("Weavr <noreply@weavr.io>",
        "You have been invited",
        "You=E2=80=99ve been invited to join a Corporate account. %s %s h= as requested that you join the account %s. Get started by = creating an account. You can also copy and paste this URL into your web browser: https://www.fake.com/invite/= consume?identity_id=3D%s&identity_type=3Dcorporates&nonce= =3D" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + "&user_id=3D%s&invite_id=3D%s&email=3D%s If you did not make this request, please ignore this email. The link will remain valid for 30 day(s)."),
    CONSUMER_USER_INVITE("Weavr <noreply@weavr.io>",
        "You have been invited",
        "You=E2=80=99ve been invited to join a Consumer account. %s %s h= as requested that you join their account. Get started by = creating an account. You can also copy and paste this URL into your web browser: https://www.fakeconsu= mer.com/invite/consume?identity_id=3D%s&identity_type=3Dcon= sumers&nonce=3D" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + "&user_id=3D%s&invite_id=3D%s&email= =3D%s The link will remain valid for 30 day(s)."),
    CORPORATE_PASSWORD_RESET("Weavr <noreply@weavr.io>",
        "Password Reset",
        "Password reset We received a request to change the password for your Weavr account. If you did not make this request, just ignore this email. Otherwise, please click the button below to reset your password: Change Password You can also copy and paste this URL into your web browser: https://www.fake.com/set-password/" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + "/%s?env=" + ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment() + " The link will remain valid for 1 hour(s)."),
    CONSUMER_PASSWORD_RESET("Weavr <noreply@weavr.io>",
        "Password Reset",
        "Password reset We received a request to change the password for your Weavr account. If you did not make this request, just ignore this email. Otherwise, please click the button below to reset your password: Change Password You can also copy and paste this URL into your web browser: https://www.fakeconsumer.com/set-password/" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + "/%s?env=" + ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment() + " The link will remain valid for 1 hour(s)."),

    CORPORATE_PASSCODE_RESET("Weavr <noreply@weavr.io>",
        "Passcode Reset",
        "Passcode reset We received a request to change the passcode for your Weavr account. If you did not make this request, just ignore this email. Otherwise, please click the button below to reset your passcode: Change Passcode You can also copy and paste this URL into your web browser: https://www.fake.com/set-passcode/" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + "/%s?env=" + ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment() + " The link will remain valid for 1 hour(s)."),
    CONSUMER_PASSCODE_RESET("Weavr <noreply@weavr.io>",
        "Passcode Reset",
        "Passcode reset We received a request to change the passcode for your Weavr account. If you did not make this request, just ignore this email. Otherwise, please click the button below to reset your passcode: Change Passcode You can also copy and paste this URL into your web browser: https://www.fakeconsumer.com/set-passcode/" + ConfigHelper.getEnvironmentConfiguration().getVerificationCode() + "/%s?env=" + ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment() + " The link will remain valid for 1 hour(s)."),

    NEW_USER_CREATED_ALERT("Weavr <noreply@weavr.io>",
        "New user added to your identity",
        "Alert: new user added to your e-money account(s) Dear %s, A new user had been added to your e-money account(s). The details of the new user are: Name: %s %s Email:%s User invited by: %s If this is correct, you don't need to take action as the new user has already been sent their login details. If this was not correct or you do not recognise the above details, please contact support immediately. Best regards, %s"),

    PASSWORD_CHANGED_ALERT("Weavr <noreply@weavr.io>",
            "Security Update: Your Password Has Been Changed",
            "Security Update: Your Password Has Been Changed Dear %s %s, This is to inform you that the password associated with your account has been changed. If you made this change, please disregard this message. If you need further assistance or suspect any unauthorized access, please contact our support team."
    );
    private final String from;
    private final String subject;
    private final String emailText;

    MailHogEmail(final String from,
        final String subject,
        final String emailText){
        this.from = from;
        this.subject = subject;
        this.emailText = emailText;
    }

    public String getFrom(){
        return from;
    }

    public String getSubject(){
        return subject;
    }

    public String getEmailText(){
        return emailText;
    }
}