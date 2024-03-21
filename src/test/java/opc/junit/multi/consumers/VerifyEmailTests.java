package opc.junit.multi.consumers;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.KycLevel;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import opc.services.multi.PasswordsService;
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
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class VerifyEmailTests extends BaseConsumersSetup {

    private String consumerEmail;
    private String consumerId;
    private String consumerToken;
    private CreateConsumerModel createConsumerModel;
    final private static int VERIFICATION_TIME_LIMIT = 90;

    @BeforeEach
    public void Setup(){

        createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        consumerEmail = createConsumerModel.getRootUser().getEmail();
        consumerId = ConsumersHelper.createConsumer(createConsumerModel, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        consumerToken = PasswordsService.createPassword(createPasswordModel, consumerId, secretKey)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("token");
    }

    @Test
    public void VerifyEmail_Success() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_InvalidApiKey_Unauthorised(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEmail_NoApiKey_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, "")
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_DifferentInnovatorApiKey_VerificationCodeInvalid(){
        sendSuccessfulVerification();

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_UnknownVerificationCode_VerificationCodeInvalid(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, RandomStringUtils.randomNumeric(6));

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_MinVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, RandomStringUtils.randomNumeric(5));

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyEmail_MaxVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, RandomStringUtils.randomNumeric(7));

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyEmail_InvalidVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, RandomStringUtils.randomAlphanumeric(6));

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: must match \"^[0-9]+$\""));
    }

    @Test
    public void VerifyEmail_NoVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, "");

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_VerificationCodeNotSent_VerificationCodeInvalid(){

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_EmailAlreadyVerified_VerificationCodeInvalid(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_NoEmail_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel("", TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_InvalidEmail_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(RandomStringUtils.randomAlphanumeric(5), TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_InvalidEmailFormat_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)), TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_UnknownEmail_VerificationCodeInvalid(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6)),
                        TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    /**
     * Consumer email should be verified in 15 sec
     * config for test environment:
     * consumer:
     *   clean:
     *     unverified-email:
     *       initialDelay: "5s"
     *       fixedDelay: "15s"
     *     validate-email-lifetime-second: 15
     */
    @Test
    public void VerifyEmail_AttemptAfterExpiry_VerificationCodeInvalid() throws InterruptedException, SQLException {
        sendSuccessfulVerification();
        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        verifyConsumerEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    /**
     * If first created consumer does not verify email in 15 sec, a new consumer can  be created with the same email and verify it
     * The selected_login field in consumer.consumer_user table indicates which user has the right to verify that email
     * If the value of this field is 1, it means that user has the right
     * config for test environment:
     * consumer:
     *   clean:
     *     unverified-email:
     *       initialDelay: "5s"
     *       fixedDelay: "15s"
     *     validate-email-lifetime-second: 15
     */
    @Test
    public void VerifyEmail_FirstConsumerAttemptVerifyAfterSecondConsumerVerified_VerificationCodeInvalid() throws InterruptedException, SQLException {
        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);
        sendSuccessfulVerification();

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second consumer choose the same email
        final String secondConsumerId = createConsumer();
        checkEmailVerification(secondConsumerId, "0");
        isAbleToVerifyEmail(secondConsumerId, true);
        isAbleToVerifyEmail(consumerId, false);

        //Second consumer send verification
        sendSuccessfulVerification();

        //Second user verify email
        verifyConsumerEmail()
                .statusCode(SC_NO_CONTENT);

        checkEmailVerification(secondConsumerId, "1");
        checkEmailVerification(consumerId, "0");

        //First user tries to verify email
        verifyConsumerEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_BothConsumerNotValidateInTimeSendVerificationAgain_Success() throws InterruptedException, SQLException {

        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);
        sendSuccessfulVerification();

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second consumer choose the same email
        final String secondConsumerId = createConsumer();
        checkEmailVerification(secondConsumerId, "0");
        isAbleToVerifyEmail(secondConsumerId, true);
        isAbleToVerifyEmail(consumerId, false);

        //Second consumer send verification
        sendSuccessfulVerification();
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second user try to verify email
        verifyConsumerEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        checkEmailVerification(secondConsumerId, "0");
        checkEmailVerification(consumerId, "0");

        //Second consumer send email verification again
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void VerifyEmail_BothConsumerNotValidateInTimeThirdConsumerVerifyEmail_Success() throws InterruptedException, SQLException {

        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);
        sendSuccessfulVerification();

        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second consumer choose the same email
        final String secondConsumerId = createConsumer();
        checkEmailVerification(secondConsumerId, "0");
        isAbleToVerifyEmail(secondConsumerId, true);
        isAbleToVerifyEmail(consumerId, false);

        //Second consumer send verification
        sendSuccessfulVerification();
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second user try to verify email
        verifyConsumerEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        checkEmailVerification(secondConsumerId, "0");
        checkEmailVerification(consumerId, "0");

        //Third consumer choose the same email
        final String thirdConsumerId = createConsumer();

        isAbleToVerifyEmail(consumerId, false);
        isAbleToVerifyEmail(secondConsumerId, false);
        isAbleToVerifyEmail(thirdConsumerId, true);

        //Third consumer verify email
        sendSuccessfulVerification();
        verifyConsumerEmail()
                .statusCode(SC_NO_CONTENT);

        //Third consumer's email should be verified
        checkEmailVerification(consumerId, "0");
        checkEmailVerification(secondConsumerId, "0");
        checkEmailVerification(thirdConsumerId, "1");
    }

    @Test
    public void VerifyEmail_NewSendVerificationResetExpiryUserVerifyEmail_Success() throws InterruptedException, SQLException {
        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(14);
        sendSuccessfulVerification();
        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(14);

        verifyConsumerEmail()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_SecondSendVerificationResetExpiryUserNotVerifyEmail_VerificationCodeInvalid() throws InterruptedException, SQLException {
        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(14);
        sendSuccessfulVerification();
        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        verifyConsumerEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_UNVERIFIED"));
    }

    @Test
    public void VerifyEmail_ThirdSendVerificationResetExpiryUserVerifyEmail_Successful() throws InterruptedException, SQLException {
        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(14);
        sendSuccessfulVerification();
        checkEmailVerification(consumerId, "0");
        isAbleToVerifyEmail(consumerId, true);

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(14);
        sendSuccessfulVerification();

        verifyConsumerEmail()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    private void sendSuccessfulVerification(){
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(consumerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String createConsumer(){
        return ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id.id");
    }

    private ValidatableResponse verifyConsumerEmail(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(consumerEmail, TestHelper.VERIFICATION_CODE);

        return ConsumersService.verifyEmail(emailVerificationModel, secretKey)
                .then();
    }

    private void checkEmailVerification(final String identityId, final String status) throws SQLException {
        assertEquals(status, ConsumersDatabaseHelper.getConsumer(identityId).get(0).get("email_address_verified"));
    }

    private void isAbleToVerifyEmail(final String identityId, final boolean status) throws SQLException {
        final String selected_login = ConsumersDatabaseHelper.getConsumerUser(identityId).get(0).get("selected_login");

        if(status){
            assertEquals("1", selected_login);
        }else {
            assertNotEquals("1", selected_login);
        }
    }
}
