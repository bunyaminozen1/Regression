package fpi.helpers;

import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.CreatePaymentRunResponseModel;
import fpi.paymentrun.models.PaymentsModel;
import fpi.paymentrun.services.PaymentRunsService;
import io.restassured.response.ValidatableResponse;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

public class PaymentRunsHelper {

    public static CreatePaymentRunResponseModel createPaymentRun(final CreatePaymentRunModel createPaymentRunModel,
                                                                 final String secretKey,
                                                                 final String token) {
        return TestHelper.ensureAsExpected(30,
                        () -> PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, token),
                        SC_CREATED)
                .then()
                .extract()
                .as(CreatePaymentRunResponseModel.class);
    }

    public static CreatePaymentRunResponseModel createPaymentRun(final String secretKey,
                                                                 final String token) {

        final Pair<String, String> accountNumberAndSortCode =
                ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(),
                        accountNumberAndSortCode.getRight()).build();

        return createPaymentRun(createPaymentRunModel, secretKey, token);
    }

    public static String createConfirmedPaymentRun(final CreatePaymentRunModel createPaymentRunModel,
                                                   final String secretKey,
                                                   final String token) {
        final String paymentRunId = createPaymentRun(createPaymentRunModel, secretKey, token).getId();
        confirmPaymentRun(paymentRunId, secretKey, token);
        return paymentRunId;
    }

    public static CreatePaymentRunResponseModel createConfirmedPaymentRun(final String secretKey,
                                                                          final String token) {
        final CreatePaymentRunResponseModel paymentRun = createPaymentRun(secretKey, token);
        confirmPaymentRun(paymentRun.getId(), secretKey, token);
        return paymentRun;
    }

    public static void confirmPaymentRun(final String paymentRunId,
                                         final String secretKey,
                                         final String token) {

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunsService.confirmPaymentRun(paymentRunId, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void verifyPaymentRunState(final String paymentRunId,
                                             final String secretKey,
                                             final String token,
                                             final String paymentRunState,
                                             final String paymentState) {

        TestHelper.ensureAsExpected(80,
                () -> PaymentRunsService.getPaymentRun(paymentRunId, secretKey, token),
                x -> x.statusCode() == SC_OK
                        && x.jsonPath().getString("status").equals(paymentRunState)
                        && x.jsonPath().getString("payments[0].status").equals(paymentState),
                Optional.of(String.format("Expecting 200 with a paymentRun in state %s, check logged payload", paymentRunState)));
    }

    public static void verifyPaymentRunStateMultiplePayments(final String paymentRunId,
                                                             final String secretKey,
                                                             final String token,
                                                             final String paymentRunState,
                                                             final String paymentState) {

        final ValidatableResponse payments = TestHelper.ensureAsExpected(60,
                () -> PaymentRunsService.getPaymentRun(paymentRunId, secretKey, token),
                x -> x.statusCode() == SC_OK
                        && x.jsonPath().getString("status").equals(paymentRunState),
                Optional.of(String.format("Expecting 200 with a paymentRun in state %s, check logged payload", paymentRunState))).then();

        IntStream.range(0, payments.extract().jsonPath().get("payments.size()")).forEach(i ->
                payments.body(String.format("payments[%s].status", i), equalTo(paymentState)));
    }

    public static void cancelPaymentRun(final String paymentRunId,
                                        final String secretKey,
                                        final String token) {
        TestHelper.ensureAsExpected(30,
                () -> PaymentRunsService.cancelPaymentRun(paymentRunId, secretKey, token),
                SC_NO_CONTENT);
    }

    public static Pair<String, CreatePaymentRunResponseModel> createConfirmedPaymentRunWithReference(final List<String> linkedAccountIds,
                                                                                                     final String secretKey,
                                                                                                     final String token) {
        final Pair<String, String> accountNumberAndSortCode =
                ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(),
                        accountNumberAndSortCode.getRight()).build();

        final CreatePaymentRunResponseModel paymentRun =
                createPaymentRun(createPaymentRunModel, secretKey, token);

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunsService.confirmPaymentRun(paymentRun.getId(), secretKey, token),
                SC_NO_CONTENT);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", linkedAccountIds);

        final String reference = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters),
                        secretKey, token)
                .jsonPath().get("fundingInstructions[0].reference");

        return Pair.of(reference, paymentRun);
    }

    public static Pair<String, CreatePaymentRunResponseModel> createConfirmedPaymentRunWithReference(final CreatePaymentRunModel createPaymentRunModel,
                                                                                                     final List<String> linkedAccountIds,
                                                                                                     final String secretKey,
                                                                                                     final String token) {

        final CreatePaymentRunResponseModel paymentRun =
                createPaymentRun(createPaymentRunModel, secretKey, token);

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunsService.confirmPaymentRun(paymentRun.getId(), secretKey, token),
                SC_NO_CONTENT);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", linkedAccountIds);

        final String reference = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters),
                        secretKey, token)
                .jsonPath().get("fundingInstructions[0].reference");

        return Pair.of(reference, paymentRun);
    }

    public static Pair<String, CreatePaymentRunResponseModel> createConfirmedPaymentRunWithReferenceMultiplePayments(final List<String> linkedAccountIds,
                                                                                                                     final String secretKey,
                                                                                                                     final String token,
                                                                                                                     final int paymentsCount) {
        final Pair<String, String> accountNumberAndSortCode =
                ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        List<PaymentsModel> payments = new ArrayList<>();
        for (int i = 0; i < paymentsCount; i++) {
            payments.add(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build());
        }

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(),
                                accountNumberAndSortCode.getRight())
                        .payments(payments)
                        .build();

        final CreatePaymentRunResponseModel paymentRun =
                createPaymentRun(createPaymentRunModel, secretKey, token);

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunsService.confirmPaymentRun(paymentRun.getId(), secretKey, token),
                SC_NO_CONTENT);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", linkedAccountIds);

        final String reference = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters),
                        secretKey, token)
                .jsonPath().get("fundingInstructions[0].reference");

        return Pair.of(reference, paymentRun);
    }

    /**
     * This method created for FAILED states of Payment Run and to fire Sweeping webhook
     * These cases can be reproduced with accountNumber: 55555552 for PaymentRun
     */
    public static Pair<String, CreatePaymentRunResponseModel> createConfirmedPaymentRunWithReferenceFailedCase(final List<String> linkedAccountIds,
                                                                                                               final String secretKey,
                                                                                                               final String token) {
        final Pair<String, String> accountNumberAndSortCode =
                ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel("55555552",
                        accountNumberAndSortCode.getRight()).build();

        final CreatePaymentRunResponseModel paymentRun =
                createPaymentRun(createPaymentRunModel, secretKey, token);

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunsService.confirmPaymentRun(paymentRun.getId(), secretKey, token),
                SC_NO_CONTENT);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", linkedAccountIds);

        final String reference = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters),
                        secretKey, token)
                .jsonPath().get("fundingInstructions[0].reference");

        return Pair.of(reference, paymentRun);
    }

    public static Pair<String, CreatePaymentRunResponseModel> createConfirmedPaymentRunWithReferenceFailedCaseMultiplePayments(final List<String> linkedAccountIds,
                                                                                                                               final String secretKey,
                                                                                                                               final String token,
                                                                                                                               final int paymentsCount) {
        final Pair<String, String> accountNumberAndSortCode =
                ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        List<PaymentsModel> payments = new ArrayList<>();
        for (int i = 0; i < paymentsCount; i++) {
            payments.add(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel("55555552", accountNumberAndSortCode.getRight()).build());
        }

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(),
                                accountNumberAndSortCode.getRight())
                        .payments(payments)
                        .build();

        final CreatePaymentRunResponseModel paymentRun =
                createPaymentRun(createPaymentRunModel, secretKey, token);

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunsService.confirmPaymentRun(paymentRun.getId(), secretKey, token),
                SC_NO_CONTENT);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", linkedAccountIds);

        final String reference = PaymentRunsService.getPaymentRunFundingInstructions(paymentRun.getId(), Optional.of(filters),
                        secretKey, token)
                .jsonPath().get("fundingInstructions[0].reference");

        return Pair.of(reference, paymentRun);
    }

    public static String getPaymentRunFundingInstructions(final String linkedAccountId,
                                                          final String paymentRunId,
                                                          final String secretKey,
                                                          final String buyerToken) {
        final List<String> accIds = new ArrayList<>();
        accIds.add(linkedAccountId);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("linkedAccountIds[]", accIds);

        return PaymentRunsService.getPaymentRunFundingInstructions(paymentRunId, Optional.of(filters), secretKey, buyerToken)
                .jsonPath().get("fundingInstructions[0].reference");
    }
}
