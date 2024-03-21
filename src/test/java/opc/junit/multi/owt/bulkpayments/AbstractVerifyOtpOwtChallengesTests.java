package opc.junit.multi.owt.bulkpayments;

import commons.enums.State;
import io.restassured.response.Response;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import opc.enums.opc.ResourceType;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.OwtModelHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.multi.owt.BaseOutgoingWireTransfersSetup;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.services.multi.ChallengesService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.BULK_OWT)
public abstract class AbstractVerifyOtpOwtChallengesTests extends BaseOutgoingWireTransfersSetup {
  protected abstract String getToken();

  protected abstract String getCurrency();

  protected abstract String getManagedAccountProfileId();

  protected abstract String getDestinationIdentityName();

  protected abstract IdentityType getIdentityType();
  final private String VERIFICATION_CODE = "123456";

  /**
   * Test cases for create bulk OWTs
   * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2263580673
   * Test Plan: https://weavr-payments.atlassian.net/wiki/spaces/ENG/pages/2271412273/E2E+Bulk+Payments+Test+Plan#Create-Bulk-OWT
   * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5022
   *
   * The main cases:
   * 1. Verify OTP Challenges for all valid OWTs
   * 2. Verify OTP Challenges for mix of valid and invalid OWTs
   * 3. Conflict based on business logic
   * 4. Unhappy path
   */

  @Test
  public void VerifyOtpChallenges_AllValidOwt_Success() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
            .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.COMPLETED.name());}
  }

  @Test
  public void VerifyOtpChallenges_VerifyOnlyOneChallengeFromSameBulk_Success() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
            EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.COMPLETED.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());
  }

  @Test
  public void VerifyOtpChallenges_InvalidVerificationCode_Conflict() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, RandomStringUtils.randomNumeric(6)),
        scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }

  @Test
  public void VerifyOtpChallenges_ChallengeAlreadyCompleted_Conflict() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @Test
  public void VerifyOtpChallenges_BadResourceType_NotFound() {
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

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owts.get(0))),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(0), State.PENDING_SCA.name());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
            .verifyChallengesModel(ResourceType.SENDS, VERIFICATION_CODE),
        scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void VerifyOtpChallenges_InvalidScaChallengeId_NotFound() {
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

    ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owts.get(0))),
        EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    OutgoingWireTransfersHelper.checkOwtStateById(owts.get(0), State.PENDING_SCA.name());

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void VerifyOtpChallenges_NoApiKey_BadRequest() {

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "", getToken())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void VerifyOtpChallenges_InvalidApiKey_Unauthorized() {

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), "abc", getToken())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void VerifyOtpChallenges_InvalidToken_Unauthorized() {

    ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            RandomStringUtils.randomNumeric(18), EnrolmentChannel.SMS.name(), passcodeAppSecretKey, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void VerifyOtpChallenges_InsufficientFunds_Failed() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
            getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
            OwtModelHelper
                    .createOwtBulkPayments(2, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                            getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    final List<String> owts = response.jsonPath().getList("response.id");
    owts.forEach(owt -> OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name()));

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
                    .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                    .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    owts.forEach(owt -> {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.FAILED.name());
      OutgoingWireTransfersHelper.checkOwtValidationFailureById(owt, "INSUFFICIENT_FUNDS");
    });
  }

  @Test
  public void VerifyOtpChallenges_SingleOwtInsufficientFunds_Failed() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
            getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(
            OwtModelHelper
                    .createOwtBulkPayments(1, passcodeAppOutgoingWireTransfersProfileId, sourceManagedAccountId,
                            getCurrency(), 100L, OwtType.SEPA), passcodeAppSecretKey, getToken(), Optional.empty());
    response.then().statusCode(SC_OK);

    final List<String> owts = response.jsonPath().getList("response.id");
    owts.forEach(owt -> OutgoingWireTransfersHelper.checkOwtStateById(owt, State.REQUIRES_SCA.name()));

    final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper
                    .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                    .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
            scaChallengeId, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, getToken());

    owts.forEach(owt -> {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.FAILED.name());
      OutgoingWireTransfersHelper.checkOwtValidationFailureById(owt, "INSUFFICIENT_FUNDS");
    });
  }

  @Test
  public void VerifyOtpChallenges_InsufficientFundsBulkBeneficiaryOwt_Failed() {
    final String sourceManagedAccountId = createManagedAccount(getManagedAccountProfileId(),
            getCurrency(), getToken(), passcodeAppSecretKey).getLeft();

    // Create beneficiary in ACTIVE state
    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState(
            BeneficiaryState.ACTIVE, getIdentityType(), getDestinationIdentityName(),
            passcodeAppSecretKey, getToken()).getRight();

    final OutgoingWireTransfersModel beneficiaryOwtModel =
            OutgoingWireTransfersModel.BeneficiaryOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                    sourceManagedAccountId, beneficiaryId, getCurrency(), 100L).build();

    final Response response = OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper.createOwtBulkPayments(2,beneficiaryOwtModel),
            passcodeAppSecretKey, getToken(), Optional.empty());

    final List<String> owts = response.jsonPath().getList("response.id");

    owts.forEach(owt -> {
      OutgoingWireTransfersHelper.checkOwtStateById(owt, State.FAILED.name());
      OutgoingWireTransfersHelper.checkOwtValidationFailureById(owt, "INSUFFICIENT_FUNDS");
    });
  }
}