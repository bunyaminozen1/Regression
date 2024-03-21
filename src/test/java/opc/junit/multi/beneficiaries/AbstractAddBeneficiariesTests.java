package opc.junit.multi.beneficiaries;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.restassured.response.Response;
import java.util.List;
import opc.enums.opc.BeneficiaryState;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.BankAccountDetailsModel;
import opc.models.multi.beneficiaries.BeneficiaryDetailsModel;
import opc.models.multi.beneficiaries.BeneficiaryInformationModel;
import opc.models.multi.beneficiaries.BeneficiaryModel;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public abstract class AbstractAddBeneficiariesTests extends BaseBeneficiariesSetup {

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
   * 1. create beneficiaries for Instruments (Managed Accounts, Managed Cards)
   * 2. create beneficiaries for Bank Details (SEPA and Faster Payments)
   * 3. creating multiple beneficiaries (Instruments and Bank Details)
   * 4. failing to add beneficiaries based on predefined logic
   */

  @Test
  public void CreateBeneficiaries_AddValidManagedAccountInstrument_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
            getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidManagedAccountInstruments_Success() {
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
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddInvalidManagedAccountInstrument_Success() {
    // Create beneficiary with invalid managed account (does not exist)
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(RandomStringUtils.randomNumeric(18))).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddInvalidManagedAccountInstrumentIdentityName_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create model with invalid identity name
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, RandomStringUtils.randomAlphabetic(8), List.of(managedAccountId)).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidManagedCardInstrument_Success() {
    // Create destination managed card to be used as beneficiary instrument
    final String managedCardId = createPrepaidManagedCard(getDestinationPrepaidManagedCardProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_CARDS, getDestinationIdentityName(), List.of(managedCardId)).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidManagedCardInstruments_Success() {
    // Create destination managed card to be used as beneficiary instrument
    final List<String> managedCardIds = createManagedCards(getDestinationPrepaidManagedCardProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 3);

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_CARDS, getDestinationIdentityName(), managedCardIds).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddInvalidManagedCardInstrument_Success() {
    // Create beneficiary with invalid managed card (does not exist)
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_CARDS, getDestinationIdentityName(),
            List.of(RandomStringUtils.randomNumeric(18))).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddInvalidManagedCardInstrumentIdentityName_Success() {
    // Create destination managed card to be used as beneficiary instrument
    final String managedCardId = createPrepaidManagedCard(getDestinationPrepaidManagedCardProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create model with invalid identity name
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_CARDS, RandomStringUtils.randomAlphabetic(8), List.of(managedCardId)).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidSEPABankDetailsBeneficiary_Success() {
    // Create beneficiary with valid SEPA bank details
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(ModelHelper.generateRandomValidSEPABankDetails())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidSEPABankDetailsBeneficiaries_Success() {
    // Create multiple beneficiaries with valid SEPA bank details
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), ModelHelper.createMultipleValidSEPABankDetails(3)).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidFasterPaymentsBankDetailsBeneficiary_Success() {
    // Create beneficiary with valid Faster Payments bank details
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.FasterPaymentsBeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(ModelHelper.generateRandomValidFasterPaymentsBankDetails())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddValidFasterPaymentsBankDetailsBeneficiaries_Success() {
    // Create multiple beneficiaries with valid Faster Payments bank details
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.FasterPaymentsBeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), ModelHelper.createMultipleValidFasterPaymentsBankDetails(3)).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddInvalidIbanAndBankIdentifierCodeBeneficiary_BadRequest() {
    //Create beneficiaries model with bankAddress and address too long
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.builder().setBeneficiaries(List.of(BeneficiaryModel.builder()
            .setBeneficiaryInformation(BeneficiaryInformationModel.builder()
                .setBusinessName(getDestinationIdentityName()).build())
            .setBeneficiaryDetails(BeneficiaryDetailsModel.builder()
                // Set invalid iban and bankIdentifierCode
                .setBankAccountDetails(BankAccountDetailsModel.SEPABankAccountDetails(
                    RandomStringUtils.randomAlphanumeric(14),
                    RandomStringUtils.randomAlphanumeric(7)).build())
                .build()).build())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void CreateBeneficiaries_AddInvalidAccountNumberAndSortCodeBeneficiary_BadRequest() {
    //Create beneficiaries model with wrong account number and sort code
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.builder().setBeneficiaries(List.of(BeneficiaryModel.builder()
            .setBeneficiaryInformation(BeneficiaryInformationModel.builder()
                .setBusinessName(getDestinationIdentityName()).build())
            .setBeneficiaryDetails(BeneficiaryDetailsModel.builder()
                // Set invalid accountNumber and sortCode
                .setBankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(
                    RandomStringUtils.randomAlphanumeric(7),
                    RandomStringUtils.randomAlphanumeric(5)).build())
                .build()).build())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void CreateBeneficiaries_AddInvalidBankCountryBeneficiary_BadRequest() {
    //Create beneficiaries model with invalid bankCountry
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.builder().setBeneficiaries(List.of(BeneficiaryModel.builder()
            .setBeneficiaryInformation(BeneficiaryInformationModel.builder()
                    .setBusinessName(getDestinationIdentityName()).build())
                .setBeneficiaryDetails(BeneficiaryDetailsModel.builder()
                    .setBankAccountDetails(BankAccountDetailsModel.SEPABankAccountDetails(ModelHelper.generateRandomValidIban(),
                        ModelHelper.generateRandomValidBankIdentifierNumber()).build())
                    // Set invalid bank country (expect valid 2 Alpha Code country)
                    .setBankCountry(RandomStringUtils.randomAlphabetic(3))
                    .build()).build())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void CreateBeneficiaries_AddInvalidBankAddressAndAddressTooLongBeneficiary_BadRequest() {
    //Create beneficiaries model with bankAddress and address too long
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.builder().setBeneficiaries(List.of(BeneficiaryModel.builder()
            .setBeneficiaryInformation(BeneficiaryInformationModel.builder()
                .setBusinessName(getDestinationIdentityName()).build())
            .setBeneficiaryDetails(BeneficiaryDetailsModel.builder()
                .setBankAccountDetails(BankAccountDetailsModel.SEPABankAccountDetails(ModelHelper.generateRandomValidIban(),
                    ModelHelper.generateRandomValidBankIdentifierNumber()).build())
                .setBankCountry("MT")
                // Set address and bankAddress too long
                .setAddress(RandomStringUtils.randomAlphabetic(151))
                .setBankAddress(RandomStringUtils.randomAlphabetic(151))
                .build()).build())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void CreateBeneficiaries_AddMultipleValidBeneficiaries_Success() {
    // Create multiple beneficiaries with valid Instrument, SEPA and Faster Payments bank details
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.MultipleBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(),
            List.of(managedAccountId),
            List.of(ModelHelper.generateRandomValidSEPABankDetails()),
            List.of(ModelHelper.generateRandomValidFasterPaymentsBankDetails())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddMultipleValidAndInvalidBeneficiaries_Success() {
    // Create multiple beneficiaries with invalid Instrument,valid SEPA and valid Faster Payments bank details
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.MultipleBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(),
            // Invalid Instrument
            List.of(RandomStringUtils.randomNumeric(18)),
            // Valid SEPA beneficiary
            List.of(ModelHelper.generateRandomValidSEPABankDetails()),
            // Valid Faster Payments beneficiary
            List.of(ModelHelper.generateRandomValidFasterPaymentsBankDetails())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    // Expect Batch to still be pending challenge because of the valid SEPA and Faster Payment Beneficiaries
    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_AddMultipleInvalidBeneficiaries_Success() {
    // Create multiple beneficiaries with invalid instrument beneficiaries
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(),
            List.of(
                // Invalid Instruments
                RandomStringUtils.randomNumeric(18),
                RandomStringUtils.randomNumeric(18),
                RandomStringUtils.randomNumeric(18))).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    // Expect batch to fail because all the beneficiaries are invalid
    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(beneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_SucceedToAddSameBeneficiaryInStateChallengeFailed_Success() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in CHALLENGE_FAILED state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

    // Create same beneficiary
    final CreateBeneficiariesBatchModel createSameBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response sameBeneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createSameBeneficiariesBatchModel, secretKey, getToken());
    sameBeneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(sameBeneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_SucceedToAddSameBeneficiaryInStateInvalid_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in INVALID state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

    // Create same beneficiary
    final CreateBeneficiariesBatchModel createSameBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response sameBeneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createSameBeneficiariesBatchModel, secretKey, getToken());
    sameBeneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(sameBeneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_SucceedToAddSameBeneficiaryInStateRemoved_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in REMOVED state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

    // Create same beneficiary
    final CreateBeneficiariesBatchModel createSameBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response sameBeneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createSameBeneficiariesBatchModel, secretKey, getToken());
    sameBeneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
        getBeneficiaryBatchId(sameBeneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_FailToAddSameBeneficiaryInStatePendingChallenge_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

    // Create same beneficiary
    final CreateBeneficiariesBatchModel createSameBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response sameBeneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createSameBeneficiariesBatchModel, secretKey, getToken());
    sameBeneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(sameBeneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_FailToAddSameBeneficiaryInStateActive_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in ACTIVE state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

    // Create same beneficiary
    final CreateBeneficiariesBatchModel createSameBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response sameBeneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createSameBeneficiariesBatchModel, secretKey, getToken());
    sameBeneficiariesBatchResponse.then()
        .statusCode(SC_OK)
        .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
        .body("operationBatchId.batchId", notNullValue())
        .body("operationBatchId.operation", equalTo("CREATE"));

    BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED,
        getBeneficiaryBatchId(sameBeneficiariesBatchResponse) , secretKey, getToken());
  }

  @Test
  public void CreateBeneficiaries_NoApiKey_BadRequest() {

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(ModelHelper.generateRandomValidSEPABankDetails())).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, "", getToken())
        .then().statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void CreateBeneficiaries_InvalidApiKey_Unauthorised() {
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(ModelHelper.generateRandomValidSEPABankDetails())).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, "abc", getToken())
        .then().statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void CreateBeneficiaries_InvalidToken_Unauthorised() {
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
            getDestinationIdentityName(), List.of(ModelHelper.generateRandomValidSEPABankDetails())).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, "")
        .then().statusCode(SC_UNAUTHORIZED);
  }
}