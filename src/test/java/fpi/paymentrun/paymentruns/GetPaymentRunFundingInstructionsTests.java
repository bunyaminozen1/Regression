package fpi.paymentrun.paymentruns;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.PaymentRunsService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.EnrolmentChannel;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.models.shared.VerificationModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN)
public class GetPaymentRunFundingInstructionsTests extends BasePaymentRunSetup {
//    TODO add tests with SEPA details when it's implemented

    protected static final String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

    private static String buyerId;
    private static String buyerToken;

    /**
     * Required user role: CONTROLLER
     */

    @BeforeAll
    public static void BuyerSetup() {
        multipleRolesBuyerSetup();
    }

    @Test
    public void GetPaymentRunFundingInstructions_MultipleRolesBuyerToken_Success() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        final ValidatableResponse response = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyerToken)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(paymentRun, response);
    }

    @Test
    public void GetPaymentRunFundingInstructions_ValidRoleBuyerToken_Success() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledVerifiedBuyer(CreateBuyerModel.defaultCreateBuyerModel().build(), secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final CreatePaymentRunResponseModel paymentRun = PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyer.getRight(), sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyer.getLeft(), institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final ValidatableResponse response = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(paymentRun, response);
    }

    @Test
    public void GetPaymentRunFundingInstructions_ValidRoleAuthUserToken_Success() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, authUserToken)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(paymentRun, response);
    }

    @Test
    public void GetPaymentRunFundingInstructions_MultipleRolesAuthUserToken_Success() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        final String authUserToken = getMultipleRolesAuthUserToken(secretKey, buyerToken);

        final ValidatableResponse response = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, authUserToken)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(paymentRun, response);
    }

    @Test
    public void GetPaymentRunFundingInstructions_IncorrectRoleAuthUserToken_Forbidden() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, buyerToken);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, authUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunFundingInstructions_AdminRoleBuyerToken_Forbidden() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledVerifiedBuyer(CreateBuyerModel.defaultCreateBuyerModel().build(), secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final CreatePaymentRunResponseModel paymentRun = PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyer.getRight(), sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyer.getLeft(), institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        BuyersHelper.assignAdminRole(secretKey, buyer.getRight());

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunFundingInstructions_IncorrectRoleBuyerToken_Forbidden() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledVerifiedBuyer(CreateBuyerModel.defaultCreateBuyerModel().build(), secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final CreatePaymentRunResponseModel paymentRun = PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyer.getRight(), sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyer.getLeft(), institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        BuyersHelper.assignCreatorRole(secretKey, buyer.getRight());

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunFundingInstructions_TwoLinkedAccountsPerCurrency_Conflict() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountIdFirst = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();
        final String linkedManagedAccountIdSecond = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountIdFirst);
        accIds.add(linkedManagedAccountIdSecond);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TOO_MANY_LINKED_ACCOUNT_PER_CURRENCY"));
    }

    @Test
    public void GetPaymentRunFundingInstructions_InvalidPaymentRunId_BadRequest() {
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(RandomStringUtils.randomAlphanumeric(18), Optional.of(filters), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void GetPaymentRunFundingInstructions_WrongPaymentRunId_NotFound() {
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions("652d52f01c2cefca1e74d384", Optional.of(filters), secretKey, buyerToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPaymentRunFundingInstructions_InvalidLinkedManagedAccountId_BadRequest() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();

        List<String> accIds = new ArrayList<>();
        accIds.add(RandomStringUtils.randomAlphanumeric(18));
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("linkedAccountIds", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linkedAccountIds"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void GetPaymentRunFundingInstructions_WrongLinkedManagedAccountId_LinkedAccountNotFound() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();

        List<String> accIds = new ArrayList<>();
        accIds.add("652d535c1c2cefca1e74d397");
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("LINKED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void GetPaymentRunFundingInstructions_WithoutFilter_BadRequest() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.empty(), secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linkedAccountIds"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void GetPaymentRunFundingInstructions_NoApiKey_Unauthorised() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), "", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunFundingInstructions_InvalidApiKey_Unauthorised() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), "abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunFundingInstructions_NoToken_Unauthorised() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunFundingInstructions_InvalidToken_Unauthorised() {
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunFundingInstructions_OtherBuyerTokenValidRole_NotFound() {
        final String secondBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(CreateBuyerModel.defaultCreateBuyerModel().build(), secretKey).getRight();
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        BuyersHelper.assignControllerRole(secretKey, secondBuyerToken);

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, secondBuyerToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", CoreMatchers.equalTo("PAYMENT_RUN_NOT_FOUND"))
                .body("message", CoreMatchers.equalTo("Payment run not found"));
    }

    @Test
    public void GetPaymentRunFundingInstructions_OtherBuyerTokenAdminRole_Forbidden() {
        final String secondBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(CreateBuyerModel.defaultCreateBuyerModel().build(), secretKey).getRight();
        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, secondBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunFundingInstructions_OtherBuyerAuthUserTokenValidRole_NotFound() {
        final String secondBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, secondBuyerToken);

        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, authUserToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", CoreMatchers.equalTo("PAYMENT_RUN_NOT_FOUND"))
                .body("message", CoreMatchers.equalTo("Payment run not found"));
    }

    @Test
    public void GetPaymentRunFundingInstructions_OtherBuyerAuthUserTokenInvalidRole_Forbidden() {
        final String secondBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String authUserToken = getCreatorRoleAuthUserToken(secretKey, secondBuyerToken);

        final CreatePaymentRunResponseModel paymentRun = getFasterPaymentsPaymentRun();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyerToken, sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey).getLeft();

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters), secretKey, authUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunFundingInstructions_BuyerLoggedOut_Unauthorised() {
        final CreateBuyerModel buyerModel = CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledVerifiedBuyer(buyerModel, secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        AuthenticationHelper.login(buyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKey), secretKey);

        verifySuccessfulStepup(buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final String paymentRunId = PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight()).getId();
        final String institutionId = AisUxComponentHelper.getInstitutionId(buyer.getRight(), sharedKey);
        final String linkedManagedAccountId = SimulatorHelper.createLinkedAccount(buyer.getLeft(), institutionId, secretKey).getLeft();

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        List<String> accIds = new ArrayList<>();
        accIds.add(linkedManagedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        PaymentRunsService.getPaymentRunFundingInstructions(paymentRunId, Optional.of(filters), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static void verifySuccessfulStepup(final String token) {
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static void multipleRolesBuyerSetup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        buyerId = authenticatedBuyer.getLeft();
        buyerToken = authenticatedBuyer.getRight();
        final String buyerEmail = createBuyerModel.getAdminUser().getEmail();
        final String buyerPassword = TestHelper.getDefaultPassword(secretKey);
        AuthenticationHelper.login(buyerEmail, buyerPassword, secretKey);
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyerToken);
        BuyersHelper.assignAllRoles(secretKey, buyerToken);
    }

    private CreatePaymentRunResponseModel getFasterPaymentsPaymentRun() {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        return PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyerToken);
    }

    private void assertSuccessfulResponse(final CreatePaymentRunResponseModel paymentRun,
                                          final ValidatableResponse response) {
        response.body("fundingInstructions[0].reference", notNullValue())
                .body("fundingInstructions[0].totalAmount.currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("fundingInstructions[0].totalAmount.amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("fundingInstructions[0].status", equalTo(State.PENDING_FUNDING.name()))
                .body("fundingInstructions[0].payments[0].id", equalTo(paymentRun.getPayments().get(0).getId()))
                .body("fundingInstructions[0].payments[0].status", equalTo(State.PENDING_CONFIRMATION.name()))
                .body("fundingInstructions[0].payments[0].reference", equalTo(paymentRun.getPayments().get(0).getReference()))
                .body("fundingInstructions[0].payments[0].externalRef", equalTo(paymentRun.getPayments().get(0).getExternalRef()))
                .body("fundingInstructions[0].payments[0].paymentRef", equalTo(paymentRun.getPayments().get(0).getPaymentRef()))
                .body("fundingInstructions[0].payments[0].paymentAmount.currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("fundingInstructions[0].payments[0].paymentAmount.amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("fundingInstructions[0].payments[0].supplier.name", equalTo(paymentRun.getPayments().get(0).getSupplier().getName()))
                .body("fundingInstructions[0].payments[0].supplier.address", equalTo(paymentRun.getPayments().get(0).getSupplier().getAddress()))
                .body("fundingInstructions[0].payments[0].supplier.bankName", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankName()))
                .body("fundingInstructions[0].payments[0].supplier.bankAddress", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAddress()))
                .body("fundingInstructions[0].payments[0].supplier.bankCountry", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankCountry()))
                .body("fundingInstructions[0].payments[0].supplier.bankAccountDetails.sortCode", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode()))
                .body("fundingInstructions[0].payments[0].supplier.bankAccountDetails.accountNumber", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }
}
