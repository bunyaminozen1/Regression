package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public abstract class AbstractGetBeneficiaryBatchesTests extends BaseBeneficiariesSetup {
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
   * 1. get beneficiary batches for Instruments (Managed Accounts, Managed Cards)
   * 2. get beneficiary batches for Bank Details (SEPA and Faster Payments)
   * 3. get multiple beneficiary batches (Instruments and Bank Details)
   * 4. using filters
   */

  @Test
  public void GetBeneficiaryBatches_GetMultipleBeneficiaryBatches_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final String firstBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken()));
    final String secondBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken()));

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.empty(), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
        .body("batches[0].operationBatchId.batchId", equalTo(firstBatchId))
        .body("batches[1].state", equalTo(BeneficiariesBatchState.FAILED.name()))
        .body("batches[1].operationBatchId.batchId", equalTo(secondBatchId))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void GetBeneficiaryBatches_FilterByLimit_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final String firstBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken()));
    final String secondBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken()));

    final Map<String, Object> filters = new HashMap<>();
    filters.put("offset", 0);
    filters.put("limit", 1);

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(filters), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
        .body("batches[0].operationBatchId.batchId", equalTo(firstBatchId))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaryBatches_FilterByOffset_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final String firstBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken()));
    final String secondBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken()));

    final Map<String, Object> filters = new HashMap<>();
    filters.put("offset", 1);
    filters.put("limit", 1);

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(filters), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].state", equalTo(BeneficiariesBatchState.FAILED.name()))
        .body("batches[0].operationBatchId.batchId", equalTo(secondBatchId))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaryBatches_FilterByOperation_Success() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    // Create destination managed accounts to be used as beneficiary instruments
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary batch in COMPLETED state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    final Map<String, Object> removeOperationFilter = new HashMap<>();
    removeOperationFilter.put("operation", List.of("REMOVE"));

    final Map<String, Object> createOperationFilter = new HashMap<>();
    createOperationFilter.put("operation", List.of("CREATE"));

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(removeOperationFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].operationBatchId.operation", equalTo("REMOVE"))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(createOperationFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].operationBatchId.operation", equalTo("CREATE"))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaryBatches_FilterByBatchState_Success() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    // Create destination managed accounts to be used as beneficiary instruments
    final List<String> managedAccountIds = createManagedAccounts(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 4);

    // Create beneficiary batch in PENDING_CHALLENGE state
    final String pendingChallengedBatchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(0), secretKey, getToken()).getLeft();

    // Create beneficiary batch in COMPLETED state
    final String completedBatchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(1), secretKey, getToken()).getLeft();

    // Create beneficiary batch in FAILED state
    final String failedBatchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(2), secretKey, getToken()).getLeft();

    // Create beneficiary batch in CHALLENGE_FAILED state
    final String challengeFailedBatchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(3), secretKey, getToken()).getLeft();


    final Map<String, Object> pendingChallengeStateFilter = new HashMap<>();
    pendingChallengeStateFilter.put("state", List.of(BeneficiariesBatchState.PENDING_CHALLENGE.name()));

    final Map<String, Object> completedStateFilter = new HashMap<>();
    completedStateFilter.put("state", List.of(BeneficiariesBatchState.COMPLETED.name()));

    final Map<String, Object> failedStateFilter = new HashMap<>();
    failedStateFilter.put("state", List.of(BeneficiariesBatchState.FAILED.name()));

    final Map<String, Object> challengeFailedStateFilter = new HashMap<>();
    challengeFailedStateFilter.put("state", List.of(BeneficiariesBatchState.CHALLENGE_FAILED.name()));

    final Map<BeneficiariesBatchState, Pair<Map<String, Object>, String>> batchStates = new HashMap<>();
    batchStates.put(BeneficiariesBatchState.PENDING_CHALLENGE, Pair.of(pendingChallengeStateFilter, pendingChallengedBatchId));
    batchStates.put(BeneficiariesBatchState.COMPLETED, Pair.of(completedStateFilter, completedBatchId));
    batchStates.put(BeneficiariesBatchState.FAILED, Pair.of(failedStateFilter, failedBatchId));
    batchStates.put(BeneficiariesBatchState.CHALLENGE_FAILED, Pair.of(challengeFailedStateFilter, challengeFailedBatchId));

    for (var batchState : batchStates.entrySet()) {
      BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(batchState.getValue().getKey()), getToken())
          .then()
          .statusCode(SC_OK)
          .body("batches[0].state", equalTo(batchState.getKey().name()))
          .body("batches[0].operationBatchId.batchId", equalTo(batchState.getValue().getRight()))
          .body("count", equalTo(1))
          .body("responseCount", equalTo(1));
    }
  }

  @Test
  public void GetBeneficiaryBatches_FilterByTag_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final String firstTag = RandomStringUtils.randomAlphabetic(6);
    final String secondTag = RandomStringUtils.randomAlphabetic(6);

    final CreateBeneficiariesBatchModel firstCreateBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId))
            .setTag(firstTag).build();

    final CreateBeneficiariesBatchModel secondCreateBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
                ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId))
            .setTag(secondTag).build();

    final String firstBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(firstCreateBeneficiariesBatchModel, secretKey, getToken()));
    final String secondBatchId = getBeneficiaryBatchId(BeneficiariesService.createBeneficiariesBatch(secondCreateBeneficiariesBatchModel, secretKey, getToken()));

    final Map<String, Object> firstTagFilter = new HashMap<>();
    firstTagFilter.put("tag", List.of(firstTag));
    final Map<String, Object> secondTagFilter = new HashMap<>();
    secondTagFilter.put("tag", List.of(secondTag));

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(firstTagFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].tag", equalTo(firstTag))
        .body("batches[0].operationBatchId.batchId", equalTo(firstBatchId))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));

    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.of(secondTagFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("batches[0].tag", equalTo(secondTag))
        .body("batches[0].operationBatchId.batchId", equalTo(secondBatchId))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaryBatches_NoApiKey_BadRequest() {
    BeneficiariesService.getBeneficiaryBatches("", Optional.empty(), getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void GetBeneficiaryBatches_InvalidApiKey_Unauthorised() {
    BeneficiariesService.getBeneficiaryBatches("abc", Optional.empty(), getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void GetBeneficiaryBatches_InvalidToken_Unauthorised() {
    BeneficiariesService.getBeneficiaryBatches(secretKey, Optional.empty(), "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}