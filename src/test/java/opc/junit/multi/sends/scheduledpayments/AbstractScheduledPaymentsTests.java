package opc.junit.multi.sends.scheduledpayments;

import commons.enums.State;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.ResourceType;
import opc.helpers.SendModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.multi.sends.BaseSendsSetup;
import opc.models.multi.sends.BulkSendFundsModel;
import opc.models.multi.sends.BulkSendResponseModel;
import opc.models.multi.sends.BulkSendsResponseModel;
import opc.models.multi.sends.CancelScheduledModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CancellationModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.SCHEDULED_SENDS)
public abstract class AbstractScheduledPaymentsTests extends BaseSendsSetup {
    protected abstract String getToken();

    protected abstract String getCurrency();

    protected abstract String getPrepaidManagedCardProfileId();

    protected abstract String getManagedAccountProfileId();

    protected abstract String getDestinationToken();

    protected abstract String getDestinationCurrency();

    protected abstract String getDestinationIdentityName();

    protected abstract IdentityType getIdentityType();

    /**
     * Test cases for SENDs scheduled payments
     * Documentation: <a href="https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673">...</a>
     * <p>
     * Main ticket: DEV-5202
     * <p>
     * The main cases:
     * 1. Create Single Scheduled Payment
     * 2. Create Single Beneficiary Scheduled Payment
     * 3. Invalid Timestamps
     * 4. Bulk Scheduled Payments
     * 5. Cancel Scheduled Payments
     * 6. Cancel Bulk Scheduled Payments
     * 7. Conflicts and bad requests
     */

    @Test
    public void SendScheduledPayment_CreateValidMaToMaScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.SMS);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);
    }

    @Test
    public void SendScheduledPayment_CreateValidMaToMcScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_CARDS,
                destinationManagedCardId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.AUTHY);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);
    }

    @Test
    public void SendScheduledPayment_CreateValidMcToMaScheduledPayment_Success() {
        final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                MANAGED_CARDS, sourceManagedCardId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.BIOMETRIC);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);
    }

    @Test
    public void SendScheduledPayment_CreateValidMcToMcScheduledPayment_Success() {
        final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30),
                sendsProfileIdScaSendsApp, MANAGED_CARDS, sourceManagedCardId, MANAGED_CARDS,
                destinationManagedCardId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.SMS);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);
    }

    @Test
    public void SendScheduledPayment_CreateValidBeneficiaryScheduledPayment_Success() {
        // Create destination managed account to be used as beneficiary instrument
        final String beneficiaryManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        // Create beneficiary in ACTIVE state
        final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                        BeneficiaryState.ACTIVE, getIdentityType(), MANAGED_ACCOUNTS,
                        getDestinationIdentityName(), beneficiaryManagedAccountId, secretKeyScaSendsApp, getToken())
                .getRight();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30))
                        .build();

        // TX is SCA Exempted, in SCHEDULED state
        SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("challengeExemptionReason", equalTo("TRUSTED_BENEFICIARY"))
                .body("destination.beneficiaryId", equalTo(beneficiaryId))
                .body("source.id", equalTo(sourceManagedAccountId))
                .body("source.type", equalTo("managed_accounts"))
                .body("profileId", equalTo(sendFundsModel.getProfileId()))
                .body("creationTimestamp", notNullValue())
                .body("scheduledTimestamp", equalTo(sendFundsModel.getScheduledTimestamp()))
                .body("tag", equalTo(sendFundsModel.getTag()))
                .body("destinationAmount.amount", equalTo(100))
                .body("destinationAmount.currency", equalTo(getCurrency()))
                .body("state", equalTo("SCHEDULED"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc",})
    public void SendScheduledPayment_InvalidTimestamp_BadRequest(final String timestamp) {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        SendsService.sendFunds(SendModelHelper.createSendScheduledPayment(timestamp,
                        sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId,
                        getCurrency(), 100L), secretKeyScaSendsApp, getDestinationToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.scheduledTimestamp: must match \"^[0-9]+$\""));
    }

    @Test
    public void SendScheduledPayment_TimestampInPast_Conflict() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        SendsService.sendFunds(SendModelHelper.createSendScheduledPayment(TestHelper.generateTimestampBefore(30),
                        sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId,
                        getCurrency(), 100L), secretKeyScaSendsApp, getDestinationToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SCHEDULED_TIMESTAMP_INVALID"));
    }

    @Test
    public void SendScheduledPayment_CreateAndExecuteValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.AUTHY);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SimulatorService.scheduledExecuteSends(scaSendsApp.getInnovatorId(), send)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("COMPLETED"));

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.COMPLETED);
    }

    @Test
    @DisplayName("SendScheduledPayment_CreateAndExecuteValidScheduledPaymentNoFunds_InsufficientFunds - will be fixed by DEV-5679 - uncomment last 2 lines")
    public void SendScheduledPayment_CreateAndExecuteValidScheduledPaymentNoFunds_InsufficientFunds() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.BIOMETRIC);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SimulatorService.scheduledExecuteSends(scaSendsApp.getInnovatorId(), send)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("ERROR"));

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.FAILED);

//        final String validationFailure = SendsDatabaseHelper.getSendById(send).get(0).get("validation_failure");
//        Assertions.assertEquals("INSUFFICIENT_FUNDS", validationFailure);
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.SMS);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(send).cancellationReason("Cancellation reason").build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then().statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(send))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.CANCELLED);
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPaymentNoCancellationReason_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30), sendsProfileIdScaSendsApp,
                ManagedInstrumentType.MANAGED_ACCOUNTS, sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.AUTHY);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(send).build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then().statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(send))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.CANCELLED);
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPaymentCancellationReasonTooLong_BadRequest() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30),
                sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.BIOMETRIC);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(send)
                                .cancellationReason(RandomStringUtils.randomAlphabetic(51)).build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].cancellationReason: size must be between 0 and 50"));
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPaymentCancellationReasonInvalidCharacters_BadRequest() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30),
                sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.SMS);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(send)
                                .cancellationReason("ɥ ɦ ɧ ɨ ɩ ɪ ɫ ɬ ɭ ɮ").build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].cancellationReason: must match \"^[ a-zA-Z0-9_-]+$\""));
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPaymentInvalidState_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30),
                sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(send).build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(send))
                .body("cancellations[0].errorCode", equalTo("TRANSACTION_NOT_SCHEDULED"))
                .body("cancellations[0].state", equalTo("ERROR"));
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPaymentMissingId_BadRequest() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String send = SendsHelper.sendScheduledSend(TestHelper.generateTimestampAfter(30),
                sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS,
                destinationManagedAccountId, getCurrency(), 100L, secretKeyScaSendsApp, getToken());

        issueAndVerifyScaChallenge(send, EnrolmentChannel.AUTHY);

        SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED);

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().cancellationReason("Cancellation reason").build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].id: must not be blank"));
    }

    @Test
    public void SendScheduledPayment_CancelScheduledPaymentIdNotFound_Success() {
        final String invalidSendId = TestHelper.generateRandomNumericStringWithNoLeadingZero(18);

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(invalidSendId).cancellationReason("Cancellation reason").build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(invalidSendId))
                .body("cancellations[0].errorCode", equalTo("NOT_FOUND"))
                .body("cancellations[0].state", equalTo("ERROR"));
    }

    @Test
    public void SendScheduledPayment_CreateBulkValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final SendFundsModel scheduledModel = SendFundsModel.DefaultSendsModel(sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId, getCurrency(), 100L
        ).setScheduledTimestamp(TestHelper.generateTimestampAfter(30)).build();

        final List<String> sends = SendsService.bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(scheduledModel, scheduledModel)).build(),
                        secretKeyScaSendsApp, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkSendsResponseModel.class)
                .getResponse().stream().map(BulkSendResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(sends, EnrolmentChannel.SMS);

        sends.forEach(send -> SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED));
    }

    @Test
    public void SendScheduledPayment_CreateBulkValidMixScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final SendFundsModel scheduledModel = SendFundsModel.DefaultSendsModel(sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId, getCurrency(),
                100L).setScheduledTimestamp(TestHelper.generateTimestampAfter(30)).setTag("scheduledSend").build();

        final SendFundsModel unscheduledModel = SendFundsModel.DefaultSendsModel(sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId,
                getCurrency(), 100L).setTag("unscheduledSend").build();

        final List<BulkSendResponseModel> bulkSend = SendsService.bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(scheduledModel, unscheduledModel)).build(),
                secretKeyScaSendsApp, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkSendsResponseModel.class).getResponse();

        final List<String> sends = bulkSend.stream().map(BulkSendResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(sends, EnrolmentChannel.AUTHY);

        final String scheduledSendId = getSendIdViaTag(bulkSend, "scheduledSend");
        final String unscheduledSendId = getSendIdViaTag(bulkSend, "unscheduledSend");

        SendsHelper.ensureSendState(secretKeyScaSendsApp, scheduledSendId, getToken(), State.SCHEDULED);
        SendsHelper.ensureSendState(secretKeyScaSendsApp, unscheduledSendId, getToken(), State.COMPLETED);
    }

    @Test
    public void SendScheduledPayment_CancelBulkValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

        final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final SendFundsModel scheduledModel = SendFundsModel.DefaultSendsModel(sendsProfileIdScaSendsApp,
                MANAGED_ACCOUNTS, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId, getCurrency(), 100L
        ).setScheduledTimestamp(TestHelper.generateTimestampAfter(30)).build();

        final List<String> sends = SendsService.bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(scheduledModel, scheduledModel)).build(),
                secretKeyScaSendsApp, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkSendsResponseModel.class)
                .getResponse().stream().map(BulkSendResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(sends, EnrolmentChannel.BIOMETRIC);

        sends.forEach(send -> SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.SCHEDULED));

        SendsService.cancelScheduledSend(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(sends.get(0)).build(), CancellationModel.builder().id(sends.get(1)).build()))
                        .build(), secretKeyScaSendsApp, getToken())
                .then().statusCode(SC_OK)
                .body("cancellations[0].state", equalTo("SUCCESS"))
                .body("cancellations[1].state", equalTo("SUCCESS"));

        sends.forEach(send -> SendsHelper.ensureSendState(secretKeyScaSendsApp, send, getToken(), State.CANCELLED));
    }

    private void issueAndVerifyScaChallenge(final String send,
                                            final EnrolmentChannel enrolmentChannel) {
        if (enrolmentChannel.equals(EnrolmentChannel.SMS)) {
            SendsHelper.verifySendOtp(send, secretKeyScaSendsApp, getToken());
        } else {
            SendsHelper.issueAndVerifyPushChallenge(send, enrolmentChannel, secretKeyScaSendsApp, getToken());
        }
    }

    private void issueAndVerifyScaChallenge(final List<String> sends,
                                            final EnrolmentChannel enrolmentChannel) {
        if (enrolmentChannel.equals(EnrolmentChannel.SMS)) {
            ChallengesHelper.issueAndVerifyOtpChallenge(ResourceType.SENDS, sends,
                    secretKeyScaSendsApp, getToken());
        } else {
            ChallengesHelper.issueAndVerifyPushChallenge(ResourceType.SENDS, enrolmentChannel, sends,
                    secretKeyScaSendsApp, getToken());
        }
    }

    private String getSendIdViaTag(final List<BulkSendResponseModel> bulkSend,
                                   final String tag) {
        return bulkSend.stream().filter(x -> x.getTag().equals(tag)).findFirst().orElseThrow().getId();
    }
}
