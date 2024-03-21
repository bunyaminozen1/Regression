package opc.junit.multi.webhooks;

import io.restassured.response.Response;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.WebhookType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.models.webhook.WebhookTrustedBeneficiaryBatchModel;
import opc.services.multi.BeneficiariesService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(MultiTags.WEBHOOKS_PARALLEL)
public abstract class AbstractBeneficiariesWebhooksTests extends BaseBeneficiariesWebhooksSetup {

    protected abstract String getToken();

    protected abstract String getIdentityId();

    protected abstract String getDestinationToken();

    protected abstract String getDestinationCurrency();

    protected abstract String getDestinationIdentityName();

    protected abstract String getDestinationPrepaidManagedCardProfileId();

    protected abstract String getDestinationDebitManagedCardProfileId();

    protected abstract String getDestinationManagedAccountProfileId();

    protected abstract IdentityType getIdentityType();

    @Test
    public void WebhookBeneficiaryBatch_PendingChallengeBeneficiaryInstrument_Success() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        final long timestamp = Instant.now().toEpochMilli();

        // Create beneficiary in PENDING_CHALLENGE state
        final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken());

        final WebhookTrustedBeneficiaryBatchModel event = getWebhookResponse(timestamp, batchAndBeneficiary.getLeft());

        assertBeneficiaryEvent(event, managedAccountId, "CREATE",
                batchAndBeneficiary.getLeft(), batchAndBeneficiary.getRight(), "TRUSTED", "PENDING_CHALLENGE",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "PENDING_CHALLENGE");
    }

    @Test
    public void WebhookBeneficiaryBatch_PendingChallengeSepaBankDetails_Success() {
        // Create beneficiary with valid SEPA bank details
        final Pair<String, String> sepaBankDetails = ModelHelper.generateRandomValidSEPABankDetails();

        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
                        getDestinationIdentityName(), List.of(sepaBankDetails)).build();

        final long timestamp = Instant.now().toEpochMilli();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        final String batchId = getBeneficiaryBatchId(beneficiariesBatchResponse);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiariesIdsByBatchId(batchId, secretKey, getToken()).get(0);

        final WebhookTrustedBeneficiaryBatchModel event = getWebhookResponse(timestamp, batchId);

        assertBeneficiaryEvent(event, sepaBankDetails, createBeneficiariesBatchModel.getTag(),
                batchId, beneficiaryId, "PENDING_CHALLENGE", "TRUSTED", "PENDING_CHALLENGE",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "CREATE", "SEPA");
    }

    @Test
    public void WebhookBeneficiaryBatch_PendingChallengeFasterPaymentsBankDetails_Success() {
        // Create beneficiary with valid Faster Payments bank details
        final Pair<String, String> fasterPaymentsBankDetails = ModelHelper.generateRandomValidFasterPaymentsBankDetails();

        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.FasterPaymentsBeneficiaryBatch(getIdentityType(),
                        getDestinationIdentityName(), List.of(fasterPaymentsBankDetails)).build();

        final long timestamp = Instant.now().toEpochMilli();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        final String batchId = getBeneficiaryBatchId(beneficiariesBatchResponse);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiariesIdsByBatchId(batchId, secretKey, getToken()).get(0);

        final WebhookTrustedBeneficiaryBatchModel event = getWebhookResponse(timestamp, batchId);

        assertBeneficiaryEvent(event, fasterPaymentsBankDetails, createBeneficiariesBatchModel.getTag(),
                batchId, beneficiaryId, "PENDING_CHALLENGE", "TRUSTED", "PENDING_CHALLENGE",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "CREATE", "FASTER_PAYMENTS");
    }

    @Test
    public void WebhookBeneficiaryBatch_InvalidBeneficiaryInstrument_Success() {
        // Create beneficiary with invalid managed account (does not exist)
        final String managedAccountId = RandomStringUtils.randomNumeric(18);
        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
                        ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

        final long timestamp = Instant.now().toEpochMilli();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
                getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        final String batchId = getBeneficiaryBatchId(beneficiariesBatchResponse);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiariesIdsByBatchId(batchId, secretKey, getToken()).get(0);

        final WebhookTrustedBeneficiaryBatchModel event = getWebhookResponse(timestamp, batchId);

        assertBeneficiaryEvent(event, managedAccountId, "CREATE",
                batchId, beneficiaryId, "TRUSTED", "INVALID",
                "INSTRUMENT_DETAILS_NOT_FOUND", "FAILED");
    }

    @Test
    public void WebhookBeneficiaryBatch_ActiveBeneficiaryInstrument_Success() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        final long timestamp = Instant.now().toEpochMilli();

        // Create beneficiary in ACTIVE state
        final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken());

        final List<WebhookTrustedBeneficiaryBatchModel> events = getWebhookResponses(timestamp, batchAndBeneficiary.getLeft(), 2);
        final WebhookTrustedBeneficiaryBatchModel pendingEvent =
                events.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookTrustedBeneficiaryBatchModel completedEvent =
                events.stream().filter(x -> x.getEventType().equals("CHALLENGE_COMPLETED")).collect(Collectors.toList()).get(0);

        assertBeneficiaryEvent(pendingEvent, managedAccountId, "CREATE",
                batchAndBeneficiary.getLeft(), batchAndBeneficiary.getRight(), "TRUSTED", "PENDING_CHALLENGE",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "PENDING_CHALLENGE");

        assertBeneficiaryEvent(completedEvent, managedAccountId, "CREATE",
                batchAndBeneficiary.getLeft(), batchAndBeneficiary.getRight(), "TRUSTED", "ACTIVE",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "CHALLENGE_COMPLETED");
    }

    @Test
    public void WebhookBeneficiaryBatch_RemoveBeneficiaryInstrument_Success() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        final long timestamp = Instant.now().toEpochMilli();

        // Create beneficiary in REMOVED state
        final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken());

        final List<WebhookTrustedBeneficiaryBatchModel> events = getWebhookResponses(timestamp, batchAndBeneficiary.getLeft(), 2);
        final WebhookTrustedBeneficiaryBatchModel pendingEvent =
                events.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookTrustedBeneficiaryBatchModel completedEvent =
                events.stream().filter(x -> x.getEventType().equals("CHALLENGE_COMPLETED")).collect(Collectors.toList()).get(0);

        assertBeneficiaryEvent(pendingEvent, managedAccountId, "REMOVE",
                batchAndBeneficiary.getLeft(), batchAndBeneficiary.getRight(), "TRUSTED", "ACTIVE",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "PENDING_CHALLENGE");

        assertBeneficiaryEvent(completedEvent, managedAccountId, "REMOVE",
                batchAndBeneficiary.getLeft(), batchAndBeneficiary.getRight(), "TRUSTED", "REMOVED",
                "BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", "CHALLENGE_COMPLETED");
    }

    @Test
    public void WebhookBeneficiaryBatch_MixOfValidAndInvalidBeneficiaries_Success() {
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

        final String managedCardId = RandomStringUtils.randomNumeric(18).replaceFirst("^0+(?!$)", "");
        final Pair<String, String> fasterPaymentsBankDetails = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final Pair<String, String> sepaBankDetails = ModelHelper.generateRandomValidSEPABankDetails();

        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.MultipleBeneficiaryBatch(getIdentityType(), ManagedInstrumentType.MANAGED_CARDS, getDestinationIdentityName(),
                        List.of(managedCardId),
                        List.of(sepaBankDetails),
                        List.of(fasterPaymentsBankDetails)).build();

        final long timestamp = Instant.now().toEpochMilli();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        final String batchId = getBeneficiaryBatchId(beneficiariesBatchResponse);

        BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchId, EnrolmentChannel.AUTHY.name(), secretKey, getToken());

        SimulatorHelper.acceptAuthyBeneficiaryBatch(secretKey, batchId);

        final List<WebhookTrustedBeneficiaryBatchModel> events = getWebhookResponses(timestamp, batchId, 2);
        final WebhookTrustedBeneficiaryBatchModel pendingEvent =
                events.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookTrustedBeneficiaryBatchModel completedEvent =
                events.stream().filter(x -> x.getEventType().equals("CHALLENGE_COMPLETED")).collect(Collectors.toList()).get(0);

        assertEquals(3, pendingEvent.getBeneficiaries().size());
        assertEquals("CREATE", pendingEvent.getOperation());
        assertEquals(batchId, pendingEvent.getId());
        assertEquals(getDestinationIdentityName(),
                getIdentityType() == IdentityType.CORPORATE ? pendingEvent.getBeneficiaries()
                        .get(0).getBeneficiaryInformation().getBusinessBeneficiaryType().getBusinessName()
                        : pendingEvent.getBeneficiaries().get(0).getBeneficiaryInformation()
                        .getConsumerBeneficiaryType().getFullName());
        assertEquals("PENDING_CHALLENGE", pendingEvent.getEventType());
        assertEquals("TRUSTED", pendingEvent.getBeneficiaries().get(0).getTrustLevel());

        assertEquals(managedCardId, pendingEvent.getBeneficiaries().get(0)
                .getBeneficiaryDetails().getInstrumentDetailsBeneficiary().getInstrument().getId());
        assertEquals("managed_cards", pendingEvent.getBeneficiaries().get(0)
                .getBeneficiaryDetails().getInstrumentDetailsBeneficiary().getInstrument().getType());
        assertEquals("INVALID", pendingEvent.getBeneficiaries().get(0).getState());
        assertEquals("INSTRUMENT_DETAILS_NOT_FOUND", pendingEvent.getBeneficiaries()
                .get(0).getValidationFailure());

        assertEquals(sepaBankDetails.getLeft(), pendingEvent.getBeneficiaries().get(1)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getSepaBankDetails().getIban());
        assertEquals(sepaBankDetails.getRight(), pendingEvent.getBeneficiaries().get(1)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getSepaBankDetails().getBankIdentifierCode());
        assertEquals("PENDING_CHALLENGE", pendingEvent.getBeneficiaries().get(1).getState());
        assertEquals("BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", pendingEvent.getBeneficiaries()
                .get(1).getValidationFailure());

        assertEquals(fasterPaymentsBankDetails.getLeft(), pendingEvent.getBeneficiaries().get(2)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getFasterPaymentsBankDetails().getAccountNumber());
        assertEquals(fasterPaymentsBankDetails.getRight(), pendingEvent.getBeneficiaries().get(2)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getFasterPaymentsBankDetails().getSortCode());
        assertEquals("PENDING_CHALLENGE", pendingEvent.getBeneficiaries().get(2).getState());
        assertEquals("BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", pendingEvent.getBeneficiaries()
                .get(2).getValidationFailure());

        assertEquals(2, completedEvent.getBeneficiaries().size());
        assertEquals("CHALLENGE_COMPLETED", completedEvent.getEventType());
        assertEquals(batchId, completedEvent.getId());

        assertEquals("ACTIVE", completedEvent.getBeneficiaries().get(0).getState());
        assertEquals("ACTIVE", completedEvent.getBeneficiaries().get(1).getState());
    }

    @Test
    public void WebhookBeneficiaryBatch_RejectChallengeOnMultipleBeneficiaries_Success() {
        SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKey, secretKey, getToken());

        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();
        final Pair<String, String> fasterPaymentsBankDetails = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final Pair<String, String> sepaBankDetails = ModelHelper.generateRandomValidSEPABankDetails();

        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.MultipleBeneficiaryBatch(getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(),
                        List.of(managedAccountId),
                        List.of(sepaBankDetails),
                        List.of(fasterPaymentsBankDetails)).build();

        final long timestamp = Instant.now().toEpochMilli();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        final String batchId = getBeneficiaryBatchId(beneficiariesBatchResponse);

        BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchId, EnrolmentChannel.BIOMETRIC.name(), secretKey, getToken());

        SimulatorHelper.rejectOkayBeneficiaryBatch(secretKey, batchId);

        final List<WebhookTrustedBeneficiaryBatchModel> events = getWebhookResponses(timestamp, batchId, 2);
        final WebhookTrustedBeneficiaryBatchModel pendingChallengeBeneficiariesBatchEvent =
                events.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookTrustedBeneficiaryBatchModel completedChallengeBeneficiariesBatchEvent =
                events.stream().filter(x -> x.getEventType().equals("CHALLENGE_FAILED")).collect(Collectors.toList()).get(0);

        assertEquals(3, pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().size());
        assertEquals("CREATE", pendingChallengeBeneficiariesBatchEvent.getOperation());
        assertEquals(batchId, pendingChallengeBeneficiariesBatchEvent.getId());
        assertEquals(getDestinationIdentityName(),
                getIdentityType() == IdentityType.CORPORATE ? pendingChallengeBeneficiariesBatchEvent.getBeneficiaries()
                        .get(0).getBeneficiaryInformation().getBusinessBeneficiaryType().getBusinessName()
                        : pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(0).getBeneficiaryInformation()
                        .getConsumerBeneficiaryType().getFullName());
        assertEquals("PENDING_CHALLENGE", pendingChallengeBeneficiariesBatchEvent.getEventType());
        assertEquals("TRUSTED", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(0).getTrustLevel());

        assertEquals(managedAccountId, pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(0)
                .getBeneficiaryDetails().getInstrumentDetailsBeneficiary().getInstrument().getId());
        assertEquals("managed_accounts", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(0)
                .getBeneficiaryDetails().getInstrumentDetailsBeneficiary().getInstrument().getType());
        assertEquals("PENDING_CHALLENGE", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(0).getState());
        assertEquals("BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries()
                .get(0).getValidationFailure());

        assertEquals(sepaBankDetails.getLeft(), pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(1)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getSepaBankDetails().getIban());
        assertEquals(sepaBankDetails.getRight(), pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(1)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getSepaBankDetails().getBankIdentifierCode());
        assertEquals("PENDING_CHALLENGE", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(1).getState());
        assertEquals("BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries()
                .get(1).getValidationFailure());

        assertEquals(fasterPaymentsBankDetails.getLeft(), pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(2)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getFasterPaymentsBankDetails().getAccountNumber());
        assertEquals(fasterPaymentsBankDetails.getRight(), pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(2)
                .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getFasterPaymentsBankDetails().getSortCode());
        assertEquals("PENDING_CHALLENGE", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries().get(2).getState());
        assertEquals("BENEFICIARY_VALIDATION_FAILURE_UNKNOWN", pendingChallengeBeneficiariesBatchEvent.getBeneficiaries()
                .get(2).getValidationFailure());

        assertEquals(3, completedChallengeBeneficiariesBatchEvent.getBeneficiaries().size());
        assertEquals("CHALLENGE_FAILED", completedChallengeBeneficiariesBatchEvent.getEventType());
        assertEquals(batchId, completedChallengeBeneficiariesBatchEvent.getId());

        assertEquals("CHALLENGE_FAILED", completedChallengeBeneficiariesBatchEvent.getBeneficiaries().get(0).getState());
        assertEquals("CHALLENGE_FAILED", completedChallengeBeneficiariesBatchEvent.getBeneficiaries().get(1).getState());
        assertEquals("CHALLENGE_FAILED", completedChallengeBeneficiariesBatchEvent.getBeneficiaries().get(2).getState());
    }

    //common checks
    private void assertBeneficiaryEvent(final WebhookTrustedBeneficiaryBatchModel beneficiariesBatchEvent,
                                        final String instrumentId,
                                        final String operation,
                                        final String batchId,
                                        final String beneficiaryId,
                                        final String trustLevel,
                                        final String beneficiariesState,
                                        final String validationFailure,
                                        final String eventType) {

        assertEquals(instrumentId, beneficiariesBatchEvent.getBeneficiaries().get(0)
                .getBeneficiaryDetails().getInstrumentDetailsBeneficiary().getInstrument().getId());
        assertEquals("managed_accounts", beneficiariesBatchEvent.getBeneficiaries().get(0)
                .getBeneficiaryDetails().getInstrumentDetailsBeneficiary().getInstrument().getType());
        assertEquals(operation, beneficiariesBatchEvent.getOperation());
        assertEquals(batchId, beneficiariesBatchEvent.getId());
        assertEquals(beneficiaryId, beneficiariesBatchEvent.getBeneficiaries().get(0).getId());
        assertEquals(getDestinationIdentityName(),
                getIdentityType() == IdentityType.CORPORATE ? beneficiariesBatchEvent.getBeneficiaries().get(0)
                        .getBeneficiaryInformation().getBusinessBeneficiaryType().getBusinessName()
                        : beneficiariesBatchEvent.getBeneficiaries().get(0).getBeneficiaryInformation()
                        .getConsumerBeneficiaryType().getFullName());
        assertEquals(eventType, beneficiariesBatchEvent.getEventType());
        assertEquals(trustLevel, beneficiariesBatchEvent.getBeneficiaries().get(0).getTrustLevel());
        assertEquals(beneficiariesState, beneficiariesBatchEvent.getBeneficiaries().get(0).getState());
        assertEquals(validationFailure, beneficiariesBatchEvent.getBeneficiaries().get(0)
                .getValidationFailure());
        assertNotNull(beneficiariesBatchEvent.getPublishedTimestamp());
    }

    //    Sepa + FasterPayments
    private void assertBeneficiaryEvent(final WebhookTrustedBeneficiaryBatchModel beneficiariesBatchEvent,
                                        final Pair<String, String> bankDetails,
                                        final String tag,
                                        final String batchId,
                                        final String beneficiaryId,
                                        final String eventType,
                                        final String trustLevel,
                                        final String beneficiariesState,
                                        final String validationFailure,
                                        final String operation,
                                        final String payment) {

        assertEquals(tag, beneficiariesBatchEvent.getTag());
        assertEquals(operation, beneficiariesBatchEvent.getOperation());
        assertEquals(batchId, beneficiariesBatchEvent.getId());
        assertEquals(beneficiaryId, beneficiariesBatchEvent.getBeneficiaries().get(0).getId());
        assertEquals(getDestinationIdentityName(),
                getIdentityType() == IdentityType.CORPORATE ? beneficiariesBatchEvent.getBeneficiaries()
                        .get(0).getBeneficiaryInformation().getBusinessBeneficiaryType().getBusinessName()
                        : beneficiariesBatchEvent.getBeneficiaries().get(0).getBeneficiaryInformation()
                        .getConsumerBeneficiaryType().getFullName());
        assertEquals(eventType, beneficiariesBatchEvent.getEventType());
        assertEquals(trustLevel, beneficiariesBatchEvent.getBeneficiaries().get(0).getTrustLevel());
        assertEquals(beneficiariesState, beneficiariesBatchEvent.getBeneficiaries().get(0).getState());
        assertEquals(validationFailure, beneficiariesBatchEvent.getBeneficiaries()
                .get(0).getValidationFailure());
        assertNotNull(beneficiariesBatchEvent.getPublishedTimestamp());

        if (payment.equals("SEPA")) {
            assertEquals(bankDetails.getLeft(), beneficiariesBatchEvent.getBeneficiaries().get(0)
                    .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getSepaBankDetails().getIban());
            assertEquals(bankDetails.getRight(), beneficiariesBatchEvent.getBeneficiaries().get(0)
                    .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getSepaBankDetails().getBankIdentifierCode());
        } else {
            assertEquals(bankDetails.getLeft(), beneficiariesBatchEvent.getBeneficiaries().get(0)
                    .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getFasterPaymentsBankDetails().getAccountNumber());
            assertEquals(bankDetails.getRight(), beneficiariesBatchEvent.getBeneficiaries().get(0)
                    .getBeneficiaryDetails().getBankAccountDetailsBeneficiary().getFasterPaymentsBankDetails().getSortCode());
        }
    }

    private WebhookTrustedBeneficiaryBatchModel getWebhookResponse(final long timestamp,
                                                                   final String batchId) {
        return (WebhookTrustedBeneficiaryBatchModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.BENEFICIARIES_BATCH,
                Pair.of("id", batchId),
                WebhookTrustedBeneficiaryBatchModel.class,
                ApiSchemaDefinition.BeneficiariesBatchEvent);
    }

    private List<WebhookTrustedBeneficiaryBatchModel> getWebhookResponses(final long timestamp,
                                                                          final String batchId,
                                                                          final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.BENEFICIARIES_BATCH,
                Pair.of("id", batchId),
                WebhookTrustedBeneficiaryBatchModel.class,
                ApiSchemaDefinition.BeneficiariesBatchEvent,
                expectedEventCount);
    }
}