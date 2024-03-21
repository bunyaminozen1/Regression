package fpi.paymentrun.identities.buyers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.BuyersService;
import opc.junit.helpers.TestHelper;
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

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class VerifyEmailTests extends BasePaymentRunSetup {

    private String buyerEmail;

    @BeforeEach
    public void Setup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        BuyersHelper.createBuyer(createBuyerModel, secretKey);
        buyerEmail = createBuyerModel.getAdminUser().getEmail();
    }

    @Test
    public void VerifyEmail_Success() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEmail_InvalidApiKey_Unauthorised() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEmail_NoApiKey_Unauthorised() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, "")
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEmail_DifferentInnovatorApiKey_VerificationCodeInvalid() {
        sendSuccessfulVerification();

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_UnknownVerificationCode_VerificationCodeInvalid() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, RandomStringUtils.randomNumeric(6));

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_MinVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, RandomStringUtils.randomNumeric(5));

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @Test
    public void VerifyEmail_MaxVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, RandomStringUtils.randomNumeric(7));

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
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
                new EmailVerificationModel(buyerEmail, RandomStringUtils.randomAlphanumeric(6));

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void VerifyEmail_EmptyVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, "");

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test
    public void VerifyEmail_NullVerificationCode_BadRequest() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, null);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void VerifyEmail_VerificationCodeNotSent_VerificationCodeInvalid() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_EmailAlreadyVerified_VerificationCodeInvalid() {
        sendSuccessfulVerification();

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(buyerEmail, TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEmail_EmptyEmail_BadRequest() {
        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel("", TestHelper.VERIFICATION_CODE);

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
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

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
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

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
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

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
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

        BuyersService.verifyEmail(emailVerificationModel, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    private void sendSuccessfulVerification() {
        BuyersService.sendEmailVerification(new SendEmailVerificationModel(buyerEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
