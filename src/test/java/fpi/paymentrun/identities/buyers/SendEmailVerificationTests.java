package fpi.paymentrun.identities.buyers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.BuyersService;
import opc.enums.mailhog.MailHogEmail;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
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

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class SendEmailVerificationTests extends BasePaymentRunSetup {

    private String buyerEmail;

    @BeforeEach
    public void Setup(){
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        BuyersHelper.createBuyer(createBuyerModel, secretKey);
        buyerEmail = createBuyerModel.getAdminUser().getEmail();
    }

    @Test
    public void SendEmailVerification_Success(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(buyerEmail);
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getSubject(), email.getSubject());
        assertEquals(buyerEmail, email.getTo());
        assertEquals(String.format(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getEmailText(), buyerEmail), email.getBody());
    }

    @Test
    public void SendEmailVerification_InvalidApiKey_Unauthorised(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendEmailVerification_NoApiKey_Unauthorised(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), "")
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_DifferentInnovatorApiKey_EmailNotFound(){
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void SendEmailVerification_EmailAlreadySent_Success(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_EmailAlreadyVerified_Success(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.verifyEmail(new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_NoEmail_BadRequest(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(""), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendEmailVerification_InvalidEmail_BadRequest(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(RandomStringUtils.randomAlphanumeric(6)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendEmailVerification_InvalidEmailFormat_BadRequest(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendEmailVerification_UnknownEmail_EmailNotFound(){
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6))), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }
}
