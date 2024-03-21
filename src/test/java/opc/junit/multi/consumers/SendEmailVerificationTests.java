package opc.junit.multi.consumers;

import opc.enums.mailhog.MailHogEmail;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SendEmailVerificationTests extends BaseConsumersSetup {

    private String consumerId;
    private String consumerEmail;
    final private static int VERIFICATION_TIME_LIMIT = 90;

    @BeforeEach
    public void Setup(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        consumerId = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey).getLeft();
        consumerEmail = createConsumerModel.getRootUser().getEmail();
    }

    @Test
    public void SendEmailVerification_Success() {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(consumerEmail);
        assertEquals(MailHogEmail.CONSUMER_EMAIL_VERIFICATION.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CONSUMER_EMAIL_VERIFICATION.getSubject(), email.getSubject());
        assertEquals(consumerEmail, email.getTo());
        assertEquals(String.format(MailHogEmail.CONSUMER_EMAIL_VERIFICATION.getEmailText(), consumerEmail, consumerId), email.getBody());
    }

    @Test
    public void SendEmailVerification_InvalidApiKey_Unauthorised(){

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendEmailVerification_NoApiKey_BadRequest(){

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), "")
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_DifferentInnovatorApiKey_EmailNotFound(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void SendEmailVerification_EmailAlreadySent_Success() {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_EmailAlreadyVerified_Success() {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.verifyEmail(new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    /**
     * Triggering the email validation can be done only within first 60 minutes from creation.
     * config for test environment:
     * consumer:
     *   clean:
     *     unverified-email:
     *       initialDelay: "5s"
     *       fixedDelay: "15s"
     *     validate-email-lifetime-second: 15
     */
    @Test
    public void SendEmailVerification_AfterExpiry_EmailNotFound() throws InterruptedException, SQLException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .build())
                .build();
        final String consumerId = ConsumersHelper.createConsumer(createConsumerModel, secretKey);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);
        assertNotEquals("1", ConsumersDatabaseHelper.getConsumerUser(consumerId).get(0).get("selected_login"));
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(createConsumerModel.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void SendEmailVerification_SameEmailDifferentProgrammes_Success() {

        final String consumerEmail = String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10));

        //Create consumer and send email verification code under applicationOne
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel().setEmail(consumerEmail)
                        .build())
                .build();
        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty());

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.verifyEmail(new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        //Create consumer and send email verification code under applicationThree
        final CreateConsumerModel createConsumerModelAnotherProgramme = CreateConsumerModel.DefaultCreateConsumerModel(applicationThree.getConsumersProfileId())
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel().setEmail(consumerEmail)
                        .build())
                .build();
        ConsumersService.createConsumer(createConsumerModelAnotherProgramme, applicationThree.getSecretKey(), Optional.empty());

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.verifyEmail(new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendEmailVerification_NoEmail_BadRequest() {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(""), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_InvalidEmail_BadRequest() {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(RandomStringUtils.randomAlphanumeric(6)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_InvalidEmailFormat_BadRequest() {

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))),
                secretKey).then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendEmailVerification_UnknownEmail_EmailNotFound() {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6))),
                secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }
}