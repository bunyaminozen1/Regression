package opc.junit.multi.corporates;

import opc.enums.mailhog.MailHogEmail;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendEmailVerificationTests extends BaseCorporatesSetup {

    private String corporateEmail;
    final private static int VERIFICATION_TIME_LIMIT = 90;

    @BeforeEach
    public void Setup(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        corporateEmail = createCorporateModel.getRootUser().getEmail();
    }

    @Test
    public void SendEmailVerification_Success() {
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(corporateEmail);
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getSubject(), email.getSubject());
        assertEquals(corporateEmail, email.getTo());
        assertEquals(String.format(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getEmailText(), corporateEmail), email.getBody());
    }

    @Test
    public void SendEmailVerification_InvalidApiKey_Unauthorised(){

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendEmailVerification_NoApiKey_BadRequest(){

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), "")
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_DifferentInnovatorApiKey_EmailNotFound(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void SendEmailVerification_EmailAlreadySent_Success() {
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_EmailAlreadyVerified_Success() {
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.verifyEmail(new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_NoEmail_BadRequest() {
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(""), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_InvalidEmail_BadRequest() {
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(RandomStringUtils.randomAlphanumeric(6)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }


    @Test
    public void SendEmailVerification_InvalidEmailFormat_BadRequest() {

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))),
                secretKey).then()
                .statusCode(SC_BAD_REQUEST);
    }


    @Test
    public void SendEmailVerification_UnknownEmail_EmailNotFound() {
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6))),
                secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    /**
     * Triggering the email validation can be done only within first 60 minutes(15 sec for test environments) from creation.
     */
    @Test
    public void SendEmailVerification_AfterExpiry_EmailNotFound() throws InterruptedException {
        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void SendEmailVerification_SameEmailDifferentProgrammes_Success() {

        final String corporateEmail = String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10));

        //Create corporate and send email verification code under applicationOne
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setRootUser(CorporateRootUserModel.DefaultRootUserModel().setEmail(corporateEmail).build()).build();

        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.verifyEmail(new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        //Create corporate and send email verification code under applicationThree
        final CreateCorporateModel createCorporateModelUnderAnotherProgramme =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).setRootUser(CorporateRootUserModel.DefaultRootUserModel().setEmail(corporateEmail).build()).build();

        CorporatesHelper.createCorporate(createCorporateModelUnderAnotherProgramme, applicationThree.getSecretKey());

        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.verifyEmail(new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
