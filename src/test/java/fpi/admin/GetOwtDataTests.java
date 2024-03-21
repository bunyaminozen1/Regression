package fpi.admin;

import commons.enums.State;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.helpers.PaymentRunsHelper;
import fpi.helpers.SweepingConsentHelper;
import fpi.helpers.simulator.MockBankHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.helpers.uicomponents.PisUxComponentsHelper;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.services.admin.AdminService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class GetOwtDataTests extends BaseAdminSetup {
    final static String VERIFICATION_CODE = "123456";
    private static String buyerToken;
    private static String paymentRunId;
    private static String paymentRunPaymentId;
    private static String owtId;


    @BeforeAll
    public static void Setup() throws MalformedURLException, URISyntaxException {
        createBuyerWithPaymentRun();
        owtId =
                OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKey, buyerToken)
                        .then()
                        .extract()
                        .path("transfer[0].id");
    }

    @Test
    public void GetOwtData_ValidOwtId_Success() {
        AdminService.getOwtData(owtId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("paymentRunGroupId", notNullValue())
                .body("paymentRunId", equalTo(paymentRunId))
                .body("paymentRunLineId", equalTo(paymentRunPaymentId));
    }

    @Test
    public void GetOwtData_InvalidOwtId_PaymentRunNotFound() {
        AdminService.getOwtData(RandomStringUtils.randomAlphabetic(18), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_RUN_NOT_FOUND"));
    }

    @Test
    public void GetOwtData_EmptyOwtId_NotFound() {
        AdminService.getOwtData("", adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetOwtData_InvalidToken_Unauthorised() {
        AdminService.getOwtData(owtId, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetOwtData_EmptyToken_Unauthorised() {
        AdminService.getOwtData(owtId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetOwtData_NullToken_Unauthorised() {
        AdminService.getOwtData(owtId, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    public static void createBuyerWithPaymentRun() throws MalformedURLException, URISyntaxException {
        final Pair<String, String> buyer = BuyersHelper.createBuyerWithZba(secretKey);
        buyerToken = buyer.getRight();
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);

        final Pair<String, CreatePaymentRunResponseModel> paymentRunWithReference = PaymentRunsHelper.createConfirmedPaymentRunWithReference(accIds, secretKey, buyerToken);
        paymentRunId = paymentRunWithReference.getRight().getId();
        paymentRunPaymentId = paymentRunWithReference.getRight().getPayments().get(0).getId();

        SweepingConsentHelper.issueAndVerifySweepingConsentChallenge(linkedAccountId, buyerToken, secretKey, sharedKey);
        AuthenticationHelper.startAndVerifyScaPaymentRun(VERIFICATION_CODE, sharedKey, buyerToken, EnrolmentChannel.SMS.name(), paymentRunWithReference.getRight().getId());

        final String authorisationUrl = PisUxComponentsHelper.createAuthorisationUrl(paymentRunWithReference.getLeft(), buyerToken, sharedKey);
        MockBankHelper.mockBankPis(authorisationUrl);
        SimulatorHelper.simulateFunding(paymentRunWithReference.getRight().getId(), paymentRunWithReference.getLeft(), secretKey);
        PaymentRunsHelper.verifyPaymentRunState(paymentRunWithReference.getRight().getId(), secretKey, buyerToken, State.COMPLETED.name(),
                State.COMPLETED.name());
    }

}
