package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.models.multi.beneficiaries.RemoveBeneficiariesModel;
import opc.services.multi.BeneficiariesService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public abstract class AbstractVerifyBeneficiaryBatchViaOkayTests extends BaseBeneficiariesSetup {
  protected abstract String getToken();
  protected abstract String getIdentityId();
  protected abstract String getDestinationToken();
  protected abstract String getDestinationCurrency();
  protected abstract String getDestinationIdentityName();
  protected abstract String getDestinationPrepaidManagedCardProfileId();
  protected abstract String getDestinationDebitManagedCardProfileId();
  protected abstract String getDestinationManagedAccountProfileId();
  protected abstract IdentityType getIdentityType();
  private static final String BIOMETRIC_CHANNEL = EnrolmentChannel.BIOMETRIC.name();

  /**
   * Test cases for Creating/Adding Trusted Beneficiaries
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
   * Test Plan: TBA
   * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
   *
   * The main cases:
   * 1. Accept/Reject Biometric challenge for Instruments (Managed Accounts, Managed Cards)
   * 2. Accept/Reject Biometric challenge for Bank Details (SEPA and Faster Payments)
   * 3. conflicts based on determined logic
   */

  @Test
  public void VerifyBatchViaOkay_AcceptChallengeForValidBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(),
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft())
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
        batchAndBeneficiary.getLeft(), secretKey, getToken());
    BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE,
        batchAndBeneficiary.getRight(), secretKey, getToken());
  }

  @Test
  public void VerifyBatchViaOkay_RejectChallengeForValidBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(),
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.rejectOkayBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft())
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.CHALLENGE_FAILED,
        batchAndBeneficiary.getLeft(), secretKey, getToken());
    BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.CHALLENGE_FAILED,
        batchAndBeneficiary.getRight(), secretKey, getToken());
  }

  @Test
  public void VerifyBatchViaOkay_AcceptChallengeForMultipleValidBeneficiaries_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final List<String> managedAccountIds = createManagedAccounts(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 3);

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), managedAccountIds).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(getBeneficiaryBatchId(beneficiariesBatchResponse),
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, getBeneficiaryBatchId(beneficiariesBatchResponse))
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
        getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());
    final List<String> beneficiaries = BeneficiariesHelper
        .getBeneficiariesIdsByBatchId(getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());
    beneficiaries.forEach( beneficiary ->
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken()));
  }

  @Test
  public void VerifyBatchViaOkay_VerifyMultipleValidAndInvalidBeneficiaries_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String validManagedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();
    final String invalidManagedAccountId = RandomStringUtils.randomNumeric(18);

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(),
            List.of(
                validManagedAccountId,
                invalidManagedAccountId
            )).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(getBeneficiaryBatchId(beneficiariesBatchResponse),
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, getBeneficiaryBatchId(beneficiariesBatchResponse))
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());
    BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), getToken())
        .then()
        .statusCode(SC_OK)
        .body("beneficiaries[0].state", equalTo(BeneficiaryState.INVALID.name()))
        .body("beneficiaries[1].state", equalTo(BeneficiaryState.ACTIVE.name()));
  }

  @Test
  public void VerifyBatchViaOkay_AcceptChallengeForNonExistentBatch_NotFound() {
    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }

  @Test
  public void VerifyBatchViaOkay_RejectChallengeForNonExistentBatch_NotFound() {
    SimulatorService.rejectOkayBeneficiaryBatch(secretKey, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }

  @Test
  public void VerifyBatchViaOkay_ChallengeAlreadyCompleted_Conflict() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(),
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("ALREADY_COMPLETED"));

    SimulatorService.rejectOkayBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("ALREADY_COMPLETED"));
  }

  @Test
  public void VerifyBatchViaOkay_AcceptChallengeForRemoveBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    final String removeBatchId = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
            .Remove(List.of(batchAndBeneficiary.getRight())).build(), secretKey, getToken())
        .jsonPath().get("operationBatchId.batchId");

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(removeBatchId,
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.acceptOkayBeneficiaryBatch(secretKey, removeBatchId)
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
        removeBatchId, secretKey, getToken());
  }

  @Test
  public void VerifyBatchViaOkay_RejectChallengeForRemoveBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    final String removeBatchId = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
            .Remove(List.of(batchAndBeneficiary.getRight())).build(), secretKey, getToken())
        .jsonPath().get("operationBatchId.batchId");

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(removeBatchId,
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.rejectOkayBeneficiaryBatch(secretKey, removeBatchId)
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.CHALLENGE_FAILED,
        removeBatchId, secretKey, getToken());
  }

  @Test
  public void VerifyBatchViaOkay_AcceptChallengeViaAuthy_Conflict() {
    //Enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(),
        BIOMETRIC_CHANNEL, secretKey, getToken());

    SimulatorService.acceptAuthyBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("NOT_FOUND"));
  }

  @Test
  public void VerifyBatchViaOkay_NoApiKey_BadRequest() {
    SimulatorService.acceptOkayBeneficiaryBatch("", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_BAD_REQUEST);

    SimulatorService.rejectOkayBeneficiaryBatch("", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void VerifyBatchViaOkay_InvalidApiKey_Unauthorised() {
    SimulatorService.acceptOkayBeneficiaryBatch("abc", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_NOT_FOUND);

    SimulatorService.rejectOkayBeneficiaryBatch("abc", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_NOT_FOUND);
  }
}