package opc.junit.multi.corporates;

import io.restassured.response.ValidatableResponse;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class VerifyEmailTests extends BaseCorporatesSetup {

    private String corporateEmail;
    private String corporateId;
    private String corporateToken;
    final private static int VERIFICATION_TIME_LIMIT = 90;

    @BeforeEach
    public void Setup(){
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        corporateId = CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        corporateEmail = createCorporateModel.getRootUser().getEmail();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        corporateToken = PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("token");
    }

    @Test
    public void VerifyEmail_Success() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_InvalidApiKey_Unauthorised(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEmail_NoApiKey_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, "")
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
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_UnknownVerificationCode_VerificationCodeInvalid(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, RandomStringUtils.randomNumeric(6));

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_MinVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, RandomStringUtils.randomNumeric(5));

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyEmail_MaxVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, RandomStringUtils.randomNumeric(7));

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyEmail_InvalidVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, RandomStringUtils.randomAlphanumeric(6));

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: must match \"^[0-9]+$\""));
    }

    @Test
    public void VerifyEmail_NoVerificationCode_BadRequest(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, "");

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_VerificationCodeNotSent_VerificationCodeInvalid(){

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_EmailAlreadyVerified_VerificationCodeInvalid(){
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_NoEmail_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel("", TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_InvalidEmail_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(RandomStringUtils.randomAlphanumeric(5), TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_InvalidEmailFormat_BadRequest(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)), TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_UnknownEmail_VerificationCodeInvalid(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6)),
                        TestHelper.VERIFICATION_CODE);

        CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    /**
     * corporate email should be verified in 15 sec
     * config for test environment:
     * corporate:
     *   clean:
     *     unverified-email:
     *       initialDelay: "5s"
     *       fixedDelay: "15s"
     *     validate-email-lifetime-second: 15
     */
    @Test
    public void VerifyEmail_AttemptAfterExpiry_VerificationCodeInvalid() throws InterruptedException, SQLException {
        sendSuccessfulVerification();
        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        verifyCorporateEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    /**
     * If first created corporate does not verify email in 15 sec, a new corporate can  be created with the same email and verify it
     * The selected_login field in corporate.corporate_user table indicates which user has the right to verify that email
     * If the value of this field is 1, it means that user has the right
     * config for test environment:
     * corporate:
     *   clean:
     *     unverified-email:
     *       initialDelay: "5s"
     *       fixedDelay: "15s"
     *     validate-email-lifetime-second: 15
     */
    @Test
    public void VerifyEmail_FirstCorporateAttemptVerifyAfterSecondCorporateVerified_VerificationCodeInvalid() throws InterruptedException, SQLException {
        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);
        sendSuccessfulVerification();

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second corporate choose the same email
        final String secondCorporateId = createCorporate();
        checkEmailVerification(secondCorporateId, "0");
        isAbleToVerifyEmail(secondCorporateId, true);
        isAbleToVerifyEmail(corporateId, false);

        //Second corporate send verification
        sendSuccessfulVerification();

        //Second user verify email
        verifyCorporateEmail()
                .statusCode(SC_NO_CONTENT);

        checkEmailVerification(secondCorporateId, "1");
        checkEmailVerification(corporateId, "0");

        //First user tries to verify email
        verifyCorporateEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_BothCorporateNotValidateInTimeSendVerificationAgain_Success() throws InterruptedException, SQLException {

        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);
        sendSuccessfulVerification();

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second corporate choose the same email
        final String secondCorporateId = createCorporate();
        checkEmailVerification(secondCorporateId, "0");
        isAbleToVerifyEmail(secondCorporateId, true);
        isAbleToVerifyEmail(corporateId, false);

        //Second corporate send verification
        sendSuccessfulVerification();
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second user try to verify email
        verifyCorporateEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        checkEmailVerification(secondCorporateId, "0");
        checkEmailVerification(corporateId, "0");

        //Second corporate send email verification again
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
    }

    @Test
    public void VerifyEmail_BothCorporateNotValidateInTimeThirdCorporateVerifyEmail_Success() throws InterruptedException, SQLException {

        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);
        sendSuccessfulVerification();

        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second corporate choose the same email
        final String secondCorporateId = createCorporate();
        checkEmailVerification(secondCorporateId, "0");
        isAbleToVerifyEmail(secondCorporateId, true);
        isAbleToVerifyEmail(corporateId, false);

        //Second corporate send verification
        sendSuccessfulVerification();
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        //Second user try to verify email
        verifyCorporateEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        checkEmailVerification(secondCorporateId, "0");
        checkEmailVerification(corporateId, "0");

        //Third corporate choose the same email
        final String thirdCorporateId = createCorporate();

        isAbleToVerifyEmail(corporateId, false);
        isAbleToVerifyEmail(secondCorporateId, false);
        isAbleToVerifyEmail(thirdCorporateId, true);

        //Third corporate verify email
        sendSuccessfulVerification();
        verifyCorporateEmail()
                .statusCode(SC_NO_CONTENT);

        //Third corporate's email should be verified
        checkEmailVerification(corporateId, "0");
        checkEmailVerification(secondCorporateId, "0");
        checkEmailVerification(thirdCorporateId, "1");
    }

    @Test
    public void VerifyEmail_NewSendVerificationResetExpiryUserVerifyEmail_Success() throws InterruptedException, SQLException {
        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(10);
        sendSuccessfulVerification();
        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(14);

        verifyCorporateEmail()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_SecondSendVerificationResetExpiryUserNotVerifyEmail_VerificationCodeInvalid() throws InterruptedException, SQLException {
        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(10);
        sendSuccessfulVerification();
        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        verifyCorporateEmail()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        CorporatesService.startCorporateKyb(secretKey, corporateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_UNVERIFIED"));
    }

    @Test
    public void VerifyEmail_ThirdSendVerificationResetExpiryUserVerifyEmail_Successful() throws InterruptedException, SQLException {
        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(12);
        sendSuccessfulVerification();
        checkEmailVerification(corporateId, "0");
        isAbleToVerifyEmail(corporateId, true);

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(12);
        sendSuccessfulVerification();

        //Waiting .... sec because of delay time in the config
        TimeUnit.SECONDS.sleep(12);
        sendSuccessfulVerification();

        verifyCorporateEmail()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.startCorporateKyb(secretKey, corporateToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    private void sendSuccessfulVerification(){
        CorporatesService.sendEmailVerification(new SendEmailVerificationModel(corporateEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String createCorporate(){

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setEmail(corporateEmail).build()).build();
        return CorporatesService.createCorporate(corporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id.id");
    }

    private void checkEmailVerification(final String identityId, final String status) throws SQLException {
        assertEquals(status, CorporatesDatabaseHelper.getCorporateUser(identityId).get(0).get("email_address_verified"));
    }

    private void isAbleToVerifyEmail(final String identityId, final boolean status) throws SQLException {
        final String selected_login = CorporatesDatabaseHelper.getCorporateUser(identityId).get(0).get("selected_login");

        if(status){
            assertEquals("1", selected_login);
        }else {
            assertNotEquals("1", selected_login);
        }
    }

    private ValidatableResponse verifyCorporateEmail(){
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(corporateEmail, TestHelper.VERIFICATION_CODE);

        return CorporatesService.verifyEmail(emailVerificationModel, secretKey)
                .then();
    }
}