package fpi.paymentrun.paymentruns;

import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.helpers.SweepingConsentHelper;
import fpi.helpers.simulator.MockBankHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.helpers.uicomponents.PisUxComponentsHelper;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.models.GetLinkedAccountsResponseModel;
import fpi.paymentrun.services.simulator.SimulatorService;
import opc.enums.opc.EnrolmentChannel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_CONFLICT;
import static org.hamcrest.Matchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_FULL_FLOW)
public class PaymentRunFlowTests extends BasePaymentRunSetup {
    final static String VERIFICATION_CODE = "123456";

    @Test
    public void PaymentRunFlow_PisFlow_ExecutingState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.EXECUTING.name(),
                State.SUBMITTED.name());
    }

    @Test
    public void PaymentRunFlow_SimulatedDeposit_CompletedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED.name(),
                State.COMPLETED.name());
    }

    @Test
    public void PaymentRunFlow_MultiplePaymentsPisFlow_ExecutingState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference =
                PaymentRunsHelper.createConfirmedPaymentRunWithReferenceMultiplePayments(accIds, secretKey, buyerToken, 3);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        PaymentRunsHelper.verifyPaymentRunStateMultiplePayments(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.EXECUTING.name(),
                State.SUBMITTED.name());
    }

    @Test
    public void PaymentRunFlow_MultiplePaymentsSimulatedDeposit_CompletedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference =
                PaymentRunsHelper.createConfirmedPaymentRunWithReferenceMultiplePayments(accIds, secretKey, buyerToken, 3);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        PaymentRunsHelper.verifyPaymentRunStateMultiplePayments(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED.name(),
                State.COMPLETED.name());
    }

    /**
     * This case can be reproduced with accountNumber: 55555552 for PaymentRun (PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCase())
     */
    @Test
    public void PaymentRunFlow_PaymentRunFailed_FailedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCase(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED_WITH_ERRORS.name(),
                State.FAILED.name());
    }

    @Test
    public void PaymentRunFlow_MultiplePaymentsPaymentRunFailed_FailedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference =
                PaymentRunsHelper.createConfirmedPaymentRunWithReferenceFailedCaseMultiplePayments(accIds, secretKey, buyerToken, 3);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        PaymentRunsHelper.verifyPaymentRunStateMultiplePayments(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED_WITH_ERRORS.name(),
                State.FAILED.name());
    }

    @Test
    public void PaymentRunFlow_SimulatedDepositTwice_GroupStateInvalid() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);

        MockBankHelper.mockBankPis(authorisationUrl);

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED.name(),
                State.COMPLETED.name());

        SimulatorService.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("GROUP_STATE_INVALID"));
    }

    @Test
    public void PaymentRunFlow_SimulatedDepositBeforePIS_CompletedState() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);

        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED.name(),
                State.COMPLETED.name());
    }

    @Test
    public void PaymentRunFlow_TwoLinkedAccounts_Success() throws MalformedURLException, URISyntaxException {
        final String buyerToken = createBuyerAllRoles().getRight();

//        PaymentRun for first linkedAccount
        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);
        MockBankHelper.mockBankPis(authorisationUrl);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.EXECUTING.name(),
                State.SUBMITTED.name());

//        Create new LinkedAccount
        final String consentId = AisUxComponentHelper.getConsentId(buyerToken, sharedKey, linkedAccountId);
        final String reAuthUrl = AisUxComponentHelper.reAuthoriseAisConsent(consentId, buyerToken, sharedKey);

        MockBankHelper.mockBankAis(reAuthUrl);

        final String secondLinkedAccountId = InstrumentsHelper.getLinkedAccounts(secretKey, buyerToken)
                .extract()
                .as(GetLinkedAccountsResponseModel.class)
                .getLinkedAccounts().get(1).getId();

//        Create PaymentRun with second LinkedAccount
        /**
         In the list of linkedAccounts, each linked account must be one per currency.
         Otherwise getPaymentRunFundingInstructions will get 409 "TOO_MANY_LINKED_ACCOUNT_PER_CURRENCY"
         See tests for GetPaymentRunFundingInstructionsTests.
         */
        final List<String> accIdsSecond = new ArrayList<>();
        accIdsSecond.add(secondLinkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReferenceSecond = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIdsSecond, secretKey, buyerToken);
        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(secondLinkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReferenceSecond.getRight().getId());

        final String authorisationUrlSecond = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReferenceSecond.getLeft(), buyerToken, sharedKey);
        MockBankHelper.mockBankPis(authorisationUrlSecond);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReferenceSecond.getRight().getId(), secretKey, buyerToken, State.EXECUTING.name(),
                State.SUBMITTED.name());
    }

    private static Pair<String, String> createBuyerAllRoles() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createBuyerWithZba(secretKey);
        BuyersHelper.assignAllRoles(secretKey, authenticatedBuyer.getRight());

        return Pair.of(authenticatedBuyer);
    }
}
