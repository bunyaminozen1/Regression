package opc.junit.multi.owt.scheduledpayments;

import commons.enums.State;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.enums.opc.ResourceType;
import opc.helpers.OwtModelHelper;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransferResponseModel;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersResponseModel;
import opc.models.multi.outgoingwiretransfers.CancelScheduledModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.CancellationModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.SCHEDULED_OWT)
public abstract class AbstractScheduledPaymentsTests extends BaseOutgoingWireTransfersSetup {
    protected abstract String getToken();

    protected abstract String getIdentityId();

    protected abstract String getCurrency();

    protected abstract String getManagedAccountProfileId();

    protected abstract String getDestinationIdentityName();

    protected abstract IdentityType getIdentityType();

    /**
     * Test cases for OWTs scheduled payments
     * Documentation:<a href="https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673">...</a>
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
    public void OwtScheduledPayment_CreateValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.SMS);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);
    }

    @Test
    public void OwtScheduledPayment_CreateValidBeneficiaryScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        // Create beneficiary in ACTIVE state
        final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
                BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
                passcodeAppSecretKey, getToken()).getRight();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30))
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("challengeExemptionReason", equalTo("TRUSTED_BENEFICIARY"))
                .body("destination.beneficiaryId", equalTo(beneficiaryId))
                .body("sourceInstrument.id", equalTo(sourceManagedAccountId))
                .body("sourceInstrument.type", equalTo(MANAGED_ACCOUNTS.name().toLowerCase()))
                .body("profileId", equalTo(outgoingWireTransfersModel.getProfileId()))
                .body("creationTimestamp", notNullValue())
                .body("scheduledTimestamp", equalTo(outgoingWireTransfersModel.getScheduledTimestamp()))
                .body("tag", equalTo(outgoingWireTransfersModel.getTag()))
                .body("description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("state", equalTo("SCHEDULED"))
                .body("type", equalTo("SEPA"))
                .body("transferAmount.amount", equalTo(100))
                .body("transferAmount.currency", equalTo(getCurrency()));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc",})
    public void OwtScheduledPayment_InvalidTimestamp_BadRequest(final String timestamp) {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        OutgoingWireTransfersService.sendOutgoingWireTransfer(OwtModelHelper.createOwtScheduledPayment(timestamp,
                        outgoingWireTransfersProfileId, sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.scheduledTimestamp: must match \"^[0-9]+$\""));
    }

    @Test
    public void OwtScheduledPayment_TimestampInPast_Conflict() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        OutgoingWireTransfersService.sendOutgoingWireTransfer(OwtModelHelper.createOwtScheduledPayment(TestHelper.generateTimestampBefore(30),
                        outgoingWireTransfersProfileId, sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SCHEDULED_TIMESTAMP_INVALID"));
    }

    @Test
    public void OwtScheduledPayment_CreateAndExecuteValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.AUTHY);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        SimulatorService.scheduledExecuteOwt(passcodeApp.getInnovatorId(), owt)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("COMPLETED"));

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.COMPLETED);
    }

    @Test
    public void OwtScheduledPayment_CreateAndExecuteValidScheduledPaymentNoFunds_InsufficientFunds() throws SQLException {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.BIOMETRIC);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        SimulatorService.scheduledExecuteOwt(passcodeApp.getInnovatorId(), owt)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("ERROR"));

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.FAILED);
        final String validationFailure = OwtDatabaseHelper.getOwtById(owt).get(0).get("validation_failure");
        Assertions.assertEquals("INSUFFICIENT_FUNDS", validationFailure);
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.SMS);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owt).cancellationReason("Cancellation reason").build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then().statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(owt))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, owt, getToken())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("CANCELLED"));
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPaymentNoCancellationReason_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.AUTHY);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owt).build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then().statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(owt))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, owt, getToken())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("CANCELLED"));
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPaymentCancellationReasonTooLong_BadRequest() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.BIOMETRIC);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owt).cancellationReason(RandomStringUtils.randomAlphabetic(51)).build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].cancellationReason: size must be between 0 and 50"));
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPaymentCancellationReasonInvalidCharacters_BadRequest() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.SMS);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owt).cancellationReason("ɥ ɦ ɧ ɨ ɩ ɪ ɫ ɬ ɭ ɮ").build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].cancellationReason: must match \"^[ a-zA-Z0-9_-]+$\""));
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPaymentInvalidState_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owt).build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(owt))
                .body("cancellations[0].errorCode", equalTo("TRANSACTION_NOT_SCHEDULED"))
                .body("cancellations[0].state", equalTo("ERROR"));
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPaymentMissingId_BadRequest() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(30), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.AUTHY);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().cancellationReason("Cancellation reason").build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.cancellations[0].id: must not be blank"));
    }

    @Test
    public void OwtScheduledPayment_CancelScheduledPaymentIdNotFound_Success() {
        final String invalidOwtId = TestHelper.generateRandomNumericStringWithNoLeadingZero(18);

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(invalidOwtId).cancellationReason("Cancellation reason").build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(invalidOwtId))
                .body("cancellations[0].errorCode", equalTo("NOT_FOUND"))
                .body("cancellations[0].state", equalTo("ERROR"));
    }

    @Test
    public void OwtScheduledPayment_CreateBulkValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final OutgoingWireTransfersModel scheduledModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                                sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("validOwt")
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30)).build();

        final List<String> owts = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
                .outgoingWireTransfers(List.of(scheduledModel, scheduledModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkOutgoingWireTransfersResponseModel.class)
                .getResponse().stream().map(BulkOutgoingWireTransferResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(owts, EnrolmentChannel.SMS);

        owts.forEach(owt -> OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED));
    }

    @Test
    public void OwtScheduledPayment_CreateBulkValidMixScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final OutgoingWireTransfersModel scheduledModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                                sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("scheduledOwt")
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30)).build();

        final OutgoingWireTransfersModel unscheduledModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                        sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("unscheduledOwt").build();

        final List<BulkOutgoingWireTransferResponseModel> bulkOwts = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
                .outgoingWireTransfers(List.of(scheduledModel, unscheduledModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkOutgoingWireTransfersResponseModel.class)
                .getResponse();

        final List<String> owts = bulkOwts.stream().map(BulkOutgoingWireTransferResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(owts, EnrolmentChannel.AUTHY);

        final String scheduledOwtId = getOwtIdViaTag(bulkOwts, "scheduledOwt");
        final String unscheduledOwtId = getOwtIdViaTag(bulkOwts, "unscheduledOwt");

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, scheduledOwtId, getToken(), State.SCHEDULED);

        OutgoingWireTransfersHelper.checkOwtStateById(unscheduledOwtId, State.COMPLETED.name());
    }

    @Test
    public void OwtScheduledPayment_CancelBulkValidScheduledPayment_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

        final OutgoingWireTransfersModel scheduledModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                                sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("validOwt")
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30)).build();

        final List<String> owts = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
                .outgoingWireTransfers(List.of(scheduledModel, scheduledModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkOutgoingWireTransfersResponseModel.class)
                .getResponse().stream().map(BulkOutgoingWireTransferResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(owts, EnrolmentChannel.BIOMETRIC);

        owts.forEach(owt -> OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED));

        OutgoingWireTransfersService.cancelScheduledOutgoingWireTransfer(CancelScheduledModel.builder()
                        .cancellations(List.of(CancellationModel.builder().id(owts.get(0)).build(), CancellationModel.builder().id(owts.get(1)).build()))
                        .build(), passcodeAppSecretKey, getToken())
                .then().statusCode(SC_OK)
                .body("cancellations[0].state", equalTo("SUCCESS"))
                .body("cancellations[1].state", equalTo("SUCCESS"));

        owts.forEach(owt -> OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.CANCELLED));
    }

    @Test
    public void OwtScheduledPayment_InsufficientFunds_Success() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OwtModelHelper.createOwtScheduledPayment(TestHelper.generateTimestampAfter(30),
                        passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA);

        final String owtId =
                OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath().getString("id");

        OutgoingWireTransfersHelper.checkOwtStateById(owtId, State.REQUIRES_SCA.name());
    }

    @Test
    public void OwtScheduledPayment_InsufficientFunds_Failed() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        final String owt = OutgoingWireTransfersHelper.sendScheduledOwt(TestHelper.generateTimestampAfter(TimeUnit.SECONDS, 20), passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId, getCurrency(), 100L, passcodeAppSecretKey, getToken());

        issueAndVerifyScaChallenge(owt, EnrolmentChannel.SMS);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.SCHEDULED);

        OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.FAILED);
        OutgoingWireTransfersHelper.checkOwtValidationFailureById(owt, "INSUFFICIENT_FUNDS");
    }

    @Test
    public void OwtScheduledPayment_BulkInsufficientFunds_Failed() {
        final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
                getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

        final OutgoingWireTransfersModel scheduledModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                                sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("validOwt")
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(TimeUnit.SECONDS, 20)).build();

        final List<String> owts = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
                        .outgoingWireTransfers(List.of(scheduledModel, scheduledModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkOutgoingWireTransfersResponseModel.class)
                .getResponse().stream().map(BulkOutgoingWireTransferResponseModel::getId).collect(Collectors.toList());

        issueAndVerifyScaChallenge(owts, EnrolmentChannel.SMS);

        owts.forEach(owt -> {
            OutgoingWireTransfersHelper.ensureOwtState(passcodeAppSecretKey, owt, getToken(), State.FAILED);
            OutgoingWireTransfersHelper.checkOwtValidationFailureById(owt, "INSUFFICIENT_FUNDS");
        });
    }

    private void issueAndVerifyScaChallenge(final String owt,
                                            final EnrolmentChannel enrolmentChannel) {
        if (enrolmentChannel.equals(EnrolmentChannel.SMS)) {
            OutgoingWireTransfersHelper.verifyOwtOtp(owt, passcodeAppSecretKey, getToken());
        } else {
            OutgoingWireTransfersHelper.verifyOwtPush(owt, enrolmentChannel, passcodeAppSecretKey, getToken());
        }
    }

    private void issueAndVerifyScaChallenge(final List<String> owts,
                                            final EnrolmentChannel enrolmentChannel) {
        if (enrolmentChannel.equals(EnrolmentChannel.SMS)) {
            ChallengesHelper.issueAndVerifyOtpChallenge(ResourceType.OUTGOING_WIRE_TRANSFERS, owts,
                    passcodeAppSecretKey, getToken());
        } else {
            ChallengesHelper.issueAndVerifyPushChallenge(ResourceType.OUTGOING_WIRE_TRANSFERS, enrolmentChannel, owts,
                    passcodeAppSecretKey, getToken());
        }
    }

    private String getOwtIdViaTag(final List<BulkOutgoingWireTransferResponseModel> bulkOwts,
                                        final String tag) {
        return bulkOwts.stream().filter(x -> x.getTag().equals(tag)).findFirst().orElseThrow().getId();
    }
}
