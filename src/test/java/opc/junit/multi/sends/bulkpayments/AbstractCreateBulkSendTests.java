package opc.junit.multi.sends.bulkpayments;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import commons.enums.State;
import opc.helpers.SendModelHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.multi.sends.BaseSendsSetup;
import opc.models.multi.sends.BulkSendFundsModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(MultiTags.BULK_SENDS)
public abstract class AbstractCreateBulkSendTests extends BaseSendsSetup {
  protected abstract String getToken();

  protected abstract String getCurrency();

  protected abstract String getPrepaidManagedCardProfileId();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationToken();

  protected abstract String getDestinationCurrency();

  protected abstract String getDestinationIdentityName();

  protected abstract IdentityType getIdentityType();

  /**
   * Test cases for create bulk Sends
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Create All Valid Non Exempted Bulk Sends
   * 2. Create All Valid Exempted Bulk Sends
   * 3. Create All Invalid Bulk Sends
   * 4. Create Mix of valid and invalid Sends
   * 5. Create Mix of exempted and non exempted Sends
   * 6. Unhappy Path
   */

  @Test
  public void CreateBulkSend_AllValidMaToMaSend_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
            sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());
    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.id", equalTo(destinationManagedAccountId))
        .body("response[0].destination.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].source.id", equalTo(sourceManagedAccountId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedAccountId))
        .body("response[1].destination.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].source.id", equalTo(sourceManagedAccountId))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("PENDING"))
        .body("send[1].state", equalTo("PENDING"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_AllValidMaToMcSend_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp, MANAGED_ACCOUNTS,
                sourceManagedAccountId, MANAGED_CARDS, destinationManagedCardId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());
    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.id", equalTo(destinationManagedCardId))
        .body("response[0].destination.type", equalTo(MANAGED_CARDS.name().toLowerCase()))
        .body("response[0].source.id", equalTo(sourceManagedAccountId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedCardId))
        .body("response[1].destination.type", equalTo(MANAGED_CARDS.name().toLowerCase()))
        .body("response[1].source.id", equalTo(sourceManagedAccountId))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("PENDING"))
        .body("send[1].state", equalTo("PENDING"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_AllValidMcToMcSend_Success() {
    final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp, ManagedInstrumentType.MANAGED_CARDS,
                sourceManagedCardId, ManagedInstrumentType.MANAGED_CARDS, destinationManagedCardId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());
    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.id", equalTo(destinationManagedCardId))
        .body("response[0].destination.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("response[0].source.id", equalTo(sourceManagedCardId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedCardId))
        .body("response[1].destination.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("response[1].source.id", equalTo(sourceManagedCardId))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("PENDING"))
        .body("send[1].state", equalTo("PENDING"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_AllValidMcToMaSend_Success() {
    final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp, ManagedInstrumentType.MANAGED_CARDS,
                sourceManagedCardId, MANAGED_ACCOUNTS, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());
    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.id", equalTo(destinationManagedAccountId))
        .body("response[0].destination.type", equalTo(MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].source.id", equalTo(sourceManagedCardId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedAccountId))
        .body("response[1].destination.type", equalTo(MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].source.id", equalTo(sourceManagedCardId))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_CARDS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("PENDING"))
        .body("send[1].state", equalTo("PENDING"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_AllValidMaToMaBeneficiarySend_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(),ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), destinationManagedAccountId,
        secretKeyScaSendsApp, getToken()).getRight();

    final SendFundsModel beneficiarySendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
            .build();

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, beneficiarySendFundsModel),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.beneficiaryId", equalTo(beneficiaryId))
        .body("response[0].source.id", equalTo(sourceManagedAccountId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.beneficiaryId", equalTo(beneficiaryId))
        .body("response[1].source.id", equalTo(sourceManagedAccountId))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.COMPLETED.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("COMPLETED"))
        .body("send[1].state", equalTo("COMPLETED"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_AllValidMcToMcBeneficiarySend_Success() {
    final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(),ManagedInstrumentType.MANAGED_CARDS, getDestinationIdentityName(), destinationManagedCardId,
        secretKeyScaSendsApp, getToken()).getRight();

    final SendFundsModel beneficiarySendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedCardId, MANAGED_CARDS))
            .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
            .build();

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, beneficiarySendFundsModel),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.beneficiaryId", equalTo(beneficiaryId))
        .body("response[0].source.id", equalTo(sourceManagedCardId))
        .body("response[0].source.type", equalTo(MANAGED_CARDS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.beneficiaryId", equalTo(beneficiaryId))
        .body("response[1].source.id", equalTo(sourceManagedCardId))
        .body("response[1].source.type", equalTo(MANAGED_CARDS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.COMPLETED.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("COMPLETED"))
        .body("send[1].state", equalTo("COMPLETED"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_AllInvalidSend_Success() {
    final String sourceManagedAccountId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedCard(sourceManagedAccountId, getCurrency(), 10000L);

    final String invalidValue = String.format("12%s",RandomStringUtils.randomNumeric(16));

    final SendFundsModel invalidbeneficiarySendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(invalidValue))
            .build();

    final SendFundsModel invalidSourceSendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(invalidValue, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
            .build();

    final Response response = SendsService
        .bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(invalidbeneficiarySendFundsModel, invalidSourceSendFundsModel)).build(),
            secretKeyScaSendsApp, getToken(), Optional.empty());
    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.beneficiaryId", equalTo(invalidValue))
        .body("response[0].source.id", equalTo(sourceManagedAccountId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedAccountId))
        .body("response[1].destination.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].source.id", equalTo(invalidValue))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.INVALID.name());}

    // Expect all the sends in bulk to be in state INVALID
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("INVALID"))
        .body("send[1].state", equalTo("INVALID"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkSend_MixOfValidAndInvalidSend_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final SendFundsModel validSendModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
            .setTag("valid")
            .build();

    final String invalidValue = String.format("12%s",RandomStringUtils.randomNumeric(16));
    final SendFundsModel invalidSendModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(invalidValue, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
            .setTag("invalid")
            .build();

    final Response response = SendsService
        .bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(validSendModel, invalidSendModel)).build(),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedAccountId))
        .body("response[1].destination.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].source.id", equalTo(sourceManagedAccountId))
        .body("response[0].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.id", equalTo(destinationManagedAccountId))
        .body("response[1].destination.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].source.id", equalTo(invalidValue))
        .body("response[1].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()));

    final String validSendId = getSendIdViaTag(response, "valid").get(0);
    final String invalidSendId = getSendIdViaTag(response, "invalid").get(0);
    SendsHelper.checkSendStateById(validSendId, State.REQUIRES_SCA.name());
    SendsHelper.checkSendStateById(invalidSendId, State.INVALID.name());

    // Expect valid Sends to be in PENDING, invalid in INVALID
    SendsHelper.getSend(secretKeyScaSendsApp, validSendId, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING"));

    SendsHelper.getSend(secretKeyScaSendsApp, invalidSendId, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("INVALID"));
  }

  @Test
  public void CreateBulkSend_SingleSendNotExempted_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(),ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), destinationManagedAccountId,
        secretKeyScaSendsApp, getToken()).getRight();

    final SendFundsModel beneficiarySendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
            .build();

    final SendFundsModel normalSendModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
            .build();

    final Response response = SendsService
        .bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(beneficiarySendFundsModel, beneficiarySendFundsModel, normalSendModel)).build(),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[0].destination.beneficiaryId", equalTo(beneficiaryId))
        .body("response[0].source.id", equalTo(sourceManagedAccountId))
        .body("response[0].source.type", equalTo(MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[0].destinationAmount.amount", equalTo(100))
        .body("response[0].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[1].destination.beneficiaryId", equalTo(beneficiaryId))
        .body("response[1].source.id", equalTo(sourceManagedAccountId))
        .body("response[1].source.type", equalTo(MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[1].destinationAmount.amount", equalTo(100))
        .body("response[1].destinationAmount.currency", equalTo(getCurrency()))
        .body("response[2].profileId", equalTo(sendsProfileIdScaSendsApp))
        .body("response[2].destination.id", equalTo(destinationManagedAccountId))
        .body("response[2].destination.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[2].source.id", equalTo(sourceManagedAccountId))
        .body("response[2].source.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("response[2].destinationAmount.amount", equalTo(100))
        .body("response[2].destinationAmount.currency", equalTo(getCurrency()));

    // Since we have a single send model that does not exempt it from SCA, all the sends in bulk are in PENDING state
    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    // Expect all the sends in bulk to be in state PENDING_CHALLENGE (No exemptions)
    SendsHelper.getSends(secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK)
        .body("send[0].state", equalTo("PENDING"))
        .body("send[1].state", equalTo("PENDING"))
        .body("send[2].state", equalTo("PENDING"))
        .body("count", equalTo(3))
        .body("responseCount", equalTo(3));
  }

  @Test
  public void CreateBulkSend_InvalidPayload_BadRequest() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final SendFundsModel validSendModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
            .build();

    final SendFundsModel invalidSyntaxSendModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomAlphabetic(18), MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
            .build();

    SendsService.bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(validSendModel, invalidSyntaxSendModel)).build(),
            secretKeyScaSendsApp, getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("request.sends[1].source.id: must match \"^[0-9]+$\""));
  }

  @Test
  public void CreateBulkSend_TransactionLimitExceeded_Conflict() {
    // Current Bulk Transaction Limit for Send
    final int transactionLimit = 1000;
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(transactionLimit + 1, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("TRANSACTION_LIMIT_EXCEEDED"));
  }

  private List<String> getSendIdViaTag(final Response response,
      final String tag) {
    return response.path("response.findAll {send -> send.tag == '"+ tag +"'}.id");
  }
}
