package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.enums.mailhog.MailHogEmail;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class SendEmailVerificationTests extends BasePaymentRunSetup {

    private Pair<String, String> buyer;
    private String authorisedUserEmail;

    @BeforeEach
    public void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        final Triple<String, BuyerAuthorisedUserModel, String> user =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
        authorisedUserEmail = user.getMiddle().getEmail();
    }

    @Test
    public void SendEmailVerification_AdminRoleRootUser_Success() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(authorisedUserEmail);
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getSubject(), email.getSubject());
        assertEquals(authorisedUserEmail, email.getTo());
        assertEquals(String.format(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getEmailText(), authorisedUserEmail), email.getBody());
    }

    @Test
    public void SendEmailVerification_MultipleRolesRootUser_Success() {
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(authorisedUserEmail);
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getSubject(), email.getSubject());
        assertEquals(authorisedUserEmail, email.getTo());
        assertEquals(String.format(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getEmailText(), authorisedUserEmail), email.getBody());
    }

    @Test
    public void SendEmailVerification_InvalidApiKey_Unauthorised() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendEmailVerification_NoApiKey_Unauthorised() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), "")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void SendEmailVerification_DifferentInnovatorApiKey_EmailNotFound() {

        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKeyAppTwo)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void SendEmailVerification_EmailAlreadySent_Success() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_EmailAlreadyVerified_Success() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.verifyEmail(new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_EmptyEmail_BadRequest() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(""), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendEmailVerification_NullEmail_BadRequest() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(null), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void SendEmailVerification_InvalidEmail_BadRequest() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(RandomStringUtils.randomAlphanumeric(6)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendEmailVerification_InvalidEmailFormat_BadRequest() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendEmailVerification_UnknownEmail_EmailNotFound() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6))), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }
}