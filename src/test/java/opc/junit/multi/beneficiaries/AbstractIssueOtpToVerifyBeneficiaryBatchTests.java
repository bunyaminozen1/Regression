package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public abstract class AbstractIssueOtpToVerifyBeneficiaryBatchTests extends BaseBeneficiariesSetup {
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
   * 1. Start OTP verification for Instruments (Managed Accounts, Managed Cards)
   * 2. Start OTP verification for Bank Details (SEPA and Faster Payments)
   * 3. conflicts based on determined logic
   */

  @Test
  public void StartOtpVerification_StartVerificationOnPendingChallengeBeneficiaryBatch_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchOtpVerification(batchId, EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void StartOtpVerification_StartVerificationOnCompletedBeneficiaryBatch_Conflict() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state, batch COMPLETED state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchOtpVerification(batchId, EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void StartOtpVerification_StartVerificationOnAlreadyStartedBeneficiaryBatch_Conflict() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesHelper.startBeneficiaryBatchOtpVerification(batchId, EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.startBeneficiaryBatchOtpVerification(batchId, EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void StartOtpVerification_StartVerificationOnNonExistentBeneficiaryBatch_NotFound() {
    BeneficiariesService.startBeneficiaryBatchOtpVerification(
        RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void StartOtpVerification_NoApiKey_BadRequest() {
    BeneficiariesService.startBeneficiaryBatchOtpVerification(
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void StartOtpVerification_InvalidApiKey_Unauthorised() {
    BeneficiariesService.startBeneficiaryBatchOtpVerification(
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void StartOtpVerification_InvalidToken_Unauthorised() {
    BeneficiariesService.startBeneficiaryBatchOtpVerification(
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), secretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}