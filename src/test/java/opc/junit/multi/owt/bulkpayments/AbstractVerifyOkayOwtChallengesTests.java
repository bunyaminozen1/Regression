package opc.junit.multi.owt.bulkpayments;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
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
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(MultiTags.BULK_OWT)
public abstract class AbstractVerifyOkayOwtChallengesTests extends BaseOutgoingWireTransfersSetup {
  protected abstract String getToken();

  protected abstract String getCurrency();

  protected abstract String getManagedAccountProfileId();

  /**
   * Test cases for create bulk OWTs
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Verify Push (Biometric) Challenges for all valid OWTs
   * 2. Verify Push (Biometric) Challenges for all invalid OWTs
   * 3. Verify Push (Biometric) Challenges for mix of valid and invalid OWTs
   * 4. Conflict based on business logic
   * 5. Unhappy path
   */

  @Test
  public void VerifyOkayPushChallenges_AcceptChallengeAllValidOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    SimulatorHelper.acceptOkayChallenge(passcodeAppSecretKey, scaChallengeId);

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.COMPLETED.name());}
  }

  @Test
  public void VerifyOkayPushChallenges_RejectChallengeAllValidOwt_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    SimulatorHelper.rejectOkayChallenge(passcodeAppSecretKey, scaChallengeId);

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.DECLINED_SCA.name());}
  }

  @Test
  public void VerifyOkayPushChallenges_AcceptOnlyOneChallengeFromSameBulk_Success() {
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

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owts.get(0))),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(0), State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(1), State.REQUIRES_SCA.name());

    SimulatorHelper.acceptOkayChallenge(passcodeAppSecretKey, scaChallengeId);

    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(0), State.COMPLETED.name());
    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(1), State.REQUIRES_SCA.name());
  }

  @Test
  public void VerifyOkayPushChallenges_RejectOnlyOneChallengeFromSameBulk_Success() {
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

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owts.get(0))),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(0), State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(1), State.REQUIRES_SCA.name());

    SimulatorHelper.rejectOkayChallenge(passcodeAppSecretKey, scaChallengeId);

    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(0), State.DECLINED_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(1), State.REQUIRES_SCA.name());
  }

  @Test
  public void VerifyOkayPushChallenges_AcceptChallengeAlreadyCompleted_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    SimulatorHelper.acceptOkayChallenge(passcodeAppSecretKey, scaChallengeId);

    SimulatorService.acceptOkayChallenge(passcodeAppSecretKey, scaChallengeId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("ALREADY_COMPLETED"));
  }

  @Test
  public void VerifyOkayPushChallenges_RejectChallengeAlreadyCompleted_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    SimulatorHelper.rejectOkayChallenge(passcodeAppSecretKey, scaChallengeId);

    SimulatorService.rejectOkayChallenge(passcodeAppSecretKey, scaChallengeId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("ALREADY_COMPLETED"));
  }

  @Test
  public void VerifyOkayPushChallenges_InvalidAcceptScaChallengeId_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    SimulatorService.acceptOkayChallenge(passcodeAppSecretKey, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }

  @Test
  public void VerifyOkayPushChallenges_InvalidRejectScaChallengeId_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
        OwtModelHelper
            .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    List<String> owts = response.jsonPath().getList("response.id");
    for (String owt : owts) {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name());}

    ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken());

    SimulatorService.rejectOkayChallenge(passcodeAppSecretKey, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }
}
