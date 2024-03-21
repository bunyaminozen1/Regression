package fpi.paymentrun.uicomponents.pis;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.paymentrun.models.AisUxInstitutionResponseModel;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.models.simulator.SimulateLinkedAccountModel;
import fpi.paymentrun.services.PaymentRunsService;
import fpi.paymentrun.services.uicomponents.PisUxComponentService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class GetPisConsentInfoTests extends BasePaymentRunSetup {

    protected static final String VERIFICATION_CODE = "123456";

    private String buyerId;
    private String buyerToken;
    private CreateBuyerModel createBuyerModel;

    @BeforeEach
    public void SourceSetup() {
        final Triple<String, CreateBuyerModel, String> buyer = createBuyer();
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
        createBuyerModel = buyer.getMiddle();
        BuyersHelper.assignAllRoles(secretKey, buyerToken);
    }

    @Test
    public void GetPisConsentInfo_MultipleRolesBuyerToken_Success() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        ValidatableResponse response = PisUxComponentService.getConsentInfo(linkedAccount.getRight(), buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED);
        assertSuccessfulResponse(response, paymentRun.getLeft(), createBuyerModel, accountId, linkedAccount, institution);
    }

    @Test
    public void GetPisConsentInfo_ValidRoleBuyerToken_Success() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        ValidatableResponse response = PisUxComponentService.getConsentInfo(linkedAccount.getRight(), buyerToken, sharedKey)
                .then()
                .statusCode(SC_CREATED);
        assertSuccessfulResponse(response, paymentRun.getLeft(), createBuyerModel, accountId, linkedAccount, institution);
    }

    @Test
    public void GetPisConsentInfo_ValidRoleAuthUserToken_Success() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKey, buyerToken);
        ValidatableResponse response = PisUxComponentService.getConsentInfo(linkedAccount.getRight(), authUser.getRight(), sharedKey)
                .then()
                .statusCode(SC_CREATED);
        assertSuccessfulResponseAuthUser(response, paymentRun.getLeft(), createBuyerModel, authUser.getMiddle(), accountId, linkedAccount, institution);
    }

    @Test
    public void GetPisConsentInfo_MultipleRolesAuthUserToken_Success() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignAllRoles(authUser.getLeft(), secretKey, buyerToken);
        ValidatableResponse response = PisUxComponentService.getConsentInfo(linkedAccount.getRight(), authUser.getRight(), sharedKey)
                .then()
                .statusCode(SC_CREATED);
        assertSuccessfulResponseAuthUser(response, paymentRun.getLeft(), createBuyerModel, authUser.getMiddle(), accountId, linkedAccount, institution);
    }

    @Test
    public void GetPisConsentInfo_AdminRoleBuyerToken_Forbidden() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        BuyersHelper.assignAdminRole(secretKey, buyerToken);
        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPisConsentInfo_IncorrectRoleBuyerToken_Forbidden() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        BuyersHelper.assignCreatorRole(secretKey, buyerToken);
        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), buyerToken, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPisConsentInfo_IncorrectRoleAuthUserToken_Forbidden() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignCreatorRole(authUser.getLeft(), secretKey, buyerToken);
        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), authUser.getRight(), sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPisConsentInfo_NotEnrolledAuthUserToken_SweepingConsentChannelNotRegistered() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);
        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), authUserToken, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SWEEPING_CONSENT_CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void GetPisConsentInfo_OtherBuyerToken_NotFound() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final String buyerToken = createBuyer().getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);
        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), buyerToken, sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("PAYMENT_RUN_GROUP_NOT_FOUND"))
                .body("message", equalTo("Payment run group not found"));
    }

    @Test
    public void GetPisConsentInfo_OtherBuyerAuthUserToken_NotFound() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final String buyerToken = createBuyer().getRight();
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(authUser.getLeft(), secretKey, buyerToken);
        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), authUser.getRight(), sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("code", equalTo("PAYMENT_RUN_GROUP_NOT_FOUND"))
                .body("message", equalTo("Payment run group not found"));
    }

    @Test
    public void GetPisConsentInfo_InvalidReference_NotFound() {
        PisUxComponentService.getConsentInfo(RandomStringUtils.randomAlphabetic(12), buyerToken, sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPisConsentInfo_NoReference_NotFound() {
        PisUxComponentService.getConsentInfo("", buyerToken, sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPisConsentInfo_InvalidToken_Unauthorised() {
        PisUxComponentService.getConsentInfo(RandomStringUtils.randomAlphabetic(12), "abc", sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPisConsentInfo_NoToken_Unauthorised() {
        PisUxComponentService.getConsentInfo(RandomStringUtils.randomAlphabetic(12), "", sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPisConsentInfo_InvalidSharedKey_Unauthorised() {
        PisUxComponentService.getConsentInfo(RandomStringUtils.randomAlphabetic(12), buyerToken, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPisConsentInfo_DifferentProgrammeSharedKey_Unauthorised() {
        PisUxComponentService.getConsentInfo(RandomStringUtils.randomAlphabetic(12), buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPisConsentInfo_BuyerLoggedOut_Unauthorised() {
        final Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> paymentRun = createPaymentRun(buyerToken);
        final AisUxInstitutionResponseModel institution = AisUxComponentHelper.getInstitution(buyerToken, sharedKey);
        final Triple<String, SimulateLinkedAccountModel, String> linkedAccount =
                getLinkedAccountWithFundingInstructionsReference(buyerId, institution.getInstitutions().get(3).getId(), paymentRun.getRight().getId(), buyerToken);

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        AuthenticationHelper.logout(secretKey, buyerToken);

        PisUxComponentService.getConsentInfo(linkedAccount.getRight(), buyerToken, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private Triple<String, CreateBuyerModel, String> createBuyer() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        final String buyerId = authenticatedBuyer.getLeft();
        final String buyerToken = authenticatedBuyer.getRight();

        BuyersHelper.verifyKyb(secretKey, buyerId);
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyerToken);
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKey), secretKey);
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyerToken);
        return Triple.of(buyerId, createBuyerModel, buyerToken);
    }

    private Triple<String, SimulateLinkedAccountModel, String> getLinkedAccountWithFundingInstructionsReference(final String buyerId,
                                                                                                                final String institutionId,
                                                                                                                final String paymentRunId,
                                                                                                                final String buyerToken) {
        final Pair<String, SimulateLinkedAccountModel> linkedAccount = SimulatorHelper.createLinkedAccount(buyerId, institutionId, secretKey);

        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccount.getLeft());
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        final String reference = PaymentRunsService.getPaymentRunFundingInstructions(paymentRunId, Optional.of(filters), secretKey, buyerToken)
                .jsonPath().get("fundingInstructions[0].reference");

        return Triple.of(linkedAccount.getLeft(), linkedAccount.getRight(), reference);
    }

    private Pair<CreatePaymentRunModel, CreatePaymentRunResponseModel> createPaymentRun(final String buyerToken) {
        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final CreatePaymentRunResponseModel paymentRun = PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyerToken);
        return Pair.of(createPaymentRunModel, paymentRun);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final CreatePaymentRunModel createPaymentRunModel,
                                          final CreateBuyerModel createBuyerModel,
                                          final String accountId,
                                          final Triple<String, SimulateLinkedAccountModel, String> linkedAccount,
                                          final AisUxInstitutionResponseModel institution) {
        response.body("embedder.companyRegistrationName", equalTo(innovatorName))
                .body("sweepingConsent.status", equalTo("AWAITING_AUTHORIZATION"))
                .body("sweepingConsent.corporateName", equalTo(createBuyerModel.getCompany().getName()))
                .body("sweepingConsent.address", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getCity()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getState()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("sweepingConsent.country", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("sweepingConsent.managedAccount.id", equalTo(accountId))
                .body("sweepingConsent.managedAccount.bankAccountDetails.sortCode", notNullValue())
                .body("sweepingConsent.managedAccount.bankAccountDetails.accountNumber", notNullValue())
                .body("sweepingConsent.linkedAccount.id", equalTo(linkedAccount.getLeft()))
                .body("sweepingConsent.linkedAccount.bankAccountDetails.sortCode", equalTo(linkedAccount.getMiddle().getAccountIdentification().getSortCode()))
                .body("sweepingConsent.linkedAccount.bankAccountDetails.accountNumber", equalTo(linkedAccount.getMiddle().getAccountIdentification().getAccountNumber()))
                .body("sweepingConsent.paymentType", equalTo(OwtType.FASTER_PAYMENTS.name()))
                .body("sweepingConsent.mobile.countryCode", equalTo(createBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("sweepingConsent.mobile.number", equalTo(maskDataExceptFirstOneLastThreeChars(createBuyerModel.getAdminUser().getMobile().getNumber())))
                .body("payerDetails.institution.id", equalTo(institution.getInstitutions().get(3).getId()))
                .body("payerDetails.institution.displayName", equalTo(institution.getInstitutions().get(3).getDisplayName()))
                .body("payerDetails.institution.countries", equalTo(institution.getInstitutions().get(3).getCountries()))
                .body("payerDetails.institution.images.logo", equalTo(institution.getInstitutions().get(3).getImages().getLogo()))
                .body("payerDetails.institution.images.icon", equalTo(institution.getInstitutions().get(3).getImages().getIcon()))
                .body("payerDetails.institution.info.loginUrl", equalTo(institution.getInstitutions().get(3).getInfo().getLoginUrl()))
                .body("payerDetails.institution.info.helplinePhoneNumber", equalTo(institution.getInstitutions().get(3).getInfo().getHelplinePhoneNumber()))
                .body("payeeDetails.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("payeeDetails.bankAccountDetails.sortCode", notNullValue())
                .body("payeeDetails.bankAccountDetails.accountNumber", notNullValue())
                .body("paymentReference", equalTo(linkedAccount.getRight()))
                .body("paymentAmount.currency", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("paymentAmount.amount", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getAmount()));
    }

    private void assertSuccessfulResponseAuthUser(final ValidatableResponse response,
                                          final CreatePaymentRunModel createPaymentRunModel,
                                          final CreateBuyerModel createBuyerModel,
                                          final BuyerAuthorisedUserModel authorisedUserModel,
                                          final String accountId,
                                          final Triple<String, SimulateLinkedAccountModel, String> linkedAccount,
                                          final AisUxInstitutionResponseModel institution) {
        response.body("embedder.companyRegistrationName", equalTo(innovatorName))
                .body("sweepingConsent.status", equalTo("AWAITING_AUTHORIZATION"))
                .body("sweepingConsent.corporateName", equalTo(createBuyerModel.getCompany().getName()))
                .body("sweepingConsent.address", equalTo(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getAddressLine2()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getCity()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getState()
                        + ", " + createBuyerModel.getCompany().getBusinessAddress().getPostCode()))
                .body("sweepingConsent.country", equalTo(createBuyerModel.getCompany().getBusinessAddress().getCountry()))
                .body("sweepingConsent.managedAccount.id", equalTo(accountId))
                .body("sweepingConsent.managedAccount.bankAccountDetails.sortCode", notNullValue())
                .body("sweepingConsent.managedAccount.bankAccountDetails.accountNumber", notNullValue())
                .body("sweepingConsent.linkedAccount.id", equalTo(linkedAccount.getLeft()))
                .body("sweepingConsent.linkedAccount.bankAccountDetails.sortCode", equalTo(linkedAccount.getMiddle().getAccountIdentification().getSortCode()))
                .body("sweepingConsent.linkedAccount.bankAccountDetails.accountNumber", equalTo(linkedAccount.getMiddle().getAccountIdentification().getAccountNumber()))
                .body("sweepingConsent.paymentType", equalTo(OwtType.FASTER_PAYMENTS.name()))
                .body("sweepingConsent.mobile.countryCode", equalTo(authorisedUserModel.getMobile().getCountryCode()))
                .body("sweepingConsent.mobile.number", equalTo(maskDataExceptFirstOneLastThreeChars(authorisedUserModel.getMobile().getNumber())))
                .body("payerDetails.institution.id", equalTo(institution.getInstitutions().get(3).getId()))
                .body("payerDetails.institution.displayName", equalTo(institution.getInstitutions().get(3).getDisplayName()))
                .body("payerDetails.institution.countries", equalTo(institution.getInstitutions().get(3).getCountries()))
                .body("payerDetails.institution.images.logo", equalTo(institution.getInstitutions().get(3).getImages().getLogo()))
                .body("payerDetails.institution.images.icon", equalTo(institution.getInstitutions().get(3).getImages().getIcon()))
                .body("payerDetails.institution.info.loginUrl", equalTo(institution.getInstitutions().get(3).getInfo().getLoginUrl()))
                .body("payerDetails.institution.info.helplinePhoneNumber", equalTo(institution.getInstitutions().get(3).getInfo().getHelplinePhoneNumber()))
                .body("payeeDetails.name", equalTo(createBuyerModel.getCompany().getName()))
                .body("payeeDetails.bankAccountDetails.sortCode", notNullValue())
                .body("payeeDetails.bankAccountDetails.accountNumber", notNullValue())
                .body("paymentReference", equalTo(linkedAccount.getRight()))
                .body("paymentAmount.currency", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("paymentAmount.amount", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getAmount()));
    }
}