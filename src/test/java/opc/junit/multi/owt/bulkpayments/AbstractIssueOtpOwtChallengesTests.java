package opc.junit.multi.owt.bulkpayments;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.enums.opc.ResourceType;
import commons.enums.State;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.OwtModelHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.models.multi.challenges.ChallengesModel;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.services.multi.ChallengesService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(MultiTags.BULK_OWT)
public abstract class AbstractIssueOtpOwtChallengesTests extends BaseOutgoingWireTransfersSetup {

  protected abstract String getToken();

  protected abstract String getIdentityId();

  protected abstract String getCurrency();

  protected abstract String getManagedAccountProfileId();

  /**
   * Test cases for create bulk OWTs
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Issue OTP Challenges for all valid OWTs
   * 2. Issue OTP Challenges for all invalid OWTs
   * 3. Issue OTP Challenges for mix of valid and invalid OWTs
   * 4. Conflict based on business logic
   * 5. Unhappy path
   */

  @Test
  public void IssueOtpChallenges_AllValidOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK);

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.PENDING_SCA.name());}
  }

  @Test
  public void IssueOtpChallenges_AllInvalidOwt_Conflict() {
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

    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.INVALID.name());}

    ChallengesService.issueOtpChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void IssueOtpChallenges_IssueOnlyOneChallengeFromSameBulk_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    final String firstOwt = owts.get(0);
    final String secondOwt = owts.get(1);

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK);

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());
  }

  @Test
  public void IssueOtpChallenges_BadResourceType_NotFound() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, owts),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @Test
  public void IssueOtpChallenges_MixOfValidAndInvalidOWTs_NotFound() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    owts.add(2, RandomStringUtils.randomNumeric(18));

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @Test
  public void IssueOtpChallenges_AlreadyIssued_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    final String firstOwt = owts.get(0);
    final String secondOwt = owts.get(1);

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
            EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK);

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
            EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());
  }

  @Test
  public void IssueOtpChallenges_AllNonExistent_NotFound() {
    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(
            RandomStringUtils.randomNumeric(18),RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @Test
  public void IssueOtpChallenges_NullPayload_BadRequest() {
    ChallengesService.issueOtpChallenges(ChallengesModel.builder().resourceIds(null).resourceType(null).build(),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("Bad Request"))
        .body("_embedded.errors[0].message", equalTo("request.resourceIds: must not be null"))
        .body("_embedded.errors[1].message", equalTo("request.resourceType: must not be null"));
  }

  @Test
  public void IssueOtpChallenges_NoApiKey_BadRequest() {
    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.SMS.name(), "", getToken()).then().statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void IssueOtpChallenges_InvalidApiKey_Unauthorized() {
    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.SMS.name(), "abc", getToken()).then().statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void IssueOtpChallenges_InvalidToken_Unauthorized() {
    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, "").then().statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void IssueOtpChallenges_InsufficientFunds_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
            getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
            OwtModelHelper
                    .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                            getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    final List<String> owts = response.jsonPath().getList("response.id");
    owts.forEach(owt -> OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name()));

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
                    EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
            .then()
            .statusCode(SC_OK);

    owts.forEach(owt -> OutgoingWireTransfersHelper.checkOwtStateById(owt, State.PENDING_SCA.name()));
  }

  @Test
  public void IssueOtpChallenges_SingleOwtInsufficientFunds_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
            getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
            OwtModelHelper
                    .createOwtBulkPayments(1, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                            getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    final List<String> owts = response.jsonPath().getList("response.id");
    owts.forEach(owt -> OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name()));

    ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
                    EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
            .then()
            .statusCode(SC_OK);

    owts.forEach(owt -> OutgoingWireTransfersHelper.checkOwtStateById(owt, State.PENDING_SCA.name()));
  }
}
