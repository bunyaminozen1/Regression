package fpi.paymentrun.paymentruns;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.services.PaymentRunsService;
import opc.enums.opc.EnrolmentChannel;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.services.innovator.InnovatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN)
public class ConfirmPaymentRunTests extends BasePaymentRunSetup {
    protected static final String VERIFICATION_CODE = "123456";

    private static String buyerToken;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeAll
    public static void BuyerSetup() {
        multipleRolesBuyerSetup();
    }

    @Test
    public void ConfirmPaymentRun_ValidRoleBuyerFasterPaymentsDetails_Success() {
        final String paymentRunId = createPaymentRun(secretKey, buyerToken);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void ConfirmPaymentRun_ValidRoleAuthUser_Success() {
        final String controllerUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final String paymentRunId = createPaymentRun(secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, controllerUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void ConfirmPaymentRun_MultipleRolesAuthUser_Success() {
        final String multipleRolesUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final String paymentRunId = createPaymentRun(secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, multipleRolesUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void ConfirmPaymentRun_TwoAuthUsersCreatorController_Success() {
        final String creatorUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);
        final String controllerUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final String paymentRunId = createPaymentRun(secretKey, creatorUserToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, creatorUserToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, controllerUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, creatorUserToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
    }

    @Test
    public void ConfirmPaymentRun_CrossIdentityBuyer_PaymentNotFound() {
        final String paymentRunId = createPaymentRun(secretKey, buyerToken);

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String newBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignControllerRole(secretKey, newBuyerToken);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, newBuyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("PAYMENT_RUN_NOT_FOUND"));

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
    }

    @Test
    public void ConfirmPaymentRun_CrossIdentityAuthUser_PaymentNotFound() {
        final String paymentRunId = createPaymentRun(secretKey, buyerToken);

        final String newBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        BuyersHelper.assignControllerRole(secretKey, newBuyerToken);

        final String controllerUserToken = getControllerRoleAuthUserToken(secretKey, newBuyerToken);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, controllerUserToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("PAYMENT_RUN_NOT_FOUND"));

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
    }

    @Test
    public void ConfirmPaymentRun_AdminRoleBuyer_Forbidden() {
        final String paymentRunId = createPaymentRun(secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String adminBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, adminBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
    }

    @Test
    public void ConfirmPaymentRun_IncorrectRoleBuyer_Forbidden() {
        final String paymentRunId = createPaymentRun(secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String newBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, newBuyerToken);

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, newBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
    }

    @Test
    public void ConfirmPaymentRun_IncorrectRoleAuthUser_Forbidden() {
        final String creatorUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        final String paymentRunId = createPaymentRun(secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, creatorUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
    }

    @Test
    public void ConfirmPaymentRun_ConfirmTwice_PymentRunAlreadyConfirmed() {
        final String paymentRunId = createPaymentRun(secretKey, buyerToken);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_RUN_ALREADY_CONFIRMED"));
    }

    @Test
    public void ConfirmPaymentRun_WrongPaymentRunId_PaymentRunNotFound() {
        PaymentRunsService.confirmPaymentRun("6537dfd67ebeebcc5f5a62c8", secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("PAYMENT_RUN_NOT_FOUND"));
    }

    @Test
    public void ConfirmPaymentRun_InvalidPaymentRunId_BadRequest() {
        PaymentRunsService.confirmPaymentRun(RandomStringUtils.randomAlphanumeric(24), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ConfirmPaymentRun_NoPaymentRunId_NotFound() {
        PaymentRunsService.confirmPaymentRun("", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ConfirmPaymentRun_NoApiKey_Unauthorised() {
        PaymentRunsService.confirmPaymentRun("6537dfd67ebeebcc5f5a62c8", "", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConfirmPaymentRun_InvalidApiKey_Unauthorised() {
        PaymentRunsService.confirmPaymentRun("6537dfd67ebeebcc5f5a62c8", "abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void ConfirmPaymentRun_NoToken_Unauthorised(final String token) {
        PaymentRunsService.confirmPaymentRun("6537dfd67ebeebcc5f5a62c8", secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConfirmPaymentRun_InvalidToken_Unauthorised() {
        PaymentRunsService.confirmPaymentRun("6537dfd67ebeebcc5f5a62c8", secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConfirmPaymentRun_DifferentInnovatorApiKey_Unauthorised() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("secretKey");

        PaymentRunsService.confirmPaymentRun(RandomStringUtils.randomAlphanumeric(24), secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConfirmPaymentRun_LoggedOutBuyer_Unauthorised() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final String paymentRunId = PaymentRunsHelper.createPaymentRun(CreatePaymentRunModel
                .defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build(), secretKey, buyerToken).getId();

        AuthenticationHelper.logout(secretKey, buyerToken);

        PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private String createPaymentRun(final String secretKey,
                                    final String token) {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        return PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, token).getId();
    }


    private static void multipleRolesBuyerSetup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        buyerToken = authenticatedBuyer.getRight();

        BuyersHelper.verifyKyb(secretKey, authenticatedBuyer.getLeft());
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyerToken);
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKey), secretKey);
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyerToken);

        BuyersHelper.assignAllRoles(secretKey, buyerToken);
    }
}
