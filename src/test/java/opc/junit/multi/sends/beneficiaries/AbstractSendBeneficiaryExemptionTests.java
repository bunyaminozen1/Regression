package opc.junit.multi.sends.beneficiaries;

import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.multi.sends.BaseSendsSetup;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.BENEFICIARIES_SENDS)
public abstract class AbstractSendBeneficiaryExemptionTests extends BaseSendsSetup {

  protected abstract String getToken();

  protected abstract String getIdentityId();

  protected abstract String getCurrency();

  protected abstract String getPrepaidManagedCardProfileId();

  protected abstract String getDebitManagedCardProfileId();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationToken();

  protected abstract String getDestinationCurrency();

  protected abstract String getDestinationIdentityName();

  protected abstract String getDestinationPrepaidManagedCardProfileId();

  protected abstract String getDestinationDebitManagedCardProfileId();

  protected abstract String getDestinationManagedAccountProfileId();

  protected abstract IdentityType getIdentityType();

  /**
   * Test cases for exempting SEND SCA for trusted beneficiaries
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
   * Test Plan: TBA
   * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
   *
   * The main cases:
   * 1. SEND to MA Beneficiary
   * 2. SEND to MC Beneficiary
   * 3. Unhappy Path
   * 4. Conflicts based on business logic
   */

  @Test
  public void SendBeneficiaryExemption_SendMaToMaBeneficiary_Success() {
    // Create destination managed account to be used as beneficiary instrument
    final String beneficiaryManagedAccountId = createManagedAccount(
        getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
            getDestinationIdentityName(), beneficiaryManagedAccountId, secretKeyScaSendsApp, getToken())
        .getRight();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final SendFundsModel sendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
            .build();

    // TX is SCA Exempted, in COMPLETED state
    SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
        .then()
        .statusCode(SC_OK)
        .body("challengeExemptionReason", equalTo("TRUSTED_BENEFICIARY"))
        .body("destination.beneficiaryId", equalTo(beneficiaryId))
        .body("source.id", equalTo(sourceManagedAccountId))
        .body("source.type", equalTo("managed_accounts"))
        .body("destinationAmount.amount", equalTo(100))
        .body("destinationAmount.currency", equalTo(getCurrency()))
        .body("state", equalTo("COMPLETED"));
  }

  @Test
  public void SendBeneficiaryExemption_SendMcToMcBeneficiary_Success() {
    // Create destination managed card to be used as beneficiary instrument
    final String beneficiaryManagedCardId = createPrepaidManagedCard(
        getDestinationPrepaidManagedCardProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_CARDS,
            getDestinationIdentityName(), beneficiaryManagedCardId, secretKeyScaSendsApp, getToken())
        .getRight();

    fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

    final SendFundsModel sendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedCardId, MANAGED_CARDS))
            .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
            .build();

    // TX is SCA Exempted, in COMPLETED state
    SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
        .then()
        .statusCode(SC_OK)
        .body("challengeExemptionReason", equalTo("TRUSTED_BENEFICIARY"))
        .body("destination.beneficiaryId", equalTo(beneficiaryId))
        .body("source.id", equalTo(sourceManagedCardId))
        .body("source.type", equalTo("managed_cards"))
        .body("destinationAmount.amount", equalTo(100))
        .body("destinationAmount.currency", equalTo(getCurrency()))
        .body("state", equalTo("COMPLETED"));
  }

  @Test
  public void SendBeneficiaryExemption_InvalidBeneficiaryId_NotFound() {
    final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

    final SendFundsModel sendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedCardId, MANAGED_CARDS))
            .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18)))
            .build();

    SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
  }

  @Test
  public void SendBeneficiaryExemption_BlankBeneficiaryId_NotFound() {
    final String sourceManagedCardId = createPrepaidManagedCard(getPrepaidManagedCardProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    fundManagedCard(sourceManagedCardId, getCurrency(), 10000L);

    final SendFundsModel sendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedCardId, MANAGED_CARDS))
            .setDestination(new ManagedInstrumentTypeId(""))
            .build();

    SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("_embedded.errors[0].message", equalTo("request.destination.beneficiaryId: must match \"^[0-9]+$\""))
        .body("_embedded.errors[1].message", equalTo("request.destination.beneficiaryId: must not be blank"));
  }

  @Test
  public void SendBeneficiaryExemption_BankDetailsBeneficiary_Conflict() {

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    // Create Bank Details beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
            secretKeyScaSendsApp, getToken()).getRight();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final SendFundsModel sendFundsModel =
        SendFundsModel.newBuilder()
            .setProfileId(sendsProfileIdScaSendsApp)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
            .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
            .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
            .build();

    SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("BENEFICIARY_TYPE_INVALID"));
  }

  @Test
  public void SendBeneficiaryExemption_InvalidBeneficiaryState_Conflict() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp,
        getToken());

    // Create destination managed accounts to be used as beneficiary instruments
    final List<Pair<String, CreateManagedAccountModel>> managedAccountIds = createManagedAccounts(
        getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken(), 4);

    final List<String> listOfInvalidBeneficiaries = new ArrayList<>();
    // Create beneficiary in PENDING_CHALLENGE state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(),
        ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(0).getLeft(), secretKeyScaSendsApp,
        getToken()).getRight());
    // Create beneficiary in REMOVED state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(1).getLeft(), secretKeyScaSendsApp,
        getToken()).getRight());
    // Create beneficiary in INVALID state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(2).getLeft(), secretKeyScaSendsApp,
        getToken()).getRight());
    // Create beneficiary in CHALLENGE_FAILED state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createInstrumentBeneficiaryInState(
        BeneficiaryState.CHALLENGE_FAILED, getIdentityType(),
        ManagedInstrumentType.MANAGED_ACCOUNTS,
        getDestinationIdentityName(), managedAccountIds.get(3).getLeft(), secretKeyScaSendsApp,
        getToken()).getRight());

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    listOfInvalidBeneficiaries.forEach(beneficiaryId -> {
      final SendFundsModel sendFundsModel =
          SendFundsModel.newBuilder()
              .setProfileId(sendsProfileIdScaSendsApp)
              .setTag(RandomStringUtils.randomAlphabetic(5))
              .setDestinationAmount(new CurrencyAmount(getCurrency(), 100L))
              .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
              .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
              .build();

      SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, getToken(), Optional.empty())
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("BENEFICIARY_TYPE_INVALID"));
    });
  }
}