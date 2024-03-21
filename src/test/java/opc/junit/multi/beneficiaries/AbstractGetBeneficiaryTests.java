package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import io.restassured.response.Response;
import java.util.List;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public abstract class AbstractGetBeneficiaryTests extends BaseBeneficiariesSetup {
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
   * 1. Get beneficiary by ID for Instruments (Managed Accounts, Managed Cards)
   * 2. Get beneficiary by ID for Bank Details (SEPA and Faster Payments)
   * 3. Conflicts based on determined logic
   */

  @Test
  public void GetBeneficiary_GetPendingChallengeBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    final String beneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getRight();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.PENDING_CHALLENGE.name()));
  }

  @Test
  public void GetBeneficiary_GetInvalidBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in INVALID state
    final String beneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getRight();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.INVALID.name()))
        .body("validationFailure", equalTo("INSTRUMENT_DETAILS_NOT_FOUND"));
  }

  @Test
  public void GetBeneficiary_GetRemovedBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in REMOVED state
    final String beneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getRight();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.REMOVED.name()))
        .body("relatedOperationBatches[0].operation", equalTo("CREATE"))
        .body("relatedOperationBatches[1].operation", equalTo("REMOVE"));
  }

  @Test
  public void GetBeneficiary_GetActiveBeneficiary_Success() {
    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getRight();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.ACTIVE.name()));
  }

  @Test
  public void GetBeneficiary_GetChallengeFailedBeneficiary_Success() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    final String managedAccount = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in CHALLENGE_FAILED state
    final String beneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccount, secretKey, getToken()).getRight();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.CHALLENGE_FAILED.name()));
  }

  @Test
  public void GetBeneficiary_GetNonExistentBeneficiary_NotFound() {
    BeneficiariesService.getBeneficiary(RandomStringUtils.randomNumeric(18), secretKey, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void GetBeneficiary_GetManagedAccountDetailsBeneficiary_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response batchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    final String beneficiary = BeneficiariesHelper.getBeneficiaryIdByInstrumentIdAndBatchId(managedAccountId,
        getBeneficiaryBatchId(batchResponse), secretKey, getToken());
    final List<String> initialExternalRef = createBeneficiariesBatchModel.getBeneficiaries().get(0).getExternalRefs();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.PENDING_CHALLENGE.name()))
        .body("trustLevel", equalTo("TRUSTED"))
        .body("group", notNullValue())
        .body("externalRefs", equalTo(initialExternalRef))
        .body("id", equalTo(beneficiary))
        .body(getIdentityType() == IdentityType.CORPORATE?
            "beneficiaryInformation.businessName" : "beneficiaryInformation.fullName", equalTo(getDestinationIdentityName()))
        .body("beneficiaryDetails.instrument.id", equalTo(managedAccountId))
        .body("beneficiaryDetails.instrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("beneficiaryDetails.bankAccountDetails", nullValue())
        .body("beneficiaryDetails.address", nullValue())
        .body("beneficiaryDetails.bankCountry", nullValue())
        .body("beneficiaryDetails.bankAddress", nullValue())
        .body("beneficiaryDetails.bankName", nullValue());
  }

  @Test
  public void GetBeneficiary_GetManagedCardDetailsBeneficiary_Success() {
    // Create destination managed card to be used as beneficiary instrument
    final String managedCardId = createPrepaidManagedCard(getDestinationPrepaidManagedCardProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_CARDS, getDestinationIdentityName(), List.of(managedCardId)).build();

    final Response batchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    final String beneficiary = BeneficiariesHelper.getBeneficiaryIdByInstrumentIdAndBatchId(managedCardId,
        getBeneficiaryBatchId(batchResponse), secretKey, getToken());
    final List<String> initialExternalRef = createBeneficiariesBatchModel.getBeneficiaries().get(0).getExternalRefs();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.PENDING_CHALLENGE.name()))
        .body("trustLevel", equalTo("TRUSTED"))
        .body("group", notNullValue())
        .body("externalRefs", equalTo(initialExternalRef))
        .body("id", equalTo(beneficiary))
        .body(getIdentityType() == IdentityType.CORPORATE?
            "beneficiaryInformation.businessName" : "beneficiaryInformation.fullName", equalTo(getDestinationIdentityName()))
        .body("beneficiaryDetails.instrument.id", equalTo(managedCardId))
        .body("beneficiaryDetails.instrument.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("beneficiaryDetails.bankAccountDetails", nullValue())
        .body("beneficiaryDetails.address", nullValue())
        .body("beneficiaryDetails.bankCountry", nullValue())
        .body("beneficiaryDetails.bankAddress", nullValue())
        .body("beneficiaryDetails.bankName", nullValue());
  }

  @Test
  public void GetBeneficiary_GetSEPABankDetailsBeneficiary_Success() {
    // Create beneficiary with valid SEPA bank details
    final Pair<String, String> SEPADetails = ModelHelper.generateRandomValidSEPABankDetails();
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(SEPADetails)).build();

    final Response batchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    final String beneficiary = BeneficiariesHelper.getBeneficiaryIdByIbanAndBatchId(SEPADetails.getLeft(),
        getBeneficiaryBatchId(batchResponse), secretKey, getToken());
    final List<String> initialExternalRef = createBeneficiariesBatchModel.getBeneficiaries().get(0).getExternalRefs();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.PENDING_CHALLENGE.name()))
        .body("trustLevel", equalTo("TRUSTED"))
        .body("group", notNullValue())
        .body("externalRefs", equalTo(initialExternalRef))
        .body("id", equalTo(beneficiary))
        .body(getIdentityType() == IdentityType.CORPORATE?
            "beneficiaryInformation.businessName" : "beneficiaryInformation.fullName", equalTo(getDestinationIdentityName()))
        .body("beneficiaryDetails.instrument.type", nullValue())
        .body("beneficiaryDetails.instrument.id", nullValue())
        .body("beneficiaryDetails.bankAccountDetails.iban", equalTo(SEPADetails.getLeft()))
        .body("beneficiaryDetails.bankAccountDetails.bankIdentifierCode", equalTo(SEPADetails.getRight()))
        .body("beneficiaryDetails.bankAccountDetails.accountNumber", nullValue())
        .body("beneficiaryDetails.bankAccountDetails.sortCode", nullValue())
        .body("beneficiaryDetails.address", notNullValue())
        .body("beneficiaryDetails.bankCountry", notNullValue())
        .body("beneficiaryDetails.bankAddress", notNullValue())
        .body("beneficiaryDetails.bankName", notNullValue());
  }

  @Test
  public void GetBeneficiary_GetFasterPaymentsBankDetailsBeneficiary_Success() {
    // Create beneficiary with valid Faster Payments bank details
    final Pair<String, String> FasterPaymentsDetails = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.FasterPaymentsBeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(FasterPaymentsDetails)).build();

    final Response batchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    final String beneficiary = BeneficiariesHelper.getBeneficiaryIdByAccountNumberAndBatchId(FasterPaymentsDetails.getLeft(),
        getBeneficiaryBatchId(batchResponse), secretKey, getToken());
    final List<String> initialExternalRef = createBeneficiariesBatchModel.getBeneficiaries().get(0).getExternalRefs();

    BeneficiariesService.getBeneficiary(beneficiary, secretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiaryState.PENDING_CHALLENGE.name()))
        .body("trustLevel", equalTo("TRUSTED"))
        .body("group", notNullValue())
        .body("externalRefs", equalTo(initialExternalRef))
        .body("id", equalTo(beneficiary))
        .body(getIdentityType() == IdentityType.CORPORATE?
            "beneficiaryInformation.businessName" : "beneficiaryInformation.fullName", equalTo(getDestinationIdentityName()))
        .body("beneficiaryDetails.instrument.type", nullValue())
        .body("beneficiaryDetails.instrument.id", nullValue())
        .body("beneficiaryDetails.bankAccountDetails.accountNumber", equalTo(FasterPaymentsDetails.getLeft()))
        .body("beneficiaryDetails.bankAccountDetails.sortCode", equalTo(FasterPaymentsDetails.getRight()))
        .body("beneficiaryDetails.bankAccountDetails.iban", nullValue())
        .body("beneficiaryDetails.bankAccountDetails.bankIdentifierCode", nullValue())
        .body("beneficiaryDetails.address", notNullValue())
        .body("beneficiaryDetails.bankCountry", notNullValue())
        .body("beneficiaryDetails.bankAddress", notNullValue())
        .body("beneficiaryDetails.bankName", notNullValue());
  }

  @Test
  public void GetBeneficiary_InvalidBeneficiaryId_BadRequest() {
    BeneficiariesService.getBeneficiary(RandomStringUtils.randomAlphanumeric(5), secretKey, getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void GetBeneficiary_NoApiKey_BadRequest() {
    BeneficiariesService.getBeneficiary(RandomStringUtils.randomNumeric(18), "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void GetBeneficiary_InvalidApiKey_Unauthorised() {
    BeneficiariesService.getBeneficiary(RandomStringUtils.randomNumeric(18), "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void GetBeneficiary_InvalidToken_Unauthorised() {
    BeneficiariesService.getBeneficiary(RandomStringUtils.randomNumeric(18), secretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}
