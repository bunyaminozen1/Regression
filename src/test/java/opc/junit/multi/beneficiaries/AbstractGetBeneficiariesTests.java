package opc.junit.multi.beneficiaries;

import io.restassured.response.Response;
import opc.enums.opc.BeneficiaryState;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractGetBeneficiariesTests extends BaseBeneficiariesSetup {
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
   * Documentation: <a href="https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs">...</a>
   * Test Plan: TBA
   * Main ticket: <a href="https://weavr-payments.atlassian.net/browse/ROADMAP-507">...</a>
   * The main cases:
   * 1. get beneficiaries for Instruments (Managed Accounts, Managed Cards)
   * 2. get beneficiaries for Bank Details (SEPA and Faster Payments)
   * 3. get multiple beneficiaries (Instruments and Bank Details)
   * 4. using filters
   */

  @Test
  public void GetBeneficiaries_GetMultipleInstrumentBeneficiaries_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final List<String> managedAccountIds = createManagedAccounts(getDestinationManagedAccountProfileId(),
            getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 3);

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
            CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
                    ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), managedAccountIds).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());

    final List<String> beneficiaryAccounts =
            BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), getToken())
                    .then()
                    .statusCode(SC_OK)
                    .body("count", equalTo(3))
                    .body("responseCount", equalTo(3))
                    .extract()
                    .jsonPath()
                    .getList("beneficiaries.beneficiaryDetails.instrument.id");

    managedAccountIds.forEach(managedAccount -> assertTrue(beneficiaryAccounts.contains(managedAccount)));
  }

  @Test
  public void GetBeneficiaries_FilterByLimit_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final List<String> managedAccountIds = createManagedAccounts(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 3);

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), managedAccountIds).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());

    final Map<String, Object> filters = new HashMap<>();
    filters.put("offset", 0);
    filters.put("limit", 1);

    BeneficiariesService.getBeneficiaries(secretKey, Optional.of(filters), getToken())
        .then()
        .statusCode(SC_OK)
        .body("beneficiaries[0].beneficiaryDetails.instrument.id", equalTo(managedAccountIds.get(0)))
        .body("count", equalTo(3))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaries_FilterByOffset_Success() {
    // Create destination managed accounts to be used as beneficiary instruments
    final List<String> managedAccountIds = createManagedAccounts(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 3);

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), managedAccountIds).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());

    final Map<String, Object> filters = new HashMap<>();
    filters.put("offset", 2);
    filters.put("limit", 1);

    BeneficiariesService.getBeneficiaries(secretKey, Optional.of(filters), getToken())
        .then()
        .statusCode(SC_OK)
        .body("beneficiaries[0].beneficiaryDetails.instrument.id", equalTo(managedAccountIds.get(2)))
        .body("count", equalTo(3))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaries_FilterByBatchId_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    final Response firstBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    final Response secondBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());

    final String firstBatchId = getBeneficiaryBatchId(firstBatchResponse);
    final String secondBatchId = getBeneficiaryBatchId(secondBatchResponse);

    final Map<String, Object> firstFilter = new HashMap<>();
    firstFilter.put("batchId", firstBatchId);

    final Map<String, Object> secondFilter = new HashMap<>();
    secondFilter.put("batchId", secondBatchId);

    BeneficiariesService.getBeneficiaries(secretKey, Optional.of(firstFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("beneficiaries[0].relatedOperationBatches[0].batchId", equalTo(firstBatchId))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));

    BeneficiariesService.getBeneficiaries(secretKey, Optional.of(secondFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("beneficiaries[0].relatedOperationBatches[0].batchId", equalTo(secondBatchId))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaries_InvalidFilterByBatchId_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
            ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), List.of(managedAccountId)).build();

    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());

    final Map<String, Object> firstFilter = new HashMap<>();
    firstFilter.put("batchId", RandomStringUtils.randomAlphanumeric(18));

    BeneficiariesService.getBeneficiaries(secretKey, Optional.of(firstFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("count", equalTo(0))
        .body("responseCount", equalTo(0));
  }

  @Test
  public void GetBeneficairies_FilterByGroup_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

    // Create beneficiary in PENDING_CHALLENGE state
    BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

    //Create beneficiary with custom group field
    final String group = RandomStringUtils.randomAlphabetic(6);
    final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
        CreateBeneficiariesBatchModel.builder().setBeneficiaries(List.of(BeneficiaryModel.builder()
            .setTrustLevel("TRUSTED")
            .setGroup(group)
            .setBeneficiaryInformation(BeneficiaryInformationModel.builder()
                .setBusinessName(getDestinationIdentityName()).build())
            .setBeneficiaryDetails(BeneficiaryDetailsModel.builder()
                .setBankAccountDetails(BankAccountDetailsModel.SEPABankAccountDetails(ModelHelper.generateRandomValidIban(),
                    ModelHelper.generateRandomValidBankIdentifierNumber()).build())
                .setBankCountry("MT")
                .setAddress(RandomStringUtils.randomAlphabetic(6))
                .setBankAddress(RandomStringUtils.randomAlphabetic(6))
                .build()).build())).build();

    final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
    beneficiariesBatchResponse.then()
        .statusCode(SC_OK);

    final Map<String, Object> groupFilter = new HashMap<>();
    groupFilter.put("group", group);

    BeneficiariesService.getBeneficiaries(secretKey, Optional.of(groupFilter), getToken())
        .then()
        .statusCode(SC_OK)
        .body("beneficiaries[0].group", equalTo(group))
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));
  }

  @Test
  public void GetBeneficiaries_NoApiKey_BadRequest() {
    BeneficiariesService.getBeneficiaries("", Optional.empty(), getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void GetBeneficiaries_InvalidApiKey_Unauthorised() {
    BeneficiariesService.getBeneficiaries("abc", Optional.empty(), getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void GetBeneficiaries_InvalidToken_Unauthorised() {
    BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}