package opc.junit.multi.sends.bulkpayments;

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
import opc.enums.opc.ResourceType;
import commons.enums.State;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.SendModelHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.multi.sends.BaseSendsSetup;
import opc.services.multi.ChallengesService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(MultiTags.BULK_SENDS)
public abstract class AbstractVerifyOtpSendChallengesTests extends BaseSendsSetup {
  protected abstract String getToken();

  protected abstract String getCurrency();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationToken();

  protected abstract String getDestinationCurrency();

  final private String VERIFICATION_CODE = "123456";

  /**
   * Test cases for create bulk Sends
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Verify OTP Challenges for all valid Sends
   * 2. Verify OTP Challenges for mix of valid and invalid Sends
   * 3. Conflict based on business logic
   * 4. Unhappy path
   */

  @Test
  public void VerifyOtpChallenges_AllValidSend_Success() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.COMPLETED.name());}
  }

  @Test
  public void VerifyOtpChallenges_VerifyOnlyOneChallengeFromSameBulk_Success() {
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
    final String firstSend = sends.get(0);
    final String secondSend = sends.get(1);

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(firstSend)),
        EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    SendsHelper.checkSendStateById(firstSend, State.PENDING_SCA.name());
    SendsHelper.checkSendStateById(secondSend, State.REQUIRES_SCA.name());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    SendsHelper.checkSendStateById(firstSend, State.COMPLETED.name());
    SendsHelper.checkSendStateById(secondSend, State.REQUIRES_SCA.name());
  }

  @Test
  public void VerifyOtpChallenges_InvalidVerificationCode_Conflict() {
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
    final String firstSend = sends.get(0);
    final String secondSend = sends.get(1);

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.REQUIRES_SCA.name());}

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(firstSend)),
        EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    SendsHelper.checkSendStateById(firstSend, State.PENDING_SCA.name());
    SendsHelper.checkSendStateById(secondSend, State.REQUIRES_SCA.name());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.SENDS, RandomStringUtils.randomNumeric(6)),
        scaChallengeId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }

  @Test
  public void VerifyOtpChallenges_ChallengeAlreadyCompleted_Conflict() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
            scaChallengeId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void VerifyOtpChallenges_BadResourceType_NotFound() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.PENDING_SCA.name());}

  }

  @Test
  public void VerifyOtpChallenges_InvalidScaChallengeId_NotFound() {
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

    ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.SENDS, sends),
        EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void VerifyOtpChallenges_NoApiKey_BadRequest() {

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void VerifyOtpChallenges_InvalidApiKey_Unauthorized() {

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void VerifyOtpChallenges_InvalidToken_Unauthorized() {

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}
