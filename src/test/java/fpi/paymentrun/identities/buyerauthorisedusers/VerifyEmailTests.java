package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.junit.helpers.TestHelper;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class VerifyEmailTests extends BasePaymentRunSetup {

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
    public void VerifyEmail_RootUser_Success() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_AuthorisedUser_Success() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_InvalidApiKey_Unauthorised() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEmail_NoApiKey_BadRequest() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, "")
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_DifferentInnovatorApiKey_VerificationCodeInvalid() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKeyAppTwo)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_UnknownVerificationCode_VerificationCodeInvalid() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, RandomStringUtils.randomNumeric(6));

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void VerifyEmail_InvalidVerificationCodeSize_BadRequest(final int codeSize) {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, RandomStringUtils.randomNumeric(codeSize));

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @Test
    public void VerifyEmail_InvalidVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, RandomStringUtils.randomAlphanumeric(6));

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void VerifyEmail_NoVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, "");

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void VerifyEmail_NullVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, null);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void VerifyEmail_VerificationCodeNotSent_VerificationCodeInvalid() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_EmailAlreadyVerified_VerificationCodeInvalid() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(authorisedUserEmail, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_NoEmail_BadRequest() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel("", TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void VerifyEmail_NullEmail_BadRequest() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(null, TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void VerifyEmail_InvalidEmail_BadRequest() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(RandomStringUtils.randomAlphanumeric(5), TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void VerifyEmail_InvalidEmailFormat_BadRequest() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)), TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void VerifyEmail_UnknownEmail_VerificationCodeInvalid() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6)), TestHelper.VERIFICATION_CODE);

        BuyersAuthorisedUsersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    private void sendSuccessfulVerification() {
        BuyersAuthorisedUsersService.sendEmailVerification(new SendEmailVerificationModel(authorisedUserEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}