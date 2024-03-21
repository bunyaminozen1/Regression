package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public abstract class AbstractIssuePushNotificationToVerifyBeneficiaryBatchTests extends BaseBeneficiariesSetup {
  protected abstract String getToken();
  protected abstract String getIdentityId();
  protected abstract String getDestinationToken();
  protected abstract String getDestinationCurrency();
  protected abstract String getDestinationIdentityName();
  protected abstract String getDestinationPrepaidManagedCardProfileId();
  protected abstract String getDestinationDebitManagedCardProfileId();
  protected abstract String getDestinationManagedAccountProfileId();
  protected abstract IdentityType getIdentityType();
  private static final String AUTHY_CHANNEL = EnrolmentChannel.AUTHY.name();
  private static final String BIOMETRIC_CHANNEL = EnrolmentChannel.BIOMETRIC.name();

  /**
   * Test cases for Creating/Adding Trusted Beneficiaries
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
   * Test Plan: TBA
   * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
   *
   * The main cases:
   * 1. Start PUSH verification for Instruments (Managed Accounts, Managed Cards)
   * 2. Start PUSH verification for Bank Details (SEPA and Faster Payments)
   * 3. conflicts based on determined logic
   */

  @Test
  public void StartPushNotificationVerification_StartAuthyVerificationOnPendingChallengeBeneficiaryBatch_Success() {
    //Enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, AUTHY_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void StartPushNotificationVerification_StartBiometricVerificationOnPendingChallengeBeneficiaryBatch_Success() {
    //Enroll identity for biometric
    SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKey, secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, BIOMETRIC_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void StartPushNotificationVerification_StartAuthyVerificationOnPendingChallengeBeneficiaryBatchIdentityNotEnrolled_Conflict() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, AUTHY_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void StartPushNotificationVerification_StartBiometricVerificationOnPendingChallengeBeneficiaryBatchIdentityNotEnrolled_Conflict() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, BIOMETRIC_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void StartPushNotificationVerification_StartAuthyVerificationOnAlreadyStartedBeneficiaryBatch_Conflict() {
    //Enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchId, AUTHY_CHANNEL, secretKey, getToken());

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, AUTHY_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void StartPushNotificationVerification_StartBiometricVerificationOnAlreadyStartedBeneficiaryBatch_Conflict() {
    //Enroll identity for biometric
    SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKey, secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchId, BIOMETRIC_CHANNEL, secretKey, getToken());

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, BIOMETRIC_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void StartPushNotificationVerification_StartAuthyVerificationOnCompletedBeneficiaryBatch_Conflict() {
    //Enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state, batch COMPLETED state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, AUTHY_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void StartPushNotificationVerification_StartBiometricVerificationOnCompletedBeneficiaryBatch_Conflict() {
    //Enroll identity for BIOMETRIC
    SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKey, secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state, batch COMPLETED state
    final String batchId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getLeft();

    BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, BIOMETRIC_CHANNEL, secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void StartPushNotificationVerification_NoApiKey_BadRequest() {
    BeneficiariesService.startBeneficiaryBatchPushVerification(
            RandomStringUtils.randomNumeric(18), AUTHY_CHANNEL, "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);

    BeneficiariesService.startBeneficiaryBatchPushVerification(
            RandomStringUtils.randomNumeric(18), BIOMETRIC_CHANNEL, "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void StartPushNotificationVerification_InvalidApiKey_Unauthorised() {
    BeneficiariesService.startBeneficiaryBatchPushVerification(
            RandomStringUtils.randomNumeric(18), AUTHY_CHANNEL, "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);

    BeneficiariesService.startBeneficiaryBatchPushVerification(
            RandomStringUtils.randomNumeric(18), BIOMETRIC_CHANNEL, "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void StartPushNotificationVerification_InvalidToken_Unauthorised() {
    BeneficiariesService.startBeneficiaryBatchPushVerification(
            RandomStringUtils.randomNumeric(18), AUTHY_CHANNEL, secretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);

    BeneficiariesService.startBeneficiaryBatchPushVerification(
            RandomStringUtils.randomNumeric(18), BIOMETRIC_CHANNEL, secretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}