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
import org.hamcrest.CoreMatchers;
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
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN)
public class CancelPaymentRunTests extends BasePaymentRunSetup {
    protected static final String VERIFICATION_CODE = "123456";

    private static String buyerToken;

    /**
     * Required user role: CREATOR
     */

    @BeforeAll
    public static void BuyerSetup() {
        creatorBuyerSetup();
    }

    @Test
    public void CancelPaymentRun_ValidRoleBuyerNotVerifiedPaymentRun_Success() {
        final String paymentRunId = createPaymentRun();

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsService.getPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo(State.CANCELLED.name()))
                .body("payments[0].status", equalTo(State.CANCELLED.name()));
    }

    @Test
    public void CancelPaymentRun_CrossIdentityBuyer_PaymentNotFound() {
        final String paymentRunId = createPaymentRun();

//        create new Buyer with CREATOR role
        final String creatorBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, creatorBuyerToken);

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, creatorBuyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("PAYMENT_RUN_NOT_FOUND"))
                .body("message", equalTo("Payment run not found"));
    }

    @Test
    public void CancelPaymentRun_CrossIdentityAuthUser_PaymentNotFound() {
        final String paymentRunId = createPaymentRun();

//        create new Buyer and User with CREATOR role
        final String newBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String creatorUserToken = getCreatorRoleAuthUserToken(secretKey, newBuyerToken);

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, creatorUserToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("PAYMENT_RUN_NOT_FOUND"))
                .body("message", equalTo("Payment run not found"));
    }

    @Test
    public void CancelPaymentRun_ValidRoleAuthUser_Success() {
//         create authorised user with CREATOR role
        final String creatorUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

//        create payment run with buyer
        final String paymentRunId = createPaymentRun();

//        cancel payment run with user
        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, creatorUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsService.getPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo(State.CANCELLED.name()));
    }

    @Test
    public void CancelPaymentRun_MultipleRolesAuthUser_Success() {
//         create authorised user with CREATOR role
        final String multipleRolesUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

//        create payment run with buyer
        final String paymentRunId = createPaymentRun();

//        cancel payment run with user
        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, multipleRolesUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsService.getPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo(State.CANCELLED.name()));
    }

    @Test
    public void CancelPaymentRun_AdminRoleBuyer_Forbidden() {
        final String paymentRunId = createPaymentRun();

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String adminBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, adminBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CancelPaymentRun_IncorrectRoleBuyer_Forbidden() {
        final String paymentRunId = createPaymentRun();

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String controllerBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignControllerRole(secretKey, controllerBuyerToken);

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, controllerBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CancelPaymentRun_IncorrectRoleAuthUser_Forbidden() {
        final String controllerUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final String paymentRunId = createPaymentRun();

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, controllerUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CancelPaymentRun_VerifiedPaymentRun_CannotCancelPaymentRun() {
//        create buyer with ZBA
        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

//        create and verify Payment run
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final String paymentRunId = PaymentRunsHelper.createConfirmedPaymentRun(createPaymentRunModel, secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());

//        cancel Payment run
        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CANNOT_CANCEL_PAYMENT_RUN"));
    }

    @Test
    public void CancelPaymentRun_ConfirmedPaymentRun_CannotCancelPaymentRun() {
//        create buyer with ZBA
        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

//        create confirmed Payment run
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final String paymentRunId = PaymentRunsHelper.createConfirmedPaymentRun(createPaymentRunModel, secretKey, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKey, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

//        cancel Payment run
        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CANNOT_CANCEL_PAYMENT_RUN"));
    }

    @Test
    public void CancelPaymentRun_AlreadyCancelled_CannotCancelPaymentRun() {
        final String paymentRunId = createPaymentRun();

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsService.getPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo(State.CANCELLED.name()));

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CANNOT_CANCEL_PAYMENT_RUN"));
    }

    @Test
    public void CancelPaymentRun_InvalidPaymentRunId_BadRequest() {
        PaymentRunsService.cancelPaymentRun(RandomStringUtils.randomAlphanumeric(24), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CancelPaymentRun_WrongPaymentRunId_PaymentRunNotFound() {
        PaymentRunsService.cancelPaymentRun("6537dfd67ebeebcc5f5a62c8", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("PAYMENT_RUN_NOT_FOUND"))
                .body("message", equalTo("Payment run not found"));
    }

    @Test
    public void CancelPaymentRun_NoPaymentRunId_NotFound() {
        PaymentRunsService.cancelPaymentRun("", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CancelPaymentRun_NoApiKey_BadRequest() {
        PaymentRunsService.cancelPaymentRunNoApiKey(RandomStringUtils.randomAlphanumeric(24), buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", CoreMatchers.equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", CoreMatchers.equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", CoreMatchers.equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", CoreMatchers.equalTo("REQUIRED"));
    }

    @Test
    public void CancelPaymentRun_InvalidApiKey_Unauthorised() {
        PaymentRunsService.cancelPaymentRun(RandomStringUtils.randomAlphanumeric(24), "abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CancelPaymentRun_NoToken_Unauthorised(final String token) {
        PaymentRunsService.cancelPaymentRun(RandomStringUtils.randomAlphanumeric(24), secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CancelPaymentRun_InvalidToken_Unauthorised() {
        PaymentRunsService.cancelPaymentRun(RandomStringUtils.randomAlphanumeric(24), secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CancelPaymentRun_DifferentInnovatorApiKey_Unauthorised() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("secretKey");

        PaymentRunsService.cancelPaymentRun(RandomStringUtils.randomAlphanumeric(24), secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CancelPaymentRun_LoggedOutBuyer_Unauthorised() {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final String paymentRunId = PaymentRunsHelper.createPaymentRun(CreatePaymentRunModel
                .defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build(), secretKey, buyerToken).getId();

        AuthenticationHelper.logout(secretKey, buyerToken);

        PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private String createPaymentRun() {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        return PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyerToken).getId();
    }

    private static void creatorBuyerSetup() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        buyerToken = authenticatedBuyer.getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);
    }
}
