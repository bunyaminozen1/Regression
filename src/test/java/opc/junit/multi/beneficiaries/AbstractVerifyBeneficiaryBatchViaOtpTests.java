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
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.models.multi.beneficiaries.RemoveBeneficiariesModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public abstract class AbstractVerifyBeneficiaryBatchViaOtpTests extends BaseBeneficiariesSetup {
  protected abstract String getToken();
  protected abstract String getIdentityId();
  protected abstract String getDestinationToken();
  protected abstract String getDestinationCurrency();
  protected abstract String getDestinationIdentityName();
  protected abstract String getDestinationPrepaidManagedCardProfileId();
  protected abstract String getDestinationDebitManagedCardProfileId();
  protected abstract String getDestinationManagedAccountProfileId();
  protected abstract IdentityType getIdentityType();
  final private String VERIFICATION_CODE = "123456";

  /**
   * Test cases for Creating/Adding Trusted Beneficiaries
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
   * Test Plan: TBA
   * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
   *
   * The main cases:
   * 1. Verify OTP challenge for Instruments (Managed Accounts, Managed Cards)
   * 2. Verify OTP challenge for Bank Details (SEPA and Faster Payments)
   * 3. conflicts based on determined logic
   */

  @Test
  public void VerifyBatchViaOtp_VerifyValidBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchOtpVerification(batchAndBeneficiary.getLeft(),
        EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
        batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
        batchAndBeneficiary.getLeft(), secretKey, getToken());
    BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE,
        batchAndBeneficiary.getRight(), secretKey, getToken());
  }

  @Test
  public void VerifyBatchViaOtp_VerifyMultipleValidBeneficiaries_Success() {
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

    BeneficiariesHelper.startBeneficiaryBatchOtpVerification(getBeneficiaryBatchId(beneficiariesBatchResponse),
        EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            getBeneficiaryBatchId(beneficiariesBatchResponse), EnrolmentChannel.SMS.name(), secretKey, getToken())
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
  public void VerifyBatchViaOtp_VerifyMultipleValidAndInvalidBeneficiaries_Success() {
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

    BeneficiariesHelper.startBeneficiaryBatchOtpVerification(getBeneficiaryBatchId(beneficiariesBatchResponse),
        EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            getBeneficiaryBatchId(beneficiariesBatchResponse), EnrolmentChannel.SMS.name(), secretKey, getToken())
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
  public void VerifyBatchViaOtp_VerifyNonExistentBatch_NotFound() {
    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void VerifyBatchViaOtp_ChallengeAlreadyCompleted_Conflict() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
        batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void VerifyBatchViaOtp_VerifyRemoveBatch_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    final String removeBatchId = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
        .Remove(List.of(batchAndBeneficiary.getRight())).build(), secretKey, getToken())
            .jsonPath().get("operationBatchId.batchId");

    BeneficiariesHelper.startBeneficiaryBatchOtpVerification(removeBatchId,
        EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            removeBatchId, EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_NO_CONTENT);

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
        removeBatchId, secretKey, getToken());
  }

  @Test
  public void VerifyBatchViaOtp_VerificationCodeInvalid_Conflict() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final Pair<String, String> batchAndBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken());

    BeneficiariesHelper.startBeneficiaryBatchOtpVerification(batchAndBeneficiary.getLeft(),
        EnrolmentChannel.SMS.name(), secretKey, getToken());

    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(RandomStringUtils.randomNumeric(6)),
            batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }

  @Test
  public void VerifyBatchViaOtp_NoApiKey_BadRequest() {
    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void VerifyBatchViaOtp_InvalidApiKey_Unauthorised() {
    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void VerifyBatchViaOtp_InvalidToken_Unauthorised() {
    BeneficiariesService.verifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), secretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}