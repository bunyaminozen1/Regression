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
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
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
public class IssueScaPaymentRunRequestTests extends BasePaymentRunSetup {
    protected static final String CHANNEL = EnrolmentChannel.SMS.name();
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
    public void StartIssueScaPaymentRun_MultipleRolesBuyerToken_Success() {
        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void StartIssueScaPaymentRun_ValidRoleBuyerToken_Success() {
        final String buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        BuyersHelper.assignControllerRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void StartIssueScaPaymentRun_ValidRoleAuthUserToken_Success() {
        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void StartIssueScaPaymentRun_MultipleRolesAuthUserToken_Success() {
        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignAllRoles(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void StartIssueScaPaymentRun_AdminRoleBuyerToken_Forbidden() {
        final String buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        BuyersHelper.assignAdminRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartIssueScaPaymentRun_IncorrectRoleAuthUserToken_Forbidden() {
        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, buyerToken);
        BuyerAuthorisedUserHelper.assignCreatorRole(authUser.getLeft(), secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartIssueScaPaymentRun_IncorrectRoleBuyerToken_Forbidden() {
        final String buyerToken = createBuyer();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        BuyersHelper.assignCreatorRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartIssueScaPaymentRun_PaymentRunNotConfirmed_Conflict() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        PaymentRunsService.getPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", Matchers.equalTo(State.PENDING_CONFIRMATION.name()));
    }

    @Test
    public void StartIssueScaPaymentRun_InvalidPaymentRunId_BadRequest() {

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, RandomStringUtils.randomAlphanumeric(10), EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void StartIssueScaPaymentRun_WrongPaymentRunId_NotFound() {

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, "653a1d15a5e4e7c6ae02526e", EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartIssueScaPaymentRun_InvalidToken_Unauthorised() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(RandomStringUtils.randomAlphabetic(15), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartIssueScaPaymentRun_InvalidApiKey_Unauthorised() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, RandomStringUtils.randomAlphabetic(8), paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartIssueScaPaymentRun_MultiAppApiKey_Unauthorised() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, secretKeyMultiApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartIssueScaPaymentRun_NoToken_Unauthorised() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(null, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartIssueScaPaymentRun_BuyerLogout_Unauthorized() {
        final String buyerToken =
                BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp).getRight();
        BuyersHelper.assignCreatorRole(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken);

        AuthenticationHelper.logout(secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartIssueScaPaymentRun_InvalidChannel_BadRequest() {

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunConsentService.issueScaChallengeRequest(buyerToken, sharedKeyPluginsScaApp, paymentRunId, RandomStringUtils.randomAlphabetic(4))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void StartIssueScaPaymentRun_OtherBuyerTokenValidRole_NotFound() {
        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp);
        BuyersHelper.assignControllerRole(secretKeyPluginsScaApp, authenticatedBuyer.getRight());

        PaymentRunConsentService.issueScaChallengeRequest(authenticatedBuyer.getRight(), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartIssueScaPaymentRun_OtherBuyerAuthUserTokenValidRole_NotFound() {
        final String paymentRunId = createPaymentRun(buyerToken);
        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp);
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKeyPluginsScaApp, authenticatedBuyer.getRight());
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKeyPluginsScaApp, authenticatedBuyer.getRight());

        PaymentRunConsentService.issueScaChallengeRequest(authUser.getRight(), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartIssueScaPaymentRun_MultiAppIdentity_Unauthorised() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdMultiApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyMultiApp);

        final String paymentRunId = createPaymentRun(buyerToken);

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunConsentService.issueScaChallengeRequest(authenticatedCorporate.getRight(), sharedKeyPluginsScaApp, paymentRunId, EnrolmentChannel.SMS.name())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private String createPaymentRun(final String buyerToken) {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        return PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKeyPluginsScaApp, buyerToken).getId();
    }

    private static String createBuyer() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKeyPluginsScaApp);
        return authenticatedBuyer.getRight();
    }
}
