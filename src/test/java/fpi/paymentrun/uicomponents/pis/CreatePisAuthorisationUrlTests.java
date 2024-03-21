package fpi.paymentrun.uicomponents.pis;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.helpers.SweepingConsentHelper;
import fpi.helpers.simulator.MockBankHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.models.CreatePisAuthorisationUrlRequestModel;
import fpi.paymentrun.services.uicomponents.PisUxComponentService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class CreatePisAuthorisationUrlTests extends BasePaymentRunSetup {
    protected static final String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

    /**
     * Required user role: CONTROLLER
     */

    @Test
    public void CreatePisAuthorisationUrl_MultipleRolesBuyerToken_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreatePisAuthorisationUrl_ValidRoleBuyerToken_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        BuyersHelper.assignControllerRole(secretKeyPluginsScaApp, buyerToken);
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreatePisAuthorisationUrl_MultipleRolesAuthUserToken_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authUserToken = getMultipleRolesAuthUserToken(secretKeyPluginsScaApp, buyerToken);

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, authUserToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreatePisAuthorisationUrl_ValidRoleAuthUserToken_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authUserToken = getControllerRoleAuthUserToken(secretKeyPluginsScaApp, buyerToken);

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, authUserToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CREATED)
                .body("authorisationUrl", notNullValue());
    }

    @Test
    public void CreatePisAuthorisationUrl_AdminRoleBuyerToken_Forbidden() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        BuyersHelper.assignAdminRole(secretKeyPluginsScaApp, buyerToken);
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePisAuthorisationUrl_IncorrectRoleBuyerToken_Forbidden() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        BuyersHelper.assignCreatorRole(secretKeyPluginsScaApp, buyerToken);
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePisAuthorisationUrl_IncorrectRoleAuthUserToken_Forbidden() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authUserToken = getCreatorRoleAuthUserToken(secretKeyPluginsScaApp, buyerToken);

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, authUserToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePisAuthorisationUrl_NoSweeping_ConsentNotAuthorised() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONSENT_NOT_AUTHORIZED"));
    }

    @Test
    public void CreatePisAuthorisationUrl_NoScaPaymentRun_PaymentRunInvalidState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_RUN_INVALID_STATE"));
    }

    @Test
    public void CreatePisAuthorisationUrl_IncorrectReference_ReferenceInvalid() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final CreatePaymentRunResponseModel paymentRun = PaymentRunsHelper.createConfirmedPaymentRun(secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRun.getId());

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(RandomStringUtils.randomAlphanumeric(12)).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("REFERENCE_INVALID"));
    }

    @Test
    public void CreatePisAuthorisationUrl_NoReference_ReferenceInvalid() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final CreatePaymentRunResponseModel paymentRun = PaymentRunsHelper.createConfirmedPaymentRun(secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRun.getId());

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel("").build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("REFERENCE_INVALID"));
    }

    @Test
    public void CreatePisAuthorisationUrl_InvalidToken_Unauthorized() {
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(RandomStringUtils.randomAlphanumeric(12)).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, RandomStringUtils.randomAlphabetic(18), sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePisAuthorisationUrl_DifferentBuyerToken_ReferenceInvalid() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKeyPluginsScaApp, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String newBuyerToken = createBuyerAllRoles().getRight();
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunWithReference.getLeft()).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, newBuyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("REFERENCE_INVALID"));
    }

    @Test
    public void CreatePisAuthorisationUrl_NoToken_Unauthorized() {
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(RandomStringUtils.randomAlphanumeric(12)).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, null, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePisAuthorisationUrl_InvalidSharedKey_Unauthorized() {
        final String buyerToken = createBuyerAllRoles().getRight();

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(RandomStringUtils.randomAlphanumeric(12)).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePisAuthorisationUrl_NoSharedKey_Unauthorized() {
        final String buyerToken = createBuyerAllRoles().getRight();

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(RandomStringUtils.randomAlphanumeric(12)).build();

        PisUxComponentService.createAuthorisationUrlNoSharedKey(createPisAuthorisationUrlRequestModel, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePisAuthorisationUrl_DifferentProgrammeSharedKey_Unauthorized() {
        final String buyerToken = createBuyerAllRoles().getRight();

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(RandomStringUtils.randomAlphanumeric(12)).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePisAuthorisationUrl_NewLinkedAccountWithSupplierBankDetailsBeforeSca_BankAccountCannotBeLinkedAccount() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();
        final Pair<String, String> bankDetails = Pair.of("12345602", "500000");

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(bankDetails.getLeft(),
                        bankDetails.getRight()).build();
        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(createPaymentRunModel, accIds, secretKeyPluginsScaApp, buyerToken);

//        create second Linked Account with bankDetails as for PaymentRun
        final String consentId = AisUxComponentHelper.getConsentId(buyerToken, sharedKeyPluginsScaApp, linkedAccountId);
        final String reAuthUrl = AisUxComponentHelper.reAuthoriseAisConsent(consentId, buyerToken, sharedKeyPluginsScaApp);
        MockBankHelper.mockBankAis(reAuthUrl, bankDetails);

        final String secondLinkedAccountId = InstrumentsHelper.getLinkedAccounts(secretKeyPluginsScaApp, buyerToken).extract()
                .jsonPath()
                .get("linkedAccounts[1].id");

        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(secondLinkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);

//        get new reference for PaymentRun
        final String newReference = PaymentRunsHelper.getPaymentRunFundingInstructions(secondLinkedAccountId, paymentRunWithReference.getRight().getId(), secretKeyPluginsScaApp, buyerToken);

        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(newReference).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BANK_ACCOUNT_CANNOT_BE_LINKED_ACCOUNT"));
    }

    @Test
    public void CreatePisAuthorisationUrl_NewLinkedAccountWithSupplierBankDetailsAfterSca_BankAccountCannotBeLinkedAccount() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();
        final Pair<String, String> bankDetails = Pair.of("12345602", "500000");

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(bankDetails.getLeft(),
                        bankDetails.getRight()).build();
        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(createPaymentRunModel, accIds, secretKeyPluginsScaApp, buyerToken);

        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKeyPluginsScaApp, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

//        create second Linked Account with bankDetails as for PaymentRun
        final String consentId = AisUxComponentHelper.getConsentId(buyerToken, sharedKeyPluginsScaApp, linkedAccountId);
        final String reAuthUrl = AisUxComponentHelper.reAuthoriseAisConsent(consentId, buyerToken, sharedKeyPluginsScaApp);
        MockBankHelper.mockBankAis(reAuthUrl, bankDetails);
        final String secondLinkedAccountId = InstrumentsHelper.getLinkedAccounts(secretKeyPluginsScaApp, buyerToken).extract()
                .jsonPath()
                .get("linkedAccounts[1].id");
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(secondLinkedAccountId, buyerToken, secretKeyPluginsScaApp, sharedKeyPluginsScaApp);

//        get new reference for PaymentRun
        final String newReference = PaymentRunsHelper.getPaymentRunFundingInstructions(secondLinkedAccountId, paymentRunWithReference.getRight().getId(), secretKeyPluginsScaApp, buyerToken);

        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(newReference).build();

        PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BANK_ACCOUNT_CANNOT_BE_LINKED_ACCOUNT"));
    }

    private static Pair<String, String> createBuyerAllRoles() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createBuyerWithZba(secretKeyPluginsScaApp);
        BuyersHelper.assignAllRoles(secretKeyPluginsScaApp, authenticatedBuyer.getRight());

        return Pair.of(authenticatedBuyer);
    }
}
