package fpi.paymentrun.webhooks;

import commons.enums.State;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.ChallengesHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.helpers.SweepingConsentHelper;
import fpi.helpers.simulator.MockBankHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.helpers.uicomponents.PisUxComponentsHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.models.GetLinkedAccountsResponseModel;
import fpi.paymentrun.models.LinkedAccountResponseModel;
import fpi.paymentrun.models.PaymentAmountResponseModel;
import fpi.paymentrun.models.webhook.AuthenticationFactorsWebhookEventModel;
import fpi.paymentrun.models.webhook.BuyerActivatedWebhookEventModel;
import fpi.paymentrun.models.webhook.BuyerDeactivatedWebhookEventModel;
import fpi.paymentrun.models.webhook.LinkedAccountWebhookEventModel;
import fpi.paymentrun.models.webhook.PaymentRunPaymentWebhookEventModel;
import fpi.paymentrun.models.webhook.PaymentRunWebhookEventModel;
import fpi.paymentrun.models.webhook.PluginWebhookKybEventModel;
import fpi.paymentrun.models.webhook.PluginWebhookLoginPasswordEventModel;
import fpi.paymentrun.models.webhook.StepUpWebhookEventModel;
import fpi.paymentrun.models.webhook.SweepingWebhookEventModel;
import fpi.paymentrun.services.SpiOpenBanking.SpiOpenBankingService;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.KybState;
import opc.enums.opc.UserType;
import opc.helpers.ModelHelper;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static opc.junit.helpers.webhook.WebhookHelper.getPluginWebhookServiceEvent;
import static opc.junit.helpers.webhook.WebhookHelper.getPluginWebhookServiceEventByState;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebhookTests extends BaseWebhooksSetup {

    final static String VERIFICATION_CODE = "123456";
    final static String PASSWORD = "Pass1234!";

    @Test
    public void Webhooks_BuyerKyb_Success() {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);

        final long timestamp = Instant.now().toEpochMilli();

        BuyersHelper.verifyKyb(secretKey, buyer.getLeft());

        final PluginWebhookKybEventModel kybEvent = getKybWebhookResponse(timestamp, buyer.getLeft());

        assertKybEvent("buyerKYBWatch", buyer.getLeft(), kybEvent,
                KybState.APPROVED, Optional.empty(), Optional.empty());
    }

    @Test
    public void Webhooks_PaymentRunCreated_PendingConfirmation() {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final long timestamp = Instant.now().toEpochMilli();
        final CreatePaymentRunResponseModel paymentRun =
                PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRun.getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRun.getId());

        assertPaymentRunEvent("paymentRunWatch", State.PENDING_CONFIRMATION.name(),
                State.PENDING_CONFIRMATION.name(), paymentRunEvent, paymentRun);
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.PENDING_CONFIRMATION.name(), paymentEvent, paymentRun);
    }

    @Test
    public void Webhooks_PaymentRunConfirmed_PendingChallenge(){
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final CreatePaymentRunResponseModel paymentRun =
                PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());

        PaymentRunsHelper.confirmPaymentRun(paymentRun.getId(), secretKey, buyer.getRight());
        final long timestamp = Instant.now().toEpochMilli();

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRun.getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRun.getId());

        assertPaymentRunEvent("paymentRunWatch", State.PENDING_CHALLENGE.name(),
                State.PENDING_CHALLENGE.name(), paymentRunEvent, paymentRun);
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.PENDING_CHALLENGE.name(), paymentEvent, paymentRun);
    }

    @Test
    public void Webhooks_PaymentRunScaVerified_PendingFunding() {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final CreatePaymentRunResponseModel paymentRun =
                PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());
        PaymentRunsHelper.confirmPaymentRun(paymentRun.getId(), secretKey, buyer.getRight());

        ChallengesHelper.issueAndVerifyScaChallenge(buyer.getRight(), sharedKey, paymentRun.getId());
        final long timestamp = Instant.now().toEpochMilli();

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRun.getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRun.getId());

        assertPaymentRunEvent("paymentRunWatch", State.PENDING_FUNDING.name(),
                State.PENDING_FUNDING.name(), paymentRunEvent, paymentRun);
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.PENDING_FUNDING.name(), paymentEvent, paymentRun);
    }

    @Test
    public void Webhooks_PaymentRunPisFlow_ExecutingState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        final long timestamp = Instant.now().toEpochMilli();
        MockBankHelper.mockBankPis(authorisationUrl);

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRunWithReference.getRight().getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRunWithReference.getRight().getId());

        assertPaymentRunEvent("paymentRunWatch", State.EXECUTING.name(),
                State.SUBMITTED.name(), paymentRunEvent, paymentRunWithReference.getRight());
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.SUBMITTED.name(), paymentEvent, paymentRunWithReference.getRight());
    }

    @Test
    public void Webhooks_PaymentRunSimulatedDeposit_CompletedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        final long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRunWithReference.getRight().getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRunWithReference.getRight().getId(), State.COMPLETED.name());

        assertPaymentRunEvent("paymentRunWatch", State.COMPLETED.name(),
                State.COMPLETED.name(), paymentRunEvent, paymentRunWithReference.getRight());
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.COMPLETED.name(), paymentEvent, paymentRunWithReference.getRight());
    }

    /**
     * This case can be reproduced with accountNumber: 55555552 for PaymentRun (PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCase())
     */
    @Test
    public void Webhooks_PaymentRunFailed_FailedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCase(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        final long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRunWithReference.getRight().getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRunWithReference.getRight().getId());

        assertPaymentRunEvent("paymentRunWatch", State.COMPLETED_WITH_ERRORS.name(),
                State.FAILED.name(), paymentRunEvent, paymentRunWithReference.getRight());
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.FAILED.name(), paymentEvent, paymentRunWithReference.getRight());
    }

    /**
     * Buyer funds a payment run, but for some reason the payments aren't successful and there are funds on the ZBA that need to be refunded to the linked account
     * This case can be reproduced with accountNumber: 55555552 for PaymentRun (PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCase())
     */
    @Test
    public void Webhooks_SweepingEvent_FailedState() throws MalformedURLException, URISyntaxException, InterruptedException {
        final Pair<String, String> buyer = createBuyerAllRoles();

        final LinkedAccountResponseModel linkedAccount = InstrumentsHelper.createLinkedAccount(buyer.getRight(), secretKey, sharedKey)
                .extract().as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(0);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccount.getId());

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCase(accIds, secretKey, buyer.getRight());
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccount.getId(), buyer.getRight(), secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyer.getRight(), EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyer.getRight(), sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        TimeUnit.SECONDS.sleep(20);
        final long timestamp = Instant.now().toEpochMilli();
        SweepingConsentHelper.executeSweepingJob();

        final SweepingWebhookEventModel sweepingEvent = getSweepingWebhookResponse(timestamp, linkedAccount.getId());

        assertSweepingEvent("sweepingWatch", State.FAILED.name(), linkedAccount, buyer.getLeft(),
                paymentRunWithReference.getRight().getPayments().get(0).getPaymentAmount(), sweepingEvent);
    }

    @Test
    public void Webhooks_PaymentRunCancelled_Cancelled() {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();
        final CreatePaymentRunResponseModel paymentRun =
                PaymentRunsHelper.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight());

        PaymentRunsHelper.cancelPaymentRun(paymentRun.getId(), secretKey, buyer.getRight());
        final long timestamp = Instant.now().toEpochMilli();

        final PaymentRunWebhookEventModel paymentRunEvent = getPaymentRunWebhookResponse(timestamp, paymentRun.getId());
        final PaymentRunPaymentWebhookEventModel paymentEvent = getPaymentRunPaymentWebhookResponse(timestamp, paymentRun.getId());

        assertPaymentRunEvent("paymentRunWatch", State.CANCELLED.name(),
                State.CANCELLED.name(), paymentRunEvent, paymentRun);
        assertPaymentRunPaymentEvent("paymentRunPaymentWatch", State.CANCELLED.name(), paymentEvent, paymentRun);
    }

    @Test
    public void Webhooks_LinkedAccountSimulator_Success() {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey);
        final LinkedAccountResponseModel linkedAccount = InstrumentsHelper.getLinkedAccounts(secretKey, buyer.getRight())
                .extract()
                .as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(0);

        final LinkedAccountWebhookEventModel linkedAccountEvent = getLinkedAccountWebhookResponse(timestamp, linkedAccount.getId());
        assertLinkedAccountEvent("linkedAccountWatch", linkedAccount, linkedAccountEvent, "CONNECTED", "ACTIVE");
    }

    @Test
    public void Webhooks_LinkedAccountMockBank_Success() throws MalformedURLException, URISyntaxException {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        final String authorisationUrl = AisUxComponentHelper.createAuthorisationUrlMockBank(buyer.getRight(), sharedKey);
        MockBankHelper.mockBankAis(authorisationUrl);
        final LinkedAccountResponseModel linkedAccount = InstrumentsHelper.getLinkedAccounts(secretKey, buyer.getRight())
                .extract()
                .as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(0);

        final LinkedAccountWebhookEventModel linkedAccountEvent = getLinkedAccountWebhookResponse(timestamp, linkedAccount.getId());
        assertLinkedAccountEvent("linkedAccountWatch", linkedAccount, linkedAccountEvent, "CONNECTED", "ACTIVE");
    }

    @Test
    public void Webhooks_ReAuthoriseConsent_NewLinkedAccountCreated() throws MalformedURLException, URISyntaxException {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());
        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyer.getRight(), secretKey, sharedKey);

        InstrumentsHelper.getLinkedAccounts(secretKey, buyer.getRight())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        final String consentId = AisUxComponentHelper.getConsentId(buyer.getRight(), sharedKey, linkedAccountId);

        final long timestamp = Instant.now().toEpochMilli();
        final String reAuthUrl = AisUxComponentHelper.reAuthoriseAisConsent(consentId, buyer.getRight(), sharedKey);

        MockBankHelper.mockBankAis(reAuthUrl);

        final LinkedAccountResponseModel linkedAccount = InstrumentsHelper.getLinkedAccounts(secretKey, buyer.getRight())
                .extract()
                .as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(1);

        final LinkedAccountWebhookEventModel linkedAccountEvent = getLinkedAccountWebhookResponse(timestamp, linkedAccount.getId());
        assertLinkedAccountEvent("linkedAccountWatch", linkedAccount, linkedAccountEvent, "CONNECTED", "ACTIVE");
    }

    @Test
    @DisplayName("Webhooks_DeleteBankingAccount_LinkedAccountDisconnected() - SPI OB doesn't work on QA. Will be fixed soon - should be added authKey for OB")
    public void Webhooks_DeleteBankingAccount_LinkedAccountDisconnected() throws MalformedURLException, URISyntaxException {
        final Pair<String, String> buyer = createBuyerWithZba();
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());
        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyer.getRight(), secretKey, sharedKey);

        final LinkedAccountResponseModel linkedAccount = InstrumentsHelper.getLinkedAccounts(secretKey, buyer.getRight())
                .extract()
                .as(GetLinkedAccountsResponseModel.class).getLinkedAccounts().get(0);

        final String consentId = AisUxComponentHelper.getConsentId(buyer.getRight(), sharedKey, linkedAccountId);

        final String bankAccountId = SpiOpenBankingService.getAccounts(consentId)
                .then()
                .extract()
                .jsonPath()
                .getString("accounts[0].id");

        final long timestamp = Instant.now().toEpochMilli();
        SpiOpenBankingService.deleteAccountById(bankAccountId)
                .then()
                .statusCode(SC_OK)
                .body("success",  equalTo(true));

        final LinkedAccountWebhookEventModel linkedAccountEvent = getLinkedAccountWebhookResponse(timestamp, linkedAccountId);
        assertLinkedAccountEvent("linkedAccountWatch", linkedAccount, linkedAccountEvent, "DISCONNECTED", "REVOKED");
    }

    /**
     * buyerActivatedWatch: should be fired once the ZBA is ready (after being IBAN upgraded that it's done automatically on creation)
     * or when the buyer is manually activated from the portal or Innovator API
     */
//    At the time of test implementation, it was possible to create the ZBA only automatically.
//    Cases with manually activation and from the portal should be added when it's possible to automate
    @Test
    public void Webhooks_ActivateBuyerZbaCreatedAutomatically_Success() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        BuyersHelper.verifyKyb(secretKey, buyer.getLeft());
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKey), secretKey);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());

        final BuyerActivatedWebhookEventModel buyerActivatedEvent = getBuyerActivatedWebhookResponse(timestamp, buyer.getLeft());
        assertBuyerActivatedEvent("buyerActivatedWatch", buyer.getLeft(), buyerActivatedEvent);
    }

    /**
     * buyerDeactivatedWatch: disable a buyer from the portal or via Innovator API
     */
    @Test
    public void Webhooks_DeactivateBuyerInnovatorEndpoint_Success() {
        final Pair<String, String> buyer = createBuyerWithZba();

        final long timestamp = Instant.now().toEpochMilli();
        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(true, "TEMPORARY"),
                buyer.getLeft(), innovatorToken);

        final BuyerDeactivatedWebhookEventModel buyerDeactivatedEvent = getBuyerDeactivatedWebhookResponse(timestamp, buyer.getLeft());
        assertBuyerDeactivatedEvent("buyerDeactivatedWatch", buyer.getLeft(), "EMBEDDER", "TEMPORARY", buyerDeactivatedEvent);
    }

    @Test
    public void Webhooks_DeactivateBuyerAdminEndpoint_Success() {
        final Pair<String, String> buyer = createBuyerWithZba();

        final long timestamp = Instant.now().toEpochMilli();
        AdminHelper.deactivateCorporate(new DeactivateIdentityModel(true, "TEMPORARY"),
                buyer.getLeft(), impersonatedAdminToken);

        final BuyerDeactivatedWebhookEventModel buyerDeactivatedEvent = getBuyerDeactivatedWebhookResponse(timestamp, buyer.getLeft());
        assertBuyerDeactivatedEvent("buyerDeactivatedWatch", buyer.getLeft(), "ADMIN", "TEMPORARY", buyerDeactivatedEvent);
    }

    @Test
    public void Webhooks_AuthenticationFactors_Success() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        BuyersHelper.verifyKyb(secretKey, buyer.getLeft());

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());

        final AuthenticationFactorsWebhookEventModel authenticationFactorsEvent = getAuthenticationFactorsWebhookResponse(timestamp, buyer.getLeft());
        assertAuthenticationFactorsEvent("authenticationFactorsWatch", buyer.getLeft(), UserType.ROOT.name(), "ACTIVE", "SMS_OTP", authenticationFactorsEvent);
    }

    @Test
    public void Webhooks_StepUpOtpVerified_Success() throws SQLException {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        BuyersHelper.verifyKyb(secretKey, buyer.getLeft());
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());

        final String challengeId = AuthSessionsDatabaseHelper.getChallengeWithIdentityId(
                buyer.getLeft()).get(0).get("id");

        final StepUpWebhookEventModel authenticationFactorsEvent = getStepUpWebhookResponse(timestamp, buyer.getLeft());
        assertStepUpEvent("stepupWatch", buyer.getLeft(), UserType.ROOT.name(), challengeId, "VERIFIED", "SMS_OTP", authenticationFactorsEvent);
    }

    @Test
    public void Webhooks_BuyerLoginWithPassword_Verified() throws InterruptedException {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), PASSWORD, secretKey);

        final PluginWebhookLoginPasswordEventModel loginEvent = getLoginPasswordWebhookResponse(timestamp, buyer.getLeft());

        assertLoginEvent("loginWatch", buyer.getLeft(), buyer.getLeft(), UserType.ROOT.name(),
                "VERIFIED", "PASSWORD", loginEvent);
    }

    @Test
    public void Webhooks_BuyerAuthUserLoginWithPassword_Verified() throws InterruptedException {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Triple<String, BuyerAuthorisedUserModel, String> user = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
        final String userEmail = user.getMiddle().getEmail();
        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationHelper.login(userEmail, PASSWORD, secretKey);

        final PluginWebhookLoginPasswordEventModel loginEvent = getLoginPasswordWebhookResponse(timestamp, buyer.getLeft());

        assertLoginEvent("loginWatch", user.getLeft(), buyer.getLeft(), UserType.USER.name(),
                "VERIFIED", "PASSWORD", loginEvent);
    }

    @Test
    public void Webhooks_BuyerLoginWithInvalidPassword_Declined() throws InterruptedException {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationService.loginWithPassword(new LoginModel(createBuyerModel.getAdminUser().getEmail(),
                new PasswordModel(RandomStringUtils.randomAlphanumeric(5))), secretKey);

        final PluginWebhookLoginPasswordEventModel loginEvent = getLoginPasswordWebhookResponse(timestamp, buyer.getLeft());

        assertLoginEvent("loginWatch", buyer.getLeft(), buyer.getLeft(), UserType.ROOT.name(),
                "DECLINED", "PASSWORD", loginEvent);
    }

    @Test
    public void Webhooks_BuyerAuthUserLoginWithInvalidPassword_Declined() throws InterruptedException {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Triple<String, BuyerAuthorisedUserModel, String> user = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
        final String userEmail = user.getMiddle().getEmail();
        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationService.loginWithPassword(new LoginModel(userEmail,
                new PasswordModel(RandomStringUtils.randomAlphanumeric(5))), secretKey);

        final PluginWebhookLoginPasswordEventModel loginEvent = getLoginPasswordWebhookResponse(timestamp, buyer.getLeft());

        assertLoginEvent("loginWatch", user.getLeft(), buyer.getLeft(), UserType.USER.name(),
                "DECLINED", "PASSWORD", loginEvent);
    }

    private PluginWebhookKybEventModel getKybWebhookResponse(final long timestamp,
                                                             final String identityId) {
        return (PluginWebhookKybEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "buyerKYBWatch",
                Pair.of("data.buyerId", identityId),
                PluginWebhookKybEventModel.class,
                ApiSchemaDefinition.KybEvent);
    }

    private PaymentRunWebhookEventModel getPaymentRunWebhookResponse(final long timestamp,
                                                                     final String paymentRunId) {
        return (PaymentRunWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "paymentRunWatch",
                Pair.of("data.id", paymentRunId),
                PaymentRunWebhookEventModel.class,
                ApiSchemaDefinition.PaymentRunEvent);
    }

    private SweepingWebhookEventModel getSweepingWebhookResponse(final long timestamp,
                                                                 final String linkedAccountId) {
        return (SweepingWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "sweepingWatch",
                Pair.of("data.destination.linkedAccountId", linkedAccountId),
                SweepingWebhookEventModel.class,
                ApiSchemaDefinition.SweepingEvent);
    }

    private LinkedAccountWebhookEventModel getLinkedAccountWebhookResponse(final long timestamp,
                                                                           final String linkedAccountId) {
        return (LinkedAccountWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "linkedAccountWatch",
                Pair.of("data.id", linkedAccountId),
                LinkedAccountWebhookEventModel.class,
                ApiSchemaDefinition.LinkedAccountEvent);
    }

    private PaymentRunPaymentWebhookEventModel getPaymentRunPaymentWebhookResponse(final long timestamp,
                                                                                   final String paymentRunId) {
        return (PaymentRunPaymentWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "paymentRunPaymentWatch",
                Pair.of("data.paymentRunId", paymentRunId),
                PaymentRunPaymentWebhookEventModel.class,
                ApiSchemaDefinition.PaymentRunPaymentEvent);
    }

    private PaymentRunPaymentWebhookEventModel getPaymentRunPaymentWebhookResponse(final long timestamp,
                                                                                   final String paymentRunId,
                                                                                   final String state) {
        return (PaymentRunPaymentWebhookEventModel) getPluginWebhookServiceEventByState(
                webhookServiceDetails.getLeft(),
                timestamp,
                "paymentRunPaymentWatch",
                Pair.of("data.paymentRunId", paymentRunId),
                Pair.of("data.status", state),
                PaymentRunPaymentWebhookEventModel.class,
                ApiSchemaDefinition.PaymentRunPaymentEvent);
    }

    private PluginWebhookLoginPasswordEventModel getLoginPasswordWebhookResponse(final long timestamp,
                                                                                 final String identityId) {
        return (PluginWebhookLoginPasswordEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "loginWatch",
                Pair.of("data.buyerId", identityId),
                PluginWebhookLoginPasswordEventModel.class,
                ApiSchemaDefinition.LoginEvent);
    }

    private BuyerActivatedWebhookEventModel getBuyerActivatedWebhookResponse(final long timestamp,
                                                                             final String identityId) {
        return (BuyerActivatedWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "buyerActivatedWatch",
                Pair.of("data.buyerId", identityId),
                BuyerActivatedWebhookEventModel.class,
                ApiSchemaDefinition.BuyerActivatedEvent);
    }

    private BuyerDeactivatedWebhookEventModel getBuyerDeactivatedWebhookResponse(final long timestamp,
                                                                                 final String identityId) {
        return (BuyerDeactivatedWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "buyerDeactivatedWatch",
                Pair.of("data.buyerId", identityId),
                BuyerDeactivatedWebhookEventModel.class,
                ApiSchemaDefinition.BuyerDeactivatedEvent);
    }

    private AuthenticationFactorsWebhookEventModel getAuthenticationFactorsWebhookResponse(final long timestamp,
                                                                                           final String identityId) {
        return (AuthenticationFactorsWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "authenticationFactorsWatch",
                Pair.of("data.credential.id", identityId),
                AuthenticationFactorsWebhookEventModel.class,
                ApiSchemaDefinition.AuthenticationFactorsEvent);
    }

    private StepUpWebhookEventModel getStepUpWebhookResponse(final long timestamp,
                                                             final String identityId) {
        return (StepUpWebhookEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "stepupWatch",
                Pair.of("data.buyerId", identityId),
                StepUpWebhookEventModel.class,
                ApiSchemaDefinition.StepUpEvent);
    }

    private void assertKybEvent(final String type,
                                final String identityId,
                                final PluginWebhookKybEventModel kybEvent,
                                final KybState kybState,
                                final Optional<List<String>> details,
                                final Optional<String> rejectionComment) {

        assertEquals(type, kybEvent.getType());
        assertEquals(identityId, kybEvent.getData().getBuyerId());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(kybEvent.getData().getDetails()).size());
        assertEquals(rejectionComment.orElse(""), kybEvent.getData().getRejectionComment());
        assertEquals(kybState.name(), kybEvent.getData().getStatus());
        assertEquals(kybState.name(), kybEvent.getData().getOngoingStatus());

        if (details.isPresent() && details.get().size() > 0) {
            details.get().forEach(eventDetail ->
                    assertEquals(eventDetail,
                            Arrays.stream(kybEvent.getData().getDetails())
                                    .filter(x -> x.equals(eventDetail))
                                    .findFirst()
                                    .orElse(String.format("Details did not match. Expected %s Actual %s",
                                            eventDetail, Arrays.asList(kybEvent.getData().getDetails()).size() == 0 ? "[No details returned from sumsub]" :
                                                    String.join(", ", kybEvent.getData().getDetails())))));
        }
    }

    private void assertLoginEvent(final String eventType,
                                  final String userId,
                                  final String identityId,
                                  final String userType,
                                  final String status,
                                  final String type,
                                  final PluginWebhookLoginPasswordEventModel loginEvent) {

        assertEquals(userId, loginEvent.getData().getCredential().get("id"));
        assertEquals(userType, loginEvent.getData().getCredential().get("type"));
        assertEquals(identityId, loginEvent.getData().getBuyerId());
        assertEquals(status, loginEvent.getData().getStatus());
        assertEquals(type, loginEvent.getData().getType());
        assertNotNull(loginEvent.getData().getPublishedTimestamp());
        assertEquals(eventType, loginEvent.getType());
    }

    private void assertPaymentRunEvent(final String type,
                                       final String paymentRunStatus,
                                       final String paymentsStatus,
                                       final PaymentRunWebhookEventModel paymentRunEvent,
                                       final CreatePaymentRunResponseModel paymentRun) {

        assertEquals(paymentRun.getCreatedAt(), paymentRunEvent.getData().getCreatedAt());
        assertEquals(paymentRun.getCreatedBy(), paymentRunEvent.getData().getCreatedBy());
        assertEquals(paymentRun.getDescription(), paymentRunEvent.getData().getDescription());
        assertEquals(paymentRun.getId(), paymentRunEvent.getData().getId());
        assertEquals(paymentRun.getPaymentRunRef(), paymentRunEvent.getData().getPaymentRunRef());
        assertEquals(paymentRun.getPayments().get(0).getExternalRef(), paymentRunEvent.getData().getPayments().get(0).getExternalRef());
        assertEquals(paymentRun.getPayments().get(0).getId(), paymentRunEvent.getData().getPayments().get(0).getId());
        assertEquals(String.valueOf(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()), paymentRunEvent.getData().getPayments().get(0).getPaymentAmount().get("amount"));
        assertEquals(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency(), paymentRunEvent.getData().getPayments().get(0).getPaymentAmount().get("currency"));
        assertEquals(paymentRun.getPayments().get(0).getPaymentRef(), paymentRunEvent.getData().getPayments().get(0).getPaymentRef());
        assertEquals(paymentRun.getPayments().get(0).getReference(), paymentRunEvent.getData().getPayments().get(0).getReference());
        assertEquals(paymentsStatus, paymentRunEvent.getData().getPayments().get(0).getStatus());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getAddress(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getAddress());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getBankAccountDetails().get("accountNumber"));
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getBankAccountDetails().get("sortCode"));
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankAddress(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getBankAddress());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankCountry(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getBankCountry());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankName(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getBankName());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getName(), paymentRunEvent.getData().getPayments().get(0).getSupplier().getName());
        assertEquals(paymentRunStatus, paymentRunEvent.getData().getStatus());
        assertEquals(paymentRun.getTag(), paymentRunEvent.getData().getTag());
        assertEquals(type, paymentRunEvent.getType());
    }

    private void assertPaymentRunPaymentEvent(final String eventType,
                                              final String paymentsStatus,
                                              final PaymentRunPaymentWebhookEventModel paymentEvent,
                                              final CreatePaymentRunResponseModel paymentRun) {

        assertEquals(paymentRun.getPayments().get(0).getId(), paymentEvent.getData().getId());
        assertEquals(paymentRun.getPayments().get(0).getPaymentRef(), paymentEvent.getData().getPaymentRef());
        assertEquals(paymentRun.getPayments().get(0).getExternalRef(), paymentEvent.getData().getExternalRef());
        assertEquals(paymentRun.getId(), paymentEvent.getData().getPaymentRunId());
        assertEquals(String.valueOf(paymentRun.getPayments().get(0).getPaymentAmount().getAmount()), paymentEvent.getData().getPaymentAmount().get("amount"));
        assertEquals(paymentRun.getPayments().get(0).getPaymentAmount().getCurrency(), paymentEvent.getData().getPaymentAmount().get("currency"));
        assertEquals(paymentRun.getPayments().get(0).getReference(), paymentEvent.getData().getReference());
        assertEquals(paymentsStatus, paymentEvent.getData().getStatus());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getAddress(), paymentEvent.getData().getSupplier().getAddress());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber(), paymentEvent.getData().getSupplier().getBankAccountDetails().get("accountNumber"));
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode(), paymentEvent.getData().getSupplier().getBankAccountDetails().get("sortCode"));
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankAddress(), paymentEvent.getData().getSupplier().getBankAddress());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankCountry(), paymentEvent.getData().getSupplier().getBankCountry());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getBankName(), paymentEvent.getData().getSupplier().getBankName());
        assertEquals(paymentRun.getPayments().get(0).getSupplier().getName(), paymentEvent.getData().getSupplier().getName());
        assertEquals(eventType, paymentEvent.getType());
    }

    private void assertSweepingEvent(final String type,
                                     final String sweepingStatus,
                                     final LinkedAccountResponseModel linkedAccount,
                                     final String buyerId,
                                     final PaymentAmountResponseModel paymentAmount,
                                     final SweepingWebhookEventModel sweepingEvent) {

        assertEquals(buyerId, sweepingEvent.getData().getBuyerId());
        assertEquals(linkedAccount.getAccountIdentification().getAccountNumber(), sweepingEvent.getData().getDestination().getBankAccountDetails().get("accountNumber"));
        assertEquals(linkedAccount.getAccountIdentification().getSortCode(), sweepingEvent.getData().getDestination().getBankAccountDetails().get("sortCode"));
        assertEquals(linkedAccount.getId(), sweepingEvent.getData().getDestination().getLinkedAccountId());
        assertEquals(sweepingStatus, sweepingEvent.getData().getStatus());
        assertNotNull(sweepingEvent.getData().getSweepingId());
        assertEquals(paymentAmount.getCurrency(), sweepingEvent.getData().getTransactionAmount().get("currency"));
        assertEquals(String.valueOf(paymentAmount.getAmount()), sweepingEvent.getData().getTransactionAmount().get("amount"));
        assertNotNull(sweepingEvent.getData().getTransactionDate());
        assertEquals(type, sweepingEvent.getType());
    }

    private void assertLinkedAccountEvent(final String eventType,
                                          final LinkedAccountResponseModel linkedAccount,
                                          final LinkedAccountWebhookEventModel linkedAccountEvent,
                                          final String accountState,
                                          final String consentState) {
        assertEquals(linkedAccount.getId(), linkedAccountEvent.getData().getId());
        assertEquals(linkedAccount.getCurrency(), linkedAccountEvent.getData().getCurrency());
        assertEquals(linkedAccount.getAccountIdentification().getAccountNumber(), linkedAccountEvent.getData().getAccountIdentification().get("accountNumber"));
        assertEquals(linkedAccount.getAccountIdentification().getSortCode(), linkedAccountEvent.getData().getAccountIdentification().get("sortCode"));
        assertEquals(linkedAccount.getInstitution().getCountries().get(0), linkedAccountEvent.getData().getInstitution().getCountries().get(0));
        assertEquals(linkedAccount.getInstitution().getDisplayName(), linkedAccountEvent.getData().getInstitution().getDisplayName());
        assertEquals(linkedAccount.getInstitution().getId(), linkedAccountEvent.getData().getInstitution().getId());
        assertEquals(linkedAccount.getInstitution().getImages().getIcon(), linkedAccountEvent.getData().getInstitution().getImages().get("icon"));
        assertEquals(linkedAccount.getInstitution().getImages().getLogo(), linkedAccountEvent.getData().getInstitution().getImages().get("logo"));
        assertEquals(linkedAccount.getInstitution().getInfo().getHelplinePhoneNumber(), linkedAccountEvent.getData().getInstitution().getInfo().get("helplinePhoneNumber"));
        assertEquals(linkedAccount.getInstitution().getInfo().getLoginUrl(), linkedAccountEvent.getData().getInstitution().getInfo().get("loginUrl"));
        assertEquals(linkedAccount.getConsent().getExpiresAt(), linkedAccountEvent.getData().getConsent().getExpiresAt());
        assertEquals(consentState, linkedAccountEvent.getData().getConsent().getStatus());
        assertNotNull(linkedAccountEvent.getData().getConsent().getExpiresIn());
        assertEquals(accountState, linkedAccountEvent.getData().getStatus());
        assertEquals(eventType, linkedAccountEvent.getType());
    }

    private void assertBuyerActivatedEvent(final String eventType,
                                           final String buyerId,
                                           final BuyerActivatedWebhookEventModel buyerActivateEvent) {
        assertEquals(buyerId, buyerActivateEvent.getData().getBuyerId());
        assertEquals("UNDEFINED", buyerActivateEvent.getData().getActionDoneBy());
        assertEquals(eventType, buyerActivateEvent.getType());
    }

    private void assertBuyerDeactivatedEvent(final String eventType,
                                             final String buyerId,
                                             final String actionDoneBy,
                                             final String reasonCode,
                                             final BuyerDeactivatedWebhookEventModel buyerDeactivateEvent) {
        assertEquals(buyerId, buyerDeactivateEvent.getData().getBuyerId());
        assertEquals(actionDoneBy, buyerDeactivateEvent.getData().getActionDoneBy());
        assertEquals(reasonCode, buyerDeactivateEvent.getData().getReasonCode());
        assertEquals(eventType, buyerDeactivateEvent.getType());
    }

    private void assertAuthenticationFactorsEvent(final String eventType,
                                                  final String userId,
                                                  final String userType,
                                                  final String status,
                                                  final String type,
                                                  final AuthenticationFactorsWebhookEventModel authenticationFactorsEvent) {
        assertEquals(userId, authenticationFactorsEvent.getData().getCredential().get("id"));
        assertEquals(userType, authenticationFactorsEvent.getData().getCredential().get("type"));
        assertNotNull(authenticationFactorsEvent.getData().getPublishedTimestamp());
        assertEquals(status, authenticationFactorsEvent.getData().getStatus());
        assertEquals(type, authenticationFactorsEvent.getData().getType());
        assertEquals(eventType, authenticationFactorsEvent.getType());
    }

    private void assertStepUpEvent(final String eventType,
                                   final String userId,
                                   final String userType,
                                   final String challengeId,
                                   final String status,
                                   final String type,
                                   final StepUpWebhookEventModel stepUpEvent) {
        assertEquals(userId, stepUpEvent.getData().getCredential().get("id"));
        assertEquals(userType, stepUpEvent.getData().getCredential().get("type"));
        assertNotNull(stepUpEvent.getData().getPublishedTimestamp());
        assertNotNull(stepUpEvent.getData().getAuthToken());
        assertEquals(userId, stepUpEvent.getData().getBuyerId());
        assertEquals(challengeId, stepUpEvent.getData().getChallengeId());
        assertEquals(status, stepUpEvent.getData().getStatus());
        assertEquals(type, stepUpEvent.getData().getType());
        assertEquals(eventType, stepUpEvent.getType());
    }

    private Pair<String, String> createBuyerWithZba() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        BuyersHelper.verifyKyb(secretKey, buyer.getLeft());
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKey), secretKey);
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        return buyer;
    }

    private static Pair<String, String> createBuyerAllRoles() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createBuyerWithZba(secretKey);
        BuyersHelper.assignAllRoles(secretKey, authenticatedBuyer.getRight());

        return Pair.of(authenticatedBuyer);
    }
}
