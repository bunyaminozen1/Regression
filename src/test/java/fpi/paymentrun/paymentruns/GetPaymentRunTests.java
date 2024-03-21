package fpi.paymentrun.paymentruns;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.services.PaymentRunsService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
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

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN)
public class GetPaymentRunTests extends BasePaymentRunSetup {

    protected static final String VERIFICATION_CODE = "123456";

    private static CreatePaymentRunResponseModel paymentRun;
    private static String buyerId;
    private static String buyerToken;

    /**
     * Required user role: Required user roles: CREATOR, CONTROLLER
     */

    @BeforeAll
    public static void Setup() {
        final Triple<String, CreatePaymentRunResponseModel, String> buyer = creatorBuyerWithPaymentRun();
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
        paymentRun = buyer.getMiddle();
    }

    @Test
    public void GetPaymentRun_CreatorRoleBuyerToken_Success() {
        final ValidatableResponse response = PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, buyerToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(paymentRun, response, buyerId);
    }

    @Test
    public void GetPaymentRun_CreatorRoleAuthUserToken_Success() {
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, authUserToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(paymentRun, response, buyerId);
    }

    @Test
    public void GetPaymentRun_MultipleRolesAuthUserToken_Success() {
        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, authUserToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(paymentRun, response, buyerId);
    }

    @Test
    public void GetPaymentRun_MultipleRolesBuyerToken_Success() {
        final Triple<String, CreatePaymentRunResponseModel, String> buyer = creatorBuyerWithPaymentRun();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final ValidatableResponse response = PaymentRunsService.getPaymentRun(buyer.getMiddle().getId(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(buyer.getMiddle(), response, buyer.getLeft());
    }

    @Test
    public void GetPaymentRun_AdminRoleBuyerToken_Forbidden() {
        final Triple<String, CreatePaymentRunResponseModel, String> buyer = creatorBuyerWithPaymentRun();
        BuyersHelper.assignAdminRole(secretKey, buyer.getRight());

        PaymentRunsService.getPaymentRun(buyer.getMiddle().getId(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRun_ControllerRoleBuyerToken_Success() {
        final Triple<String, CreatePaymentRunResponseModel, String> buyer = creatorBuyerWithPaymentRun();
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final ValidatableResponse response = PaymentRunsService.getPaymentRun(buyer.getMiddle().getId(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(buyer.getMiddle(), response, buyer.getLeft());
    }

    @Test
    public void GetPaymentRun_ControllerRoleAuthUserToken_Success() {
        final Triple<String, CreatePaymentRunResponseModel, String> buyer = creatorBuyerWithPaymentRun();
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyer.getRight());

        final ValidatableResponse response = PaymentRunsService.getPaymentRun(buyer.getMiddle().getId(), secretKey, authUserToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(buyer.getMiddle(), response, buyer.getLeft());
    }

    @Test
    public void GetPaymentRun_UnknownPaymentRunId_BadRequest() {
        PaymentRunsService.getPaymentRun(RandomStringUtils.randomNumeric(18), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void GetPaymentRun_InvalidPaymentRunId_BadRequest() {
        PaymentRunsService.getPaymentRun(RandomStringUtils.randomNumeric(3), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void GetPaymentRun_WrongPaymentRunId_NotFound() {
        PaymentRunsService.getPaymentRun("653a1d15a5e4e7c6ae02526e", secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPaymentRun_InvalidApiKey_Unauthorised() {
        PaymentRunsService.getPaymentRun(paymentRun.getId(), "abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRun_NoApiKey_Unauthorised() {
        PaymentRunsService.getPaymentRun(paymentRun.getId(), "", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRun_InvalidToken_Unauthorised() {
        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRun_NoToken_Unauthorised() {
        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRun_DifferentInnovatorApiKey_Unauthorised() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRun_UserLoggedOut_Unauthorised() {
        final String token = BuyersHelper.createUnauthenticatedBuyer(secretKey).getRight();

        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRun_OtherBuyerToken_NotFound() {
        final String newBuyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, newBuyerToken);

        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, newBuyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPaymentRun_OtherBuyerAuthUserToken_NotFound() {
        final String newBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, newBuyerToken);

        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey, authUserToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPaymentRun_BackofficeImpersonator_Unauthorised() {
        PaymentRunsService.getPaymentRun(paymentRun.getId(), secretKey,
                        getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    public static Triple<String, CreatePaymentRunResponseModel, String> creatorBuyerWithPaymentRun() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        final String buyerToken = buyer.getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final CreatePaymentRunResponseModel paymentRun = PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(CreatePaymentRunResponseModel.class);
        return Triple.of(buyer.getLeft(), paymentRun, buyerToken);
    }

    private void assertSuccessfulResponse(final CreatePaymentRunResponseModel paymentRun,
                                          final ValidatableResponse response,
                                          final String buyerId) {
        response.body("id", equalTo(paymentRun.getId()))
                .body("status", equalTo(State.PENDING_CONFIRMATION.name()))
                .body("payments[0].id", equalTo(paymentRun.getPayments().get(0).getId()))
                .body("payments[0].status", equalTo(State.PENDING_CONFIRMATION.name()))
                .body("payments[0].externalRef", equalTo(paymentRun.getPayments().get(0).getExternalRef()))
                .body("payments[0].paymentRef", equalTo(paymentRun.getPayments().get(0).getPaymentRef()))
                .body("payments[0].paymentAmount.currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("payments[0].paymentAmount.amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("payments[0].reference", equalTo(paymentRun.getPayments().get(0).getReference()))
                .body("payments[0].supplier.name", equalTo(paymentRun.getPayments().get(0).getSupplier().getName()))
                .body("payments[0].supplier.address", equalTo(paymentRun.getPayments().get(0).getSupplier().getAddress()))
                .body("payments[0].supplier.bankName", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankName()))
                .body("payments[0].supplier.bankAddress", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAddress()))
                .body("payments[0].supplier.bankCountry", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankCountry()))
                .body("payments[0].supplier.bankAccountDetails.accountNumber", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber()))
                .body("payments[0].supplier.bankAccountDetails.sortCode", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode()))
                .body("createdBy", equalTo(buyerId))
                .body("createdAt", notNullValue())
                .body("paymentRunRef", equalTo(paymentRun.getPaymentRunRef()))
                .body("tag", equalTo(paymentRun.getTag()))
                .body("description", equalTo(paymentRun.getDescription()));
    }
}
