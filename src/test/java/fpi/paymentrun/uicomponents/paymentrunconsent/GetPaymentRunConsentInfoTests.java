package fpi.paymentrun.uicomponents.paymentrunconsent;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.ChallengesHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.services.uicomponents.PaymentRunConsentService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.EnrolmentChannel;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class GetPaymentRunConsentInfoTests extends BasePaymentRunSetup {
    protected static final String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;
    private static String buyerToken;
    private static CreateBuyerModel createBuyerModel;

    @BeforeAll
    public static void Setup() {
        final Pair<CreateBuyerModel, String> buyer = createBuyer();
        createBuyerModel = buyer.getLeft();
        buyerToken = buyer.getRight();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
    }

    @Test
    public void GetPaymentRunConsentInfo_PendingChallengeStateNoSCA_Success() {
        final CreatePaymentRunResponseModel paymentRun = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(response, paymentRun, createBuyerModel);
    }

    @Test
    public void GetPaymentRunConsentInfo_PendingChallengeStateScaIssued_Success() {
        final CreatePaymentRunResponseModel paymentRun = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        ChallengesHelper.issueScaChallenge(buyerToken, sharedKeyPluginsScaApp, paymentRun.getId());

        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(response, paymentRun, createBuyerModel);
    }

    @Test
    public void GetPaymentRunConsentInfo_ValidRoleBuyerToken_Success() {
        final Pair<CreateBuyerModel, String> buyer = createBuyer();
        final CreateBuyerModel createBuyerModel = buyer.getLeft();
        final String buyerToken = buyer.getRight();

        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        BuyersHelper.assignControllerRole(secretKeyPluginsScaApp, buyerToken);
        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(response, paymentRun, createBuyerModel);
    }

    @Test
    public void GetPaymentRunConsentInfo_MultipleRolesBuyerToken_Success() {
        final Pair<CreateBuyerModel, String> buyer = createBuyer();
        final CreateBuyerModel createBuyerModel = buyer.getLeft();
        final String buyerToken = buyer.getRight();

        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponse(response, paymentRun, createBuyerModel);
    }

    @Test
    public void GetPaymentRunConsentInfo_ValidRoleAuthUserToken_Success() {
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = getControllerRoleAuthUser(secretKeyPluginsScaApp, buyerToken);

        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), authUser.getRight(), sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponseAuthUsers(response, paymentRun, createBuyerModel, authUser.getMiddle());
    }

    @Test
    public void GetPaymentRunConsentInfo_MultipleRolesAuthUserToken_Success() {
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = getMultipleRolesAuthUser(secretKeyPluginsScaApp, buyerToken);

        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), authUser.getRight(), sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponseAuthUsers(response, paymentRun, createBuyerModel, authUser.getMiddle());
    }

    @Test
    public void GetPaymentRunConsentInfo_TwoAuthUsers_Success() {
        final Triple<String, BuyerAuthorisedUserModel, String> multiRolesAuthUser = getMultipleRolesAuthUser(secretKeyPluginsScaApp, buyerToken);
        final Triple<String, BuyerAuthorisedUserModel, String>  controllerAuthUser = getControllerRoleAuthUser(secretKeyPluginsScaApp, buyerToken);

        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(multiRolesAuthUser.getRight());

        final ValidatableResponse response = PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), controllerAuthUser.getRight(), sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_OK);
        assertSuccessfulResponseAuthUsers(response, paymentRun, createBuyerModel, controllerAuthUser.getMiddle());
    }

    @Test
    public void GetPaymentRunConsentInfo_AdminRoleBuyerToken_Forbidden() {
        final String buyerToken = createBuyer().getRight();

        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        BuyersHelper.assignAdminRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunConsentInfo_IncorrectRoleBuyerToken_Forbidden() {
        final String buyerToken = createBuyer().getRight();

        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        BuyersHelper.assignCreatorRole(secretKeyPluginsScaApp, buyerToken);
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunConsentInfo_IncorrectRoleAuthUserToken_Forbidden() {
        final String authUserToken = getCreatorRoleAuthUserToken(secretKeyPluginsScaApp, buyerToken);

        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), authUserToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunConsentInfo_OtherBuyerToken_Forbidden() {
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        final String newBuyerToken = createBuyer().getRight();
        BuyersHelper.assignAdminRole(secretKeyPluginsScaApp, newBuyerToken);
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), newBuyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPaymentRunConsentInfo_OtherBuyerAuthUserToken_PaymentRunNotFound() {
        final CreatePaymentRunResponseModel paymentRun = getConfirmedPaymentRun(buyerToken);

        final String newBuyerToken = createBuyer().getRight();
        final String authUserToken = getControllerRoleAuthUserToken(secretKeyPluginsScaApp, newBuyerToken);
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRun.getId(), authUserToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("PAYMENT_RUN_NOT_FOUND"))
                .body("message", equalTo("Payment run not found"));
    }

    @Test
    public void GetPaymentRunConsentInfo_PendingFundingState_Conflict() {

        final String paymentRunId = createPaymentRun(buyerToken).getId();
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRunId, secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());

        ChallengesHelper.issueAndVerifyScaChallenge(buyerToken, sharedKeyPluginsScaApp, paymentRunId);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_FUNDING.name(), State.PENDING_FUNDING.name());

        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void GetPaymentRunConsentInfo_PendingConfirmationState_Conflict() {

        final String paymentRunId = createPaymentRun(buyerToken).getId();
        PaymentRunsHelper.verifyPaymentRunState(paymentRunId, secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());

        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void GetPaymentRunConsentInfo_InvalidPaymentRun_BadRequest() {

        PaymentRunConsentService.getPaymentRunConsentInfo(RandomStringUtils.randomAlphabetic(12), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", CoreMatchers.equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", CoreMatchers.equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", CoreMatchers.equalTo("payment_run_id"))
                .body("syntaxErrors.invalidFields[0].error", CoreMatchers.equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", CoreMatchers.equalTo(1));
    }

    @Test
    public void GetPaymentRunConsentInfo_WrongPaymentRun_NotFound() {

        PaymentRunConsentService.getPaymentRunConsentInfo("653a1d15a5e4e7c6ae02526e", buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPaymentRunConsentInfo_NoPaymentRun_NotFound() {

        PaymentRunConsentService.getPaymentRunConsentInfo("", buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPaymentRunConsentInfo_InvalidToken_Unauthorised() {
        final String paymentRunId = createPaymentRun(buyerToken).getId();
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, "abc", sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunConsentInfo_NoToken_Unauthorised() {
        final String paymentRunId = createPaymentRun(buyerToken).getId();
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, "", sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunConsentInfo_InvalidSharedKey_Unauthorised() {
        final String paymentRunId = createPaymentRun(buyerToken).getId();
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, buyerToken, RandomStringUtils.randomAlphabetic(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunConsentInfo_AnotherProgrammeSharedKey_Unauthorised() {
        final String paymentRunId = createPaymentRun(buyerToken).getId();
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, buyerToken, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunConsentInfo_DifferentBuyerToken_Unauthorised() {
        final String paymentRunId = createPaymentRun(buyerToken).getId();
        final String newBuyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, newBuyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPaymentRunConsentInfo_BuyerLoggedOut_Unauthorised() {
        final Pair<CreateBuyerModel, String> buyer = createBuyer();
        final String buyerToken = buyer.getRight();
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, buyerToken);

        final String paymentRunId = createPaymentRun(buyerToken).getId();

        AuthenticationHelper.logout(secretKeyPluginsScaApp, buyerToken);

        PaymentRunConsentService.getPaymentRunConsentInfo(paymentRunId, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private static Pair<CreateBuyerModel, String> createBuyer() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKeyPluginsScaApp);
        final String buyerToken = authenticatedBuyer.getRight();

        BuyersHelper.verifyKyb(secretKeyPluginsScaApp, authenticatedBuyer.getLeft());
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyPluginsScaApp, buyerToken);
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKeyPluginsScaApp), secretKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyPluginsScaApp, buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKeyPluginsScaApp, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKeyPluginsScaApp, accountId, buyerToken, "ALLOCATED");
        return Pair.of(createBuyerModel, buyerToken);
    }

    private CreatePaymentRunResponseModel createPaymentRun(final String buyerToken) {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        return PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKeyPluginsScaApp, buyerToken);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final CreatePaymentRunResponseModel paymentRun,
                                          final CreateBuyerModel createBuyerModel) {
        response.body("payments[0].reference", equalTo(paymentRun.getPayments().get(0).getReference()))
                .body("payments[0].paymentAmount.currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("payments[0].paymentAmount.amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("payments[0].payeeDetails.name", equalTo(paymentRun.getPayments().get(0).getSupplier().getName()))
                .body("payments[0].payeeDetails.bankAccountDetails.sortCode", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode()))
                .body("payments[0].payeeDetails.bankAccountDetails.accountNumber", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber()))
                .body("payments[0].payeeDetails.address", equalTo(paymentRun.getPayments().get(0).getSupplier().getAddress()))
                .body("payments[0].payeeDetails.bankName", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankName()))
                .body("payments[0].payeeDetails.bankAddress", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAddress()))
                .body("payments[0].payeeDetails.bankCountry", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankCountry()))
                .body("totalAmount[0].currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("totalAmount[0].amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("sourceAccount.corporateName", equalTo(createBuyerModel.getCompany().getName()))
                .body("sourceAccount.address", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getCity()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getState()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("sourceAccount.country", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("sourceAccount.currency", equalTo("GBP"))
                .body("sourceAccount.bankAccountDetails.sortCode", notNullValue())
                .body("sourceAccount.bankAccountDetails.accountNumber", notNullValue())
                .body("date", notNullValue())
                .body("phoneNumber.number", equalTo(maskDataExceptFirstOneLastThreeChars(createBuyerModel.getAdminUser().getMobile().getNumber())))
                .body("phoneNumber.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()));
    }

    private void assertSuccessfulResponseAuthUsers(final ValidatableResponse response,
                                                   final CreatePaymentRunResponseModel paymentRun,
                                                   final CreateBuyerModel createBuyerModel,
                                                   final BuyerAuthorisedUserModel authorisedUserModel) {
        response.body("payments[0].reference", equalTo(paymentRun.getPayments().get(0).getReference()))
                .body("payments[0].paymentAmount.currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("payments[0].paymentAmount.amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("payments[0].payeeDetails.name", equalTo(paymentRun.getPayments().get(0).getSupplier().getName()))
                .body("payments[0].payeeDetails.bankAccountDetails.sortCode", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode()))
                .body("payments[0].payeeDetails.bankAccountDetails.accountNumber", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber()))
                .body("payments[0].payeeDetails.address", equalTo(paymentRun.getPayments().get(0).getSupplier().getAddress()))
                .body("payments[0].payeeDetails.bankName", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankName()))
                .body("payments[0].payeeDetails.bankAddress", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankAddress()))
                .body("payments[0].payeeDetails.bankCountry", equalTo(paymentRun.getPayments().get(0).getSupplier().getBankCountry()))
                .body("totalAmount[0].currency", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("totalAmount[0].amount", equalTo(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("sourceAccount.corporateName", equalTo(createBuyerModel.getCompany().getName()))
                .body("sourceAccount.address", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getCity()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getState()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("sourceAccount.country", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("sourceAccount.currency", equalTo("GBP"))
                .body("sourceAccount.bankAccountDetails.sortCode", notNullValue())
                .body("sourceAccount.bankAccountDetails.accountNumber", notNullValue())
                .body("date", notNullValue())
                .body("phoneNumber.number", equalTo(maskDataExceptFirstOneLastThreeChars(authorisedUserModel.getMobile().getNumber())))
                .body("phoneNumber.countryCode", equalTo(authorisedUserModel.getMobile().getCountryCode()));
    }

    public CreatePaymentRunResponseModel getConfirmedPaymentRun(final String buyerToken) {
        final CreatePaymentRunResponseModel paymentRun = createPaymentRun(buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken, State.PENDING_CONFIRMATION.name(), State.PENDING_CONFIRMATION.name());
        PaymentRunsHelper.confirmPaymentRun(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken);
        PaymentRunsHelper.verifyPaymentRunState(paymentRun.getId(), secretKeyPluginsScaApp, buyerToken, State.PENDING_CHALLENGE.name(), State.PENDING_CHALLENGE.name());
        return paymentRun;
    }
}
