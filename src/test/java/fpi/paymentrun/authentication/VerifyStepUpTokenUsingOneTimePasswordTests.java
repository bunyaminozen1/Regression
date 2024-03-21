package fpi.paymentrun.authentication;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.services.AuthenticationService;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION)
public class VerifyStepUpTokenUsingOneTimePasswordTests extends BasePaymentRunSetup {

    private final static String VERIFICATION_CODE = "123456";

    @Test
    public void VerifyStepUpToken_AdminUser_Success() {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpToken_NoApiKey_BadRequest() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepupNoApiKey(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void VerifyStepUpToken_InvalidApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), "123", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_EmptyApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), "", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_InvalidToken_Unauthorised() {

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void VerifyStepUpToken_NoToken_Unauthorised(final String token) {

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_UserLoggedOut_Unauthorised() {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        AuthenticationHelper.logout(secretKey, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_EmptyVerificationCode_BadRequest() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepup(new VerificationModel(""), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Bad Request"));
    }

    @Test
    public void VerifyStepUpToken_NoVerificationCode_BadRequest() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepup(new VerificationModel(null), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void VerifyStepUpToken_InvalidChannel_BadRequest() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.AUTHY.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void VerifyStepUpToken_EmptyChannel_BadRequest() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), "", secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));;
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "7"})
    public void VerifyStepUpToken_MaxMinVerificationCode_BadRequest(final int verificationCode) {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        String code = RandomStringUtils.randomNumeric(verificationCode);
        AuthenticationService.verifyStepup(new VerificationModel(code), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyStepUpToken_UnknownVerificationCode_VerificationCodeInvalid() {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(RandomStringUtils.randomNumeric(6)),
                        EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyStepUpToken_InvalidVerificationCode_BadRequest() {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(RandomStringUtils.randomAlphanumeric(6)),
                        EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must match \"^[0-9]*$\""));
    }

    @Test
    public void VerifyStepUpToken_VerificationCodeExpired_Conflict() throws SQLException {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(secretKey);

        AuthenticationHelper.startStepup(secretKey, buyer.getRight());

        AuthFactorsDatabaseHelper.updateAccountInformationState("EXPIRED", buyer.getLeft());

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    public void VerifyStepUpToken_RequestAlreadyProcessed_AlreadyVerified() {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ALREADY_VERIFIED"));
    }

    @Test
    public void VerifyStepUpToken_BackofficeImpersonator_Unauthorised() {
        final String buyerId = BuyersHelper.createEnrolledBuyer(secretKey).getLeft();

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey,
                        getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_DifferentInnovatorApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_OtherApplicationSecretKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createEnrolledBuyer(secretKey).getRight();

        AuthenticationHelper.startStepup(secretKey, buyerToken);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKeyAppTwo, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
