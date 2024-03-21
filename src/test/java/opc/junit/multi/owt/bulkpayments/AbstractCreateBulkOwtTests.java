package opc.junit.multi.owt.bulkpayments;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.helpers.OwtModelHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(MultiTags.BULK_OWT)
public abstract class AbstractCreateBulkOwtTests extends BaseOutgoingWireTransfersSetup {

  protected abstract String getToken();

  protected abstract String getIdentityId();

  protected abstract String getCurrency();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationIdentityName();

  protected abstract IdentityType getIdentityType();

  /**
   * Test cases for create bulk OWTs
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Create All Valid Non Exempted Bulk OWTs
   * 2. Create All Valid Exempted Bulk OWTs
   * 3. Create All Invalid Bulk OWTs
   * 4. Create Mix of valid and invalid OWT
   * 5. Create Mix of exempted and non exempted OWTs
   * 6. Unhappy Path
   */

  @Test
  public void CreateBulkOwt_AllValidOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[0].transferAmount.amount", equalTo(100))
        .body("response[0].transferAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[1].transferAmount.amount", equalTo(100))
        .body("response[1].transferAmount.currency", equalTo(getCurrency()));

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    // Expect all the owts in bulk to be in state PENDING_CHALLENGE (No exemptions)
    OutgoingWireTransfersHelper.getOutgoingWireTransfers(passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("transfer[0].state", equalTo("PENDING_CHALLENGE"))
        .body("transfer[1].state", equalTo("PENDING_CHALLENGE"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkOwt_AllValidBeneficiaryOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
        passcodeAppSecretKey, getToken()).getRight();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel beneficiaryOwtModel =
        OutgoingWireTransfersModel.BeneficiaryOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, beneficiaryId, getCurrency(), 100L).build();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper.createOwtBulkPayments(2,beneficiaryOwtModel),
        passcodeAppSecretKey, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].destinationBeneficiary.beneficiaryId", equalTo(beneficiaryId))
        .body("response[0].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[0].transferAmount.amount", equalTo(100))
        .body("response[0].transferAmount.currency", equalTo(getCurrency()))
        .body("response[1].destinationBeneficiary.beneficiaryId", equalTo(beneficiaryId))
        .body("response[1].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[1].transferAmount.amount", equalTo(100))
        .body("response[1].transferAmount.currency", equalTo(getCurrency()));

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.COMPLETED.name());}

    // Since all the OWTs in bulk request are exempted from SCA (Trusted Beneficiaries), we expect them to be processed (COMPLETED)
    OutgoingWireTransfersHelper.getOutgoingWireTransfers(passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("transfer[0].state", equalTo("COMPLETED"))
        .body("transfer[1].state", equalTo("COMPLETED"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkOwt_AllInvalidOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel invalidBeneficiaryOwtModel =
        OutgoingWireTransfersModel.BeneficiaryOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, RandomStringUtils.randomNumeric(18), getCurrency(), 100L).build();

    final OutgoingWireTransfersModel invalidSourceOwtModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            RandomStringUtils.randomNumeric(18), getCurrency(), 100L, OwtType.SEPA).build();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        BulkOutgoingWireTransfersModel.builder().outgoingWireTransfers(List.of(invalidBeneficiaryOwtModel, invalidSourceOwtModel)).build(),
        passcodeAppSecretKey, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[0].transferAmount.amount", equalTo(100))
        .body("response[0].transferAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[1].transferAmount.amount", equalTo(100))
        .body("response[1].transferAmount.currency", equalTo(getCurrency()));

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.INVALID.name());}

    // Expect All the OWTs to be in state INVALID
    OutgoingWireTransfersHelper.getOutgoingWireTransfers(passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("transfer[0].state", equalTo("INVALID"))
        .body("transfer[1].state", equalTo("INVALID"))
        .body("count", equalTo(2))
        .body("responseCount", equalTo(2));
  }

  @Test
  public void CreateBulkOwt_MixOfValidAndInvalidOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel validOwtModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("validOwt").build();

    final OutgoingWireTransfersModel invalidOwtModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            RandomStringUtils.randomNumeric(18), getCurrency(), 100L, OwtType.SEPA).setTag("invalidOwt").build();


    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
        .outgoingWireTransfers(List.of(validOwtModel, invalidOwtModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[0].transferAmount.amount", equalTo(100))
        .body("response[0].transferAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[1].transferAmount.amount", equalTo(100))
        .body("response[1].transferAmount.currency", equalTo(getCurrency()));

    final String validOwtId = getOwtIdViaTag(response, "validOwt").get(0);
    final String invalidOwtId = getOwtIdViaTag(response, "invalidOwt").get(0);
    OutgoingWireTransfersHelper.checkOwtStateById(validOwtId, State.REQUIRES_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(invalidOwtId, State.INVALID.name());

    // Expect the valid OWT to be in state PENDING_CHALLENGE, and the invalid to be in state INVALID
    OutgoingWireTransfersHelper.getOutgoingWireTransfer(passcodeAppSecretKey, validOwtId, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));

    OutgoingWireTransfersHelper.getOutgoingWireTransfer(passcodeAppSecretKey, invalidOwtId, getToken())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("INVALID"));
  }

  @Test
  public void CreateBulkOwt_SingleOwtNotExempted_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
        BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
        passcodeAppSecretKey, getToken()).getRight();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel beneficiaryOwtModel =
        OutgoingWireTransfersModel.BeneficiaryOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, beneficiaryId, getCurrency(), 100L).build();

    final OutgoingWireTransfersModel normalModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).build();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
        .outgoingWireTransfers(List.of(beneficiaryOwtModel, beneficiaryOwtModel, normalModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty());

    response
        .then()
        .statusCode(SC_OK)
        .body("response[0].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[0].transferAmount.amount", equalTo(100))
        .body("response[0].transferAmount.currency", equalTo(getCurrency()))
        .body("response[1].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[1].transferAmount.amount", equalTo(100))
        .body("response[1].transferAmount.currency", equalTo(getCurrency()))
        .body("response[2].profileId", equalTo(passcodeAppOutgoingWireTransfersProfileId))
        .body("response[2].transferAmount.amount", equalTo(100))
        .body("response[2].transferAmount.currency", equalTo(getCurrency()));

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    // Since we have a single owt model that does not exempt it from SCA, all the owts in bulk are in PENDING_CHALLENGE state
    OutgoingWireTransfersHelper.getOutgoingWireTransfers(passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK)
        .body("transfer[0].state", equalTo("PENDING_CHALLENGE"))
        .body("transfer[1].state", equalTo("PENDING_CHALLENGE"))
        .body("transfer[2].state", equalTo("PENDING_CHALLENGE"))
        .body("count", equalTo(3))
        .body("responseCount", equalTo(3));
  }

  @Test
  public void CreateBulkOwt_TransactionLimitExceeded_Conflict() {
    // Current Bulk Transaction Limit for OWT
    final int transactionLimit = 1000;
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(transactionLimit + 1, passcodeAppOutgoingWireTransfersProfileId,
                sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(),
        Optional.empty())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("TRANSACTION_LIMIT_EXCEEDED"));
  }

  @Test
  public void CreateBulkOwt_InvalidPayload_BadRequest() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel validOwtModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("validOwt").build();

    final OutgoingWireTransfersModel invalidSyntaxModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            RandomStringUtils.randomAlphabetic(18), getCurrency(), 100L, OwtType.SEPA).build();

    // Even if one owt model has invalid syntax, we expect a 400
    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
            .outgoingWireTransfers(List.of(validOwtModel, invalidSyntaxModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("request.outgoingWireTransfers[1].sourceInstrument.id: must match \"^[0-9]+$\""));
  }

  @Test
  public void CreateBulkOwt_SepaMissingDescriptionField_BadRequest() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final OutgoingWireTransfersModel validOwtModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA).setTag("validOwt").build();

    final OutgoingWireTransfersModel invalidSyntaxModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
            sourceManagedAccountId, getCurrency(), 100L, OwtType.SEPA)
        .setDescription(null).build();

    // Even if one owt model has invalid syntax, we expect a 400
    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
            .outgoingWireTransfers(List.of(validOwtModel, invalidSyntaxModel)).build(), passcodeAppSecretKey, getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("request: 1: description size must be between 1 and 35"));
  }

  @Test
  public void CreateBulkOwt_NullPayload_BadRequest() {
    // Even if one owt model has invalid syntax, we expect a 400
    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
            .outgoingWireTransfers(null).build(), passcodeAppSecretKey, getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("request.outgoingWireTransfers: must not be null"));
  }

  @Test
  public void CreateBulkOwt_NoApiKey_BadRequest() {
    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
            .outgoingWireTransfers(null).build(), "", getToken(), Optional.empty())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void CreateBulkOwt_InvalidApiKey_Unauthorized() {
    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
            .outgoingWireTransfers(null).build(), "abc", getToken(), Optional.empty())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void CreateBulkOwt_InvalidToken_Unauthorized() {
    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
            .outgoingWireTransfers(null).build(), passcodeAppSecretKey, "", Optional.empty())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  private List<String> getOwtIdViaTag(final Response response,
                                      final String tag) {
    return response.path("response.findAll {owt -> owt.tag == '"+ tag +"'}.id");
  }
}
