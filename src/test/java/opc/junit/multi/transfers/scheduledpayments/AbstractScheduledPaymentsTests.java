package opc.junit.multi.transfers.scheduledpayments;

import opc.enums.opc.IdentityType;
import commons.enums.State;
import opc.junit.database.TransfersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.TransfersHelper;
import opc.junit.multi.transfers.BaseTransfersSetup;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.transfers.CancelScheduledModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CancellationModel;
import opc.services.multi.TransfersService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractScheduledPaymentsTests extends BaseTransfersSetup {
    protected abstract String getToken();

    protected abstract String getIdentityId();

    protected abstract String getCurrency();

    protected abstract String getManagedAccountProfileId();

    protected abstract IdentityType getIdentityType();

    /**
     * Test cases for Transfers scheduled payments
     * Documentation:https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
     * <p>
     * Main ticket: DEV-5202
     * <p>
     * The main cases:
     * 1. Create Single Scheduled Payment
     * 2. Invalid Timestamps
     * 3. Cancel Scheduled Payments
     * 4. Conflicts and bad requests
     * 5. Execute Scheduled Payment
     */

    @Test
    public void TransferScheduledPayment_CreateValidScheduledPaymentMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));


        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc",})
    public void TransferScheduledPayment_InvalidTimestamp_BadRequest(final String timestamp) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel = TransferFundsModel
                .DefaultTransfersModel(transfersProfileId, getCurrency(), sendAmount, Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                        Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                .setScheduledTimestamp(timestamp)
                .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.scheduledTimestamp: must match \"^[0-9]+$\""));
    }

    @Test
    public void TransferScheduledPayment_TimestampInPast_Conflict() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel = TransferFundsModel
                .DefaultTransfersModel(transfersProfileId, getCurrency(), sendAmount, Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                        Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                .setScheduledTimestamp(TestHelper.generateTimestampBefore(30))
                .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SCHEDULED_TIMESTAMP_INVALID"));
    }

    @Test
    public void TransferScheduledPayment_CreateAndExecuteValidScheduledPayment_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        SimulatorService.scheduledExecuteTransfer(innovatorId, transferId)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("COMPLETED"));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.COMPLETED.name());
    }

    @Test
    public void TransferScheduledPayment_CreateAndExecuteValidScheduledPaymentNoFunds_InsufficientFunds() throws SQLException {
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        SimulatorService.scheduledExecuteTransfer(innovatorId, transferId)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("ERROR"));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.FAILED.name());
        final String validationFailure = TransfersDatabaseHelper.getTransferById(transferId).get(0).get("failure_reason");
        Assertions.assertEquals("SOURCE_INSUFFICIENT_FUNDS", validationFailure);
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPayment_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(transferId).cancellationReason("Cancellation reason").build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(transferId))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        TransfersService.getTransfer(secretKey, transferId, getToken())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("CANCELLED"));
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPaymentNoCancellationReason_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(transferId).build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(transferId))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        TransfersService.getTransfer(secretKey, transferId, getToken())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("CANCELLED"));
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPaymentCancellationReasonTooLong_BadRequest() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(transferId).cancellationReason(RandomStringUtils.randomAlphabetic(51)).build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].cancellationReason: size must be between 0 and 50"));
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPaymentCancellationReasonInvalidCharacters_BadRequest() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(transferId).cancellationReason("ɥ ɦ ɧ ɨ ɩ ɪ ɫ ɬ ɭ ɮ").build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].cancellationReason: must match \"^[ a-zA-Z0-9_-]+$\""));
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPaymentInvalidState_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendTransfer(transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.COMPLETED.name());

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(transferId).build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(transferId))
                .body("cancellations[0].errorCode", equalTo("TRANSACTION_NOT_SCHEDULED"))
                .body("cancellations[0].state", equalTo("ERROR"));
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPaymentMissingId_BadRequest() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(getManagedAccountProfileId(), getCurrency(), getToken(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), getCurrency(), depositAmount);

        final String transferId = TransfersHelper.sendScheduledTransfer(TestHelper.generateTimestampAfter(30), transfersProfileId, getCurrency(), sendAmount,
                secretKey, getToken(), Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        TransfersHelper.ensureTransferState(secretKey, transferId, getToken(), State.SCHEDULED.name());

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().cancellationReason("Cancellation reason").build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].id: must not be blank"));
    }

    @Test
    public void TransferScheduledPayment_CancelScheduledPaymentIdNotFound_Success() {
        final String invalidTransferId = TestHelper.generateRandomNumericStringWithNoLeadingZero(18);

        TransfersService.cancelScheduledTransferTransactions(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(invalidTransferId).cancellationReason("Cancellation reason").build()))
                        .build(), secretKey, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(invalidTransferId))
                .body("cancellations[0].errorCode", equalTo("NOT_FOUND"))
                .body("cancellations[0].state", equalTo("ERROR"));
    }
}
