package opc.junit.multi.access;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.shared.VerificationModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;

import static opc.junit.helpers.TestHelper.OTP_VERIFICATION_CODE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractIssueOneTimePasswordToStepUpTokenTests extends BaseAuthenticationSetup {

    final private static String CHANNEL = EnrolmentChannel.SMS.name();
    protected abstract IdentityDetails getEnrolledIdentity(final ProgrammeDetailsModel programme);
    protected abstract IdentityDetails getIdentity(final ProgrammeDetailsModel programme);
    protected abstract IdentityDetails createEnrolledUser(final String identityToken);

    @Test
    public void IssueOneTimePassword_RootUser_Success(){
        final IdentityDetails identity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, identity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_IdentityNotEnrolled_ChannelNotRegistered(){
        final IdentityDetails identity = getIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void IssueOneTimePassword_SmsNotSupported_ChannelNotSupported(){
        final IdentityDetails identity = getEnrolledIdentity(applicationThree);

        AuthenticationService.startStepup(CHANNEL, applicationThree.getSecretKey(), identity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }

    @Test
    public void IssueOneTimePassword_InvalidChannel_BadRequest(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(EnrolmentChannel.AUTHY.name(), secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOneTimePassword_NoApiKey_BadRequest(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, "", enrolledIdentity.getToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOneTimePassword_InvalidApiKey_Unauthorised(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, "123", enrolledIdentity.getToken())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOneTimePassword_InvalidToken_Unauthorised(){
        AuthenticationService.startStepup(CHANNEL, secretKey, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOneTimePassword_RootUserLoggedOut_Unauthorised(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.logout(secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOneTimePassword_BackofficeImpersonator_Forbidden(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey,
                getBackofficeImpersonateToken(enrolledIdentity.getId(), enrolledIdentity.getIdentityType()))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueOneTimePassword_DifferentInnovatorApiKey_Forbidden(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, nonFpsEnabledTenant.getSecretKey(), enrolledIdentity.getToken())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueOneTimePassword_OtherApplicationSecretKey_Forbidden(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, applicationThree.getSecretKey(), enrolledIdentity.getToken())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueOneTimePassword_NoChannel_NotFound(){
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup("", secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    /**
     * With new feature, we allow the user having two retries, with 15 seconds between retries, within the same session.
     */

    @Test
    public void IssueOneTimePassword_RootUserRetryWithin15Seconds_RetryIn15Sec() throws InterruptedException{
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(10);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_AuthorizedUserRetryWithin15Seconds_RetryIn15Sec() throws InterruptedException {
        final IdentityDetails user = createEnrolledUser(getIdentity(applicationOne).getToken());

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(10);

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));
    }

    @Test
    public void IssueOneTimePassword_RootUserIssueAgainAfter15Seconds_Success() throws InterruptedException {
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_AuthorizedUserIssueAgainAfter15Seconds_Success() throws InterruptedException {
        final IdentityDetails user = createEnrolledUser(getIdentity(applicationOne).getToken());

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void IssueOneTimePassword_RootUserIssueThreeTimesInOneSession_BadRequest() throws InterruptedException {

        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOneTimePassword_AuthorizedUserIssueThreeTimesInOneSession_BadRequest() throws InterruptedException {

        final IdentityDetails user = createEnrolledUser(getIdentity(applicationOne).getToken());

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void  IssueOneTimePassword_RootUserIssueAgainAfterFirstOneVerified_Success() throws InterruptedException {
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.verifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKey, enrolledIdentity.getToken());

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void  IssueOneTimePassword_AuthorizedUserIssueAgainAfter15SecondsFirstOneVerified_Success() throws InterruptedException {
        final IdentityDetails user = createEnrolledUser(getIdentity(applicationOne).getToken());

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.verifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKey, user.getToken());

        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(CHANNEL, secretKey, user.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void  IssueOneTimePassword_UserIssueAgainWithin15SecondsAfterFirstOneVerified_RetryIn15Sec() {
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.verifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKey, enrolledIdentity.getToken());

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));
    }

    @Test
    public void  IssueOneTimePassword_UserIssueAgainWithin15SecondsAfterFirstOneDeclined_RetryIn15Sec() {
        final IdentityDetails enrolledIdentity = getEnrolledIdentity(applicationOne);

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel("000000"), CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        AuthenticationService.startStepup(CHANNEL, secretKey, enrolledIdentity.getToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("RETRY_IN_15SEC"));
    }
}
