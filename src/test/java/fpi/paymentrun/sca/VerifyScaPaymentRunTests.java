package fpi.paymentrun.sca;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.services.PaymentRunsService;
import fpi.paymentrun.services.uicomponents.PaymentRunConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.models.shared.VerificationModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_SCA)
public class VerifyScaPaymentRunTests extends BasePaymentRunSetup {
    protected static final String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;
    protected static final String CHANNEL = EnrolmentChannel.SMS.name();
    private final static int SCA_EXPIRED_TIME = 61000;
    private static String buyerToken;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeAll
    public static void BuyerSetup() {
        buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
    }

    @Test
    public void VerifyIssueScaPaymentRun_MultipleRolesBuyerToken_Success() {
        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_ValidRoleBuyerToken_Success() {
        final String buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        BuyersHelper.assignControllerRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_ValidRoleAuthUserToken_Success() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_MultipleRolesAuthUserToken_Success() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser =
                BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignAllRoles(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_AdminRoleBuyerToken_Forbidden() {
        final String buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        BuyersHelper.assignAdminRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyIssueScaPaymentRun_IncorrectRoleBuyerToken_Forbidden() {
        final String buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        BuyersHelper.assignCreatorRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyIssueScaPaymentRun_IncorrectRoleAuthUserToken_Forbidden() {
        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignCreatorRole(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyIssueScaPaymentRun_UserVerifyScaStartedByBuyer_ChannelNotRegistered() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_BuyerVerifyScaStartedByUser_ChannelNotRegistered() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_OneUserVerifyScaStartedByAnotherUser_ChannelNotRegistered() {
        final Triple<String, BuyerAuthorisedUserModel, String> firstAuthUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(firstAuthUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        final Triple<String, BuyerAuthorisedUserModel, String> secondAuthUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(secondAuthUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(firstAuthUser.getRight(), sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), secondAuthUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_IssueNotStarted_NotFound() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifyIssueScaPaymentRun_IssueExpired_VerificationCodeExpired() throws InterruptedException {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        Thread.sleep(SCA_EXPIRED_TIME);

        PaymentRunsService.getPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", Matchers.equalTo(State.PENDING_CHALLENGE.name()));

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));
    }

    //TODO Expired time for this case is 5 min, we should think about how we can reduce it.
    @Disabled
    @Test
    public void VerifyIssueScaPaymentRun_IssueExpiredNewIssueRequest_Success() throws InterruptedException {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        Thread.sleep(300002);

        PaymentRunsService.getPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", Matchers.equalTo(State.PENDING_CONFIRMATION.name()));

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_InvalidVerificationCode_VerificationCodeInvalid() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(RandomStringUtils.randomNumeric(6)), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyIssueScaPaymentRun_InvalidVerificationCodeNewVerificationRequest_Success() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(RandomStringUtils.randomNumeric(6)), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());
    }

    @Test
    public void VerifyIssueScaPaymentRun_NoVerificationCode_BadRequest() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(null), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must not be blank"));
    }

    @Test
    public void VerifyIssueScaPaymentRun_InvalidToken_Unauthorized() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomAlphabetic(15), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyIssueScaPaymentRun_InvalidApiKey_Unauthorized() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, RandomStringUtils.randomAlphabetic(15), paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyIssueScaPaymentRun_MultiApiKey_Unauthorized() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, secretKeyMultiApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyIssueScaPaymentRun_NoToken_Unauthorized() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), null, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyIssueScaPaymentRun_InvalidChannel_BadRequest() {

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, RandomStringUtils.randomAlphabetic(5))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void VerifyIssueScaPaymentRun_InvalidPaymentRunId_BadRequest() {

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, RandomStringUtils.randomAlphanumeric(6), CHANNEL)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void VerifyIssueScaPaymentRun_WrongPaymentRunId_NotFound() {

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, "653a1d15a5e4e7c6ae02526e", CHANNEL)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifyIssueScaPaymentRun_BuyerLogout_Unauthorized() {
        final String buyerToken =
                BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp).getRight();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        AuthenticationHelper.logout(secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), buyerToken, sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyIssueScaPaymentRun_OtherBuyerTokenValidRole_NotFound() {
        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp);
        BuyersHelper.assignControllerRole(secretKeyPluginsScaApp, authenticatedBuyer.getRight());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), authenticatedBuyer.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifyIssueScaPaymentRun_OtherBuyerAuthUserTokenValidRole_NotFound() {
        final String paymentRunId = createPaymentRun(buyerToken);
        AuthenticationHelper.startIssueScaPaymentRun(buyerToken, sharedKeyPluginsScaApp, paymentRunId);

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp);
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, authenticatedBuyer.getRight());
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKeyPluginsScaApp, authenticatedBuyer.getRight());

        PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, CHANNEL)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private String createPaymentRun(final String buyerToken) {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        return PaymentRunsHelper.createConfirmedPaymentRun(createPaymentRunModel, secretKeyPluginsScaApp, buyerToken);
    }


    private static String createBuyer() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp);
        return authenticatedBuyer.getRight();
    }
}
