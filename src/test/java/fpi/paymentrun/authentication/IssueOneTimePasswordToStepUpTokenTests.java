package fpi.paymentrun.authentication;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.AdminUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static opc.junit.helpers.TestHelper.OTP_VERIFICATION_CODE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION)
public class IssueOneTimePasswordToStepUpTokenTests extends BasePaymentRunSetup {

    final private static String CHANNEL = EnrolmentChannel.SMS.name();

    @Test
    public void IssueOneTimePassword_AdminUser_Success() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        AuthenticationService.startStepup(CHANNEL, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_UserNotEnrolled_ChannelNotRegistered() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.startStepup(CHANNEL, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void IssueOneTimePassword_SmsNotSupportedByProfile_ChannelNotSupported() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKeyAppTwo).getRight();

        AuthenticationService.startStepup(CHANNEL, secretKeyAppTwo, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }

    @Test
    public void IssueOneTimePassword_InvalidChannel_BadRequest() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        AuthenticationService.startStepup(EnrolmentChannel.AUTHY.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void IssueOneTimePassword_NoApiKey_BadRequest() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        AuthenticationService.startStepup(CHANNEL, "", buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOneTimePassword_InvalidApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        AuthenticationService.startStepup(CHANNEL, "123", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOneTimePassword_InvalidToken_Unauthorised() {
        AuthenticationService.startStepup(CHANNEL, secretKey, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void IssueOneTimePassword_NoToken_Unauthorised(final String token) {
        AuthenticationService.startStepup(CHANNEL, secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOneTimePassword_UserLoggedOut_Unauthorised() {
        final String buyerToken = BuyersHelper.createUnauthenticatedBuyer(secretKey).getRight();

        AuthenticationService.startStepup(CHANNEL, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOneTimePassword_BackofficeImpersonator_Forbidden() {
        final String buyerId = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getLeft();
        AuthenticationService.startStepup(CHANNEL, secretKey, getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueOneTimePassword_DifferentInnovatorApiKey_Forbidden() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.startStepup(CHANNEL, secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueOneTimePassword_OtherApplicationSecretKey_Forbidden() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        AuthenticationService.startStepup(CHANNEL, secretKeyAppTwo, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueOneTimePassword_NoChannel_NotFound() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        AuthenticationService.startStepup("", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    /**
     * With new feature, we allow the user having two successful (204) retries, with 15 seconds delay between retries, within the same session.
     */
    @Test
    public void IssueOneTimePassword_UserRetryWithin15Seconds_RetryIn15Sec() {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);
        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));
    }

    @Test
    public void IssueOneTimePassword_TwoRetriesIn15SecondOneRetryAfter15Seconds_Success() throws InterruptedException {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);
        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_UserRetryAfter15Seconds_Success() throws InterruptedException {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);
        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_UserIssueThreeTimesInOneSession_BadRequest() throws InterruptedException {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);
        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOneTimePassword_UserIssueChallengeInNewSession_Success() throws InterruptedException {
        final String email = String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                RandomStringUtils.randomAlphabetic(5));
        final CreateBuyerModel buyerModel = CreateBuyerModel.defaultCreateBuyerModel()
                .adminUser(AdminUserModel.defaultAdminUserModel()
                        .email(email)
                        .build())
                .build();
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(buyerModel, secretKey);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);

//        create new Session
        AuthenticationHelper.logout(secretKey, enrolledBuyer.getRight());
        final String newToken = AuthenticationHelper.login(email, TestHelper.getDefaultPassword(secretKey), secretKey);

        AuthenticationService.startStepup(CHANNEL, secretKey, newToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_UserIssueAgainAfterFirstOneVerifiedAfter15Sec_Success() throws InterruptedException {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);
        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.verifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKey, enrolledBuyer.getRight());

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_UserIssueAgainWithin15SecondsAfterFirstOneVerified_RetryIn15Sec() {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);
        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.verifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKey, enrolledBuyer.getRight());

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));
    }

    @Test
    public void IssueOneTimePassword_UserIssueAgainWithin15SecondsAfterFirstOneDeclined_RetryIn15Sec() {
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel("000000"), CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));
    }

    @Test
    public void IssueOneTimePassword_NewSessionAfterFirstOneDeclined_Success() {
        final String email = String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                RandomStringUtils.randomAlphabetic(5));
        final CreateBuyerModel buyerModel = CreateBuyerModel.defaultCreateBuyerModel()
                .adminUser(AdminUserModel.defaultAdminUserModel()
                        .email(email)
                        .build())
                .build();
        final Pair<String, String> enrolledBuyer = BuyersHelper.createEnrolledVerifiedBuyer(buyerModel, secretKey);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel("000000"), CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledBuyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));

//        create new Session
        AuthenticationHelper.logout(secretKey, enrolledBuyer.getRight());
        final String newToken = AuthenticationHelper.login(email, TestHelper.getDefaultPassword(secretKey), secretKey);

        AuthenticationService.startStepup(CHANNEL, secretKey, newToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
