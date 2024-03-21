package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.RemoveBeneficiariesModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public abstract class AbstractGetBeneficiaryBatchTests extends BaseBeneficiariesSetup {
  protected abstract String getToken();
  protected abstract String getIdentityId();
  protected abstract String getDestinationToken();
  protected abstract String getDestinationCurrency();
  protected abstract String getDestinationIdentityName();
  protected abstract String getDestinationPrepaidManagedCardProfileId();
  protected abstract String getDestinationDebitManagedCardProfileId();
  protected abstract String getDestinationManagedAccountProfileId();
  protected abstract IdentityType getIdentityType();

  /**
   * Test cases for Creating/Adding Trusted Beneficiaries
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
   * Test Plan: TBA
   * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
   *
   * The main cases:
   * 1. Get beneficiary batch by ID for Instruments (Managed Accounts, Managed Cards)
   * 2. Get beneficiary batch by ID for Bank Details (SEPA and Faster Payments)
   * 3. Conflicts based on determined logic
   */

  @Test
  public void GetBeneficiaryBatch_GetPendingChallengeBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create batch in PENDING_CHALLENGE state
    final String batch = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getLeft();

    BeneficiariesService.getBeneficiaryBatch(batch, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
        .body("operationBatchId.batchId", equalTo(batch))
        .body("operationBatchId.operation", equalTo("CREATE"));
  }

  @Test
  public void GetBeneficiaryBatch_GetFailedBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create batch in FAILED state
    final String batch = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getLeft();

    BeneficiariesService.getBeneficiaryBatch(batch, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.FAILED.name()))
        .body("operationBatchId.batchId", equalTo(batch))
        .body("operationBatchId.operation", equalTo("CREATE"));
  }

  @Test
  public void GetBeneficiaryBatch_GetChallengeFailedBatch_Success() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create batch in CHALLENGE_FAILED state
    final String batch = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getLeft();

    BeneficiariesService.getBeneficiaryBatch(batch, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.CHALLENGE_FAILED.name()))
        .body("operationBatchId.batchId", equalTo(batch))
        .body("operationBatchId.operation", equalTo("CREATE"));
  }

  @Test
  public void GetBeneficiaryBatch_GetCompletedBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create batch in COMPLETED state
    final String batch = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getLeft();

    BeneficiariesService.getBeneficiaryBatch(batch, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.COMPLETED.name()))
        .body("operationBatchId.batchId", equalTo(batch))
        .body("operationBatchId.operation", equalTo("CREATE"));
  }

  @Test
  public void GetBeneficiaryBatch_GetChallengeCompletedBatch_Success() {
    //TODO
  }

  @Test
  public void GetBeneficiaryBatch_GetRemoveCompletedBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create batch in COMPLETED state
    final String batch = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getLeft();

    BeneficiariesService.getBeneficiaryBatch(batch, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.COMPLETED.name()))
        .body("operationBatchId.batchId", equalTo(batch))
        .body("operationBatchId.operation", equalTo("REMOVE"));
  }

  @Test
  public void GetBeneficiaryBatch_GetRemovePendingChallengeBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create batch in COMPLETED state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    final String removeBatch = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel.Remove(List.of(batchAndBeneficiary.getRight())).build(), secretKey, getToken())
        .jsonPath().get("operationBatchId.batchId");

    BeneficiariesService.getBeneficiaryBatch(removeBatch, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
        .body("operationBatchId.batchId", equalTo(removeBatch))
        .body("operationBatchId.operation", equalTo("REMOVE"));
  }

  @Test
  public void GetBeneficiaryBatch_InvalidBeneficiaryId_BadRequest() {
    BeneficiariesService.getBeneficiaryBatch(RandomStringUtils.randomNumeric(18), secretKey, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void GetBeneficiaryBatch_NoApiKey_BadRequest() {
    BeneficiariesService.getBeneficiaryBatch(RandomStringUtils.randomNumeric(18), "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void GetBeneficiaryBatch_InvalidApiKey_Unauthorised() {
    BeneficiariesService.getBeneficiaryBatch(RandomStringUtils.randomNumeric(18), "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void GetBeneficiaryBatch_InvalidToken_Unauthorised() {
    BeneficiariesService.getBeneficiaryBatch(RandomStringUtils.randomNumeric(18), secretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}