package opc.junit.multi.sends.bulkpayments;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.ResourceType;
import commons.enums.State;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.SendModelHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.multi.sends.BaseSendsSetup;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(MultiTags.BULK_SENDS)
public abstract class AbstractVerifyOkaySendChallengesTests extends BaseSendsSetup {
  protected abstract String getToken();

  protected abstract String getCurrency();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationToken();

  protected abstract String getDestinationCurrency();


  /**
   * Test cases for create bulk Sends
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Verify Push (Biometric) Challenges for all valid Sends
   * 2. Verify Push (Biometric) Challenges for all invalid Sends
   * 3. Verify Push (Biometric) Challenges for mix of valid and invalid Sends
   * 4. Conflict based on business logic
   * 5. Unhappy path
   */

  @Test
  public void VerifyOkayPushChallenges_AcceptChallengeAllValidSend_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {
      SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SimulatorHelper.acceptOkayChallenge(secretKeyScaSendsApp, scaChallengeId);

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.COMPLETED.name());}
  }

  @Test
  public void VerifyOkayPushChallenges_RejectChallengeAllValidSend_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SimulatorHelper.rejectOkayChallenge(secretKeyScaSendsApp, scaChallengeId);

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.DECLINED_SCA.name());}
  }

  @Test
  public void VerifyOkayPushChallenges_AcceptOnlyOneChallengeFromSameBulk_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(sends.get(0))),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SendsHelper.checkSendStateById(sends.get(0), State.PENDING_SCA.name());
    SendsHelper.checkSendStateById(sends.get(1), State.REQUIRES_SCA.name());

    SimulatorHelper.acceptOkayChallenge(secretKeyScaSendsApp, scaChallengeId);

    SendsHelper.checkSendStateById(sends.get(0), State.COMPLETED.name());
    SendsHelper.checkSendStateById(sends.get(1), State.REQUIRES_SCA.name());
  }

  @Test
  public void VerifyOkayPushChallenges_RejectOnlyOneChallengeFromSameBulk_Success() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(sends.get(0))),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SendsHelper.checkSendStateById(sends.get(0), State.PENDING_SCA.name());
    SendsHelper.checkSendStateById(sends.get(1), State.REQUIRES_SCA.name());

    SimulatorHelper.rejectOkayChallenge(secretKeyScaSendsApp, scaChallengeId);

    SendsHelper.checkSendStateById(sends.get(0), State.DECLINED_SCA.name());
    SendsHelper.checkSendStateById(sends.get(1), State.REQUIRES_SCA.name());
  }

  @Test
  public void VerifyOkayPushChallenges_AcceptChallengeAlreadyCompleted_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SimulatorHelper.acceptOkayChallenge(secretKeyScaSendsApp, scaChallengeId);

    SimulatorService.acceptOkayChallenge(secretKeyScaSendsApp, scaChallengeId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("ALREADY_COMPLETED"));
  }

  @Test
  public void VerifyOkayPushChallenges_RejectChallengeAlreadyCompleted_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SimulatorHelper.rejectOkayChallenge(secretKeyScaSendsApp, scaChallengeId);

    SimulatorService.rejectOkayChallenge(secretKeyScaSendsApp, scaChallengeId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("ALREADY_COMPLETED"));
  }

  @Test
  public void VerifyOkayPushChallenges_InvalidAcceptScaChallengeId_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SimulatorService.acceptOkayChallenge(secretKeyScaSendsApp, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }

  @Test
  public void VerifyOkayPushChallenges_InvalidRejectScaChallengeId_Conflict() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getCurrency(), secretKeyScaSendsApp, getToken()).getLeft();

    final String destinationManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
        getDestinationCurrency(), secretKeyScaSendsApp, getDestinationToken()).getLeft();

    fundManagedAccount(sourceManagedAccountId, getCurrency(), 10000L);

    final Response response = SendsService
        .bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdScaSendsApp,
                sourceManagedAccountId, destinationManagedAccountId, getCurrency(), 100L),
            secretKeyScaSendsApp, getToken(), Optional.empty());

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    ChallengesHelper.issuePushChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.BIOMETRIC.name(), secretKeyScaSendsApp, getToken());

    SimulatorService.rejectOkayChallenge(secretKeyScaSendsApp, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }
}
