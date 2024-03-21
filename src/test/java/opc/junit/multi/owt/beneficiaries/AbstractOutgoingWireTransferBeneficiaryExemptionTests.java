package opc.junit.multi.owt.beneficiaries;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.Response;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransferResponseModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.BENEFICIARIES_OWT)
public abstract class AbstractOutgoingWireTransferBeneficiaryExemptionTests extends
    BaseOutgoingWireTransfersSetup {

  protected abstract String getToken();

  protected abstract String getIdentityId();

  protected abstract String getCurrency();

  protected abstract String getPrepaidManagedCardProfileId();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationToken();

  protected abstract String getDestinationCurrency();

  protected abstract String getDestinationIdentityName();

  protected abstract String getDestinationPrepaidManagedCardProfileId();

  protected abstract String getDestinationManagedAccountProfileId();

  protected abstract IdentityType getIdentityType();

  /**
   * Test cases for exempting OWT SCA for trusted beneficiaries
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
   * Test Plan: TBA
   * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
   *
   * The main cases:
   * 1. OWT to SEPA Beneficiary
   * 2. OWT to Faster Payments Beneficiary
   * 3. Unhappy Path
   * 4. Conflicts based on business logic
   */

  @Test
  public void OwtBeneficiaryExemption_SendSEPABeneficiary_Success() {

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
            passcodeAppSecretKey, getToken()).getRight();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
            .build();

    final Response response = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty());
        response.then()
        .statusCode(SC_OK)
        .body("challengeExemptionReason", equalTo("TRUSTED_BENEFICIARY"))
        .body("destination.beneficiaryId", equalTo(beneficiaryId))
        .body("sourceInstrument.id", equalTo(sourceManagedAccountId))
        .body("sourceInstrument.type", equalTo(MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("state", equalTo("SUBMITTED"))
        .body("type", equalTo("SEPA"))
        .body("transferAmount.amount", equalTo(100))
        .body("transferAmount.currency", equalTo(getCurrency()));

    assertOutgoingWireTransferState(response.jsonPath().get("id"), getToken(), State.COMPLETED);
  }

  @Test
  public void OwtBeneficiaryExemption_SendFasterPaymentsBeneficiary_Success() {

    final Pair<String, String> identityDetails =
        createIdentityDetails(getIdentityType(),
            getIdentityType() == IdentityType.CORPORATE ?
                passcodeAppCorporateProfileId : passcodeAppConsumerProfileId,
            Currency.GBP, passcodeAppSecretKey);

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        identityDetails.getLeft(), identityDetails.getRight(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createFasterPaymentsBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
        passcodeAppSecretKey, identityDetails.getRight()).getRight();

    fundManagedAccount(sourceManagedAccountId, identityDetails.getLeft(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(identityDetails.getLeft(), 100L))
            .build();

    final Response response = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, identityDetails.getRight(), Optional.empty());
        response.then()
        .statusCode(SC_OK)
        .body("challengeExemptionReason", equalTo("TRUSTED_BENEFICIARY"))
        .body("destination.beneficiaryId", equalTo(beneficiaryId))
        .body("sourceInstrument.id", equalTo(sourceManagedAccountId))
        .body("sourceInstrument.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.name().toLowerCase()))
        .body("state", equalTo("SUBMITTED"))
        .body("type", equalTo("FASTER_PAYMENTS"))
        .body("transferAmount.amount", equalTo(100))
        .body("transferAmount.currency", equalTo(identityDetails.getLeft()));

    assertOutgoingWireTransferState(response.jsonPath().get("id"), identityDetails.getRight(), State.COMPLETED);
  }

  @Test
  public void OwtBeneficiaryExemption_BeneficiaryBelongsToDifferentIdentity_Conflict() {

    final Pair<String, String> identityDetails =
        createIdentityDetails(getIdentityType(),
            getIdentityType() == IdentityType.CORPORATE ?
                passcodeAppCorporateProfileId : passcodeAppConsumerProfileId,
            Currency.GBP, passcodeAppSecretKey);

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        identityDetails.getLeft(), identityDetails.getRight(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createFasterPaymentsBeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
        passcodeAppSecretKey, getToken()).getRight();

    fundManagedAccount(sourceManagedAccountId, identityDetails.getLeft(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(identityDetails.getLeft(), 100L))
            .build();

    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, identityDetails.getRight(), Optional.empty())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
  }

  @Test
  public void OwtBeneficiaryExemption_BeneficiaryNotFound_Conflict() {

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(RandomStringUtils.randomNumeric(18)).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
            .build();

    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
  }

  @Test
  public void OwtBeneficiaryExemption_BlankBeneficiaryId_BadRequest() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId("").build())
            .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
            .build();

    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("_embedded.errors[0].message", equalTo("request.destinationBeneficiary.beneficiaryId: must match \"^[0-9]+$\""))
        .body("_embedded.errors[1].message", equalTo("request.destinationBeneficiary.beneficiaryId: must not be blank"));
  }

  @Test
  public void OwtBeneficiaryExemption_InstrumentBeneficiaryId_Conflict() {

    // Create destination managed account to be used as beneficiary instrument
    final String beneficiaryManagedAccountId = createManagedAccount(
        getDestinationManagedAccountProfileId(),
        getDestinationCurrency(), getDestinationToken(), passcodeAppSecretKey).getLeft();

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
            getDestinationIdentityName(), beneficiaryManagedAccountId, passcodeAppSecretKey, getToken())
        .getRight();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
            .build();

    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("BENEFICIARY_TYPE_INVALID"));
  }

  @Test
  public void SendBeneficiaryExemption_InvalidBeneficiaryState_Conflict() {
    //enroll identity for authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey,
        getToken());

    // Create destination SEPA Bank Details to be used as beneficiary instruments
    final List<String> listOfInvalidBeneficiaries = new ArrayList<>();
    // Create beneficiary in PENDING_CHALLENGE state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createSEPABeneficiaryInState(
        BeneficiaryState.PENDING_CHALLENGE, getIdentityType(),
        getDestinationIdentityName(), passcodeAppSecretKey,
        getToken()).getRight());
    // Create beneficiary in REMOVED state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createSEPABeneficiaryInState(
        BeneficiaryState.REMOVED, getIdentityType(),
        getDestinationIdentityName(), passcodeAppSecretKey,
        getToken()).getRight());
    // Create beneficiary in CHALLENGE_FAILED state
    listOfInvalidBeneficiaries.add(BeneficiariesHelper.createSEPABeneficiaryInState(
        BeneficiaryState.CHALLENGE_FAILED, getIdentityType(),
        getDestinationIdentityName(), passcodeAppSecretKey,
        getToken()).getRight());

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    listOfInvalidBeneficiaries.forEach(beneficiaryId -> {
      final OutgoingWireTransfersModel outgoingWireTransfersModel =
          OutgoingWireTransfersModel.newBuilder()
              .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
              .setTag(RandomStringUtils.randomAlphabetic(5))
              .setDescription(RandomStringUtils.randomAlphabetic(5))
              .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
              .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
              .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
              .build();

      OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("BENEFICIARY_TYPE_INVALID"));
    });
  }

  @Test
  public void OwtBeneficiaryExemption_InsufficientFunds_FundsInsufficient() {

    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
            getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
            passcodeAppSecretKey, getToken()).getRight();

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
            OutgoingWireTransfersModel.newBuilder()
                    .setProfileId(passcodeAppOutgoingWireTransfersProfileId)
                    .setTag(RandomStringUtils.randomAlphabetic(5))
                    .setDescription(RandomStringUtils.randomAlphabetic(5))
                    .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
                    .setSourceInstrument(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                    .setTransferAmount(new CurrencyAmount(getCurrency(), 100L))
                    .build();

    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, getToken(), Optional.empty())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

    final OutgoingWireTransferResponseModel outgoingWireTransferResponseModel =
            OutgoingWireTransfersHelper.getOwtByAccountAndTag(passcodeAppSecretKey, outgoingWireTransfersModel,  getToken());
    assertEquals(State.INVALID.name(), outgoingWireTransferResponseModel.getState());

    OutgoingWireTransfersHelper.checkOwtValidationFailureById(outgoingWireTransferResponseModel.getId(), "INSUFFICIENT_FUNDS");
  }

  private static Pair<String, String> createIdentityDetails(final IdentityType type,
                                                            final String profileId,
                                                            final Currency currency,
                                                            final String secretKey) {
    if (type == IdentityType.CORPORATE) {
      final CreateCorporateModel corporateDetails =
          CreateCorporateModel.CurrencyCreateCorporateModel(profileId, currency).build();

      final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
          corporateDetails, secretKey);

      return Pair.of(corporateDetails.getBaseCurrency(), authenticatedCorporate.getRight());
    }
    else {
      final CreateConsumerModel consumerDetails =
          CreateConsumerModel.CurrencyCreateConsumerModel(profileId, currency).build();

      final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
          consumerDetails, secretKey);

      return Pair.of(consumerDetails.getBaseCurrency(), authenticatedConsumer.getRight());
    }
  }

  private static void assertOutgoingWireTransferState(final String id, final String token, final State state) {

    TestHelper.ensureAsExpected(120,
            () -> OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id, token),
            x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
            Optional.of(String.format("Expecting 200 with an OWT in state %s, check logged payload", state.name())));
  }
}