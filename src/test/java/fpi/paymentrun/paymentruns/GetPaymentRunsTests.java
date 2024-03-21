package fpi.paymentrun.paymentruns;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.PaymentRunsService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.services.innovator.InnovatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN)
public class GetPaymentRunsTests extends BasePaymentRunSetup {

    protected static final String VERIFICATION_CODE = "123456";

    private static List<CreatePaymentRunResponseModel> paymentRuns = new ArrayList<>();
    private static String buyerId;
    private static String buyerToken;

    /**
     * Required user roles: CREATOR, CONTROLLER
     */

    @BeforeAll
    public static void Setup() {
        final Triple<String, List<CreatePaymentRunResponseModel>, String> buyer = creatorBuyerWithPaymentRuns();
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
        paymentRuns = buyer.getMiddle();
    }

    @Test
    public void GetPaymentRuns_CreatorRoleBuyerToken_Success() {
        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(paymentRuns.size()))
                .body("responseCount", equalTo(paymentRuns.size()));

        assertSuccessfulResponse(response, paymentRuns, buyerId);
    }

    @Test
    public void GetPaymentRuns_CreatorRoleAuthUserToken_Success() {
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), authUserToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(paymentRuns.size()))
                .body("responseCount", equalTo(paymentRuns.size()));

        assertSuccessfulResponse(response, paymentRuns, buyerId);
    }

    @Test
    public void GetPaymentRuns_MultipleRolesAuthUserToken_Success() {
        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), authUserToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(paymentRuns.size()))
                .body("responseCount", equalTo(paymentRuns.size()));

        assertSuccessfulResponse(response, paymentRuns, buyerId);
    }

    @Test
    public void GetPaymentRuns_MultipleRolesBuyerToken_Success() {
        final Triple<String, List<CreatePaymentRunResponseModel>, String> buyer = creatorBuyerWithPaymentRuns();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(paymentRuns.size()))
                .body("responseCount", equalTo(paymentRuns.size()));

        assertSuccessfulResponse(response, buyer.getMiddle(), buyer.getLeft());
    }

    @Test
    public void GetPaymentRuns_AdminRoleBuyerToken_Forbidden() {
        final String buyerToken = creatorBuyerWithPaymentRuns().getRight();
        BuyersHelper.assignAdminRole(secretKey, buyerToken);

        PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRuns_ControllerRoleBuyerToken_Success() {
        final Triple<String, List<CreatePaymentRunResponseModel>, String> buyer = creatorBuyerWithPaymentRuns();
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(buyer.getMiddle().size()))
                .body("responseCount", equalTo(buyer.getMiddle().size()));

        assertSuccessfulResponse(response, buyer.getMiddle(), buyer.getLeft());
    }

    @Test
    public void GetPaymentRuns_ControllerRoleAuthUserToken_Success() {
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), authUserToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(paymentRuns.size()))
                .body("responseCount", equalTo(paymentRuns.size()));

        assertSuccessfulResponse(response, paymentRuns, buyerId);
    }

    @Test
    public void GetPaymentRuns_WithAllFilters_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("tag[]", paymentRuns.get(0).getTag());

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.of(filters), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        assertSuccessfulResponse(response, Collections.singletonList(paymentRuns.get(0)), buyerId);
    }

    @Test
    public void GetPaymentRuns_OtherBuyerToken_SuccessNoPaymentRuns() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("tag[]", paymentRuns.get(0).getTag());

        final String newBuyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        BuyersHelper.assignCreatorRole(secretKey, newBuyerToken);

//        should receive Empty Response
        PaymentRunsService.getPaymentRuns(secretKey, Optional.of(filters), newBuyerToken)
                .then()
                .statusCode(SC_OK)
                .body("paymentRuns.size()", equalTo(0))
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetPaymentRuns_OtherBuyerAuthUserToken_SuccessNoPaymentRuns() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("tag[]", paymentRuns.get(0).getTag());

        final String newBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, newBuyerToken);

//        should receive Empty Response
        PaymentRunsService.getPaymentRuns(secretKey, Optional.of(filters), authUserToken)
                .then()
                .statusCode(SC_OK)
                .body("paymentRuns.size()", equalTo(0))
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetPaymentRuns_LimitFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);

        final ValidatableResponse response = PaymentRunsService.getPaymentRuns(secretKey, Optional.of(filters), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(paymentRuns.size()))
                .body("responseCount", equalTo(1));

        assertSuccessfulResponse(response, Collections.singletonList(paymentRuns.get(0)), buyerId);
    }

    @Test
    public void GetPaymentRuns_NoEntries_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("tag[]", "abc");

        PaymentRunsService.getPaymentRuns(secretKey, Optional.of(filters), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetPaymentRuns_InvalidApiKey_Unauthorised() {
        PaymentRunsService.getPaymentRuns("abc", Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRuns_NoApiKey_Unauthorised() {
        PaymentRunsService.getPaymentRuns("", Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRuns_DifferentInnovatorApiKey_Unauthorised() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRuns_RootUserLoggedOut_Unauthorised() {
        final String token = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.logout(secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);

        PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(), token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRuns_BackofficeImpersonation_Unauthorised() {

        PaymentRunsService.getPaymentRuns(secretKey, Optional.empty(),
                        getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final List<CreatePaymentRunResponseModel> paymentRuns,
                                          final String buyerId) {
        IntStream.range(0, paymentRuns.size()).forEach(i ->
                response.body(String.format("paymentRuns[%s].id", i), equalTo(paymentRuns.get(i).getId()))
                        .body(String.format("paymentRuns[%s].status", i), equalTo(State.PENDING_CONFIRMATION.name()))
                        .body(String.format("paymentRuns[%s].payments[0].id", i), equalTo(paymentRuns.get(i).getPayments().get(0).getId()))
                        .body(String.format("paymentRuns[%s].payments[0].status", i), equalTo(State.PENDING_CONFIRMATION.name()))
                        .body(String.format("paymentRuns[%s].payments[0].externalRef", i), equalTo(paymentRuns.get(i).getPayments().get(0).getExternalRef()))
                        .body(String.format("paymentRuns[%s].payments[0].paymentRef", i), equalTo(paymentRuns.get(i).getPayments().get(0).getPaymentRef()))
                        .body(String.format("paymentRuns[%s].payments[0].paymentAmount.currency", i), equalTo(paymentRuns.get(i).getPayments().get(0).getPaymentAmount().getCurrency()))
                        .body(String.format("paymentRuns[%s].payments[0].paymentAmount.amount", i), equalTo(paymentRuns.get(i).getPayments().get(0).getPaymentAmount().getAmount()))
                        .body(String.format("paymentRuns[%s].payments[0].reference", i), equalTo(paymentRuns.get(i).getPayments().get(0).getReference()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.name", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getName()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.address", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getAddress()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.bankName", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getBankName()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.bankAddress", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getBankAddress()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.bankCountry", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getBankCountry()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.bankAccountDetails.accountNumber", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber()))
                        .body(String.format("paymentRuns[%s].payments[0].supplier.bankAccountDetails.sortCode", i), equalTo(paymentRuns.get(i).getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode()))
                        .body(String.format("paymentRuns[%s].createdBy", i), equalTo(buyerId))
                        .body(String.format("paymentRuns[%s].createdAt", i), equalTo(paymentRuns.get(i).getCreatedAt()))
                        .body(String.format("paymentRuns[%s].paymentRunRef", i), equalTo(paymentRuns.get(i).getPaymentRunRef()))
                        .body(String.format("paymentRuns[%s].tag", i), equalTo(paymentRuns.get(i).getTag()))
                        .body(String.format("paymentRuns[%s].description", i), equalTo(paymentRuns.get(i).getDescription()))
        );
    }

    public static Triple<String, List<CreatePaymentRunResponseModel>, String> creatorBuyerWithPaymentRuns() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        final String buyerToken = buyer.getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);

        final List<CreatePaymentRunResponseModel> paymentRuns = new ArrayList<>();
        IntStream.range(0, 3).forEach(i -> {
            final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
            final CreatePaymentRunModel createPaymentRunModel =
                    CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                            accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

            final CreatePaymentRunResponseModel response = PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                    .then()
                    .statusCode(SC_CREATED)
                    .extract()
                    .as(CreatePaymentRunResponseModel.class);

            paymentRuns.add(response);
        });
        return Triple.of(buyer.getLeft(), paymentRuns, buyerToken);
    }
}
