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
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.secure.SecureHelper;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag(MultiTags.BULK_OWT)
public abstract class AbstractIssuePushOwtChallengesTests extends BaseOutgoingWireTransfersSetup {
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
   * 1. Issue Push (Authy/Biometric) Challenges for all valid OWTs
   * 2. Issue Push (Authy/Biometric) Challenges for all invalid OWTs
   * 3. Issue Push (Authy/Biometric) Challenges for mix of valid and invalid OWTs
   * 4. Conflict based on business logic
   * 5. Unhappy path
   */

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AllValidOwt_Success(PushEnrolmentChannel pushEnrolmentChannel) {

    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK);

    for (String owt : owts) {OutgoingWireTransfersHelper.checkOwtStateById(owt, State.PENDING_SCA.name());}
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AllInvalidOwt_Conflict(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper
                .issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_IssueOnlyOneChallengeFromSameBulk_Success(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
            pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK);

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_BadResourceType_NotFound(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, owts),
        pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_MixOfValidAndInvalidOWTs_NotFound(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

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

    ChallengesService.issuePushChallenges((ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts)),
        pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AlreadyIssued_Conflict(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
            pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_OK);

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(firstOwt)),
            pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));

    OutgoingWireTransfersHelper.checkOwtStateById(firstOwt, State.PENDING_SCA.name());
    OutgoingWireTransfersHelper.checkOwtStateById(secondOwt, State.REQUIRES_SCA.name());
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AllNonExistent_NotFound(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());
    }

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(
            RandomStringUtils.randomNumeric(18),RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18))),
        pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_NullPayload_BadRequest(PushEnrolmentChannel enrolmentChannel) {
    ChallengesService.issuePushChallenges(
            ChallengesModel.builder().resourceIds(null).resourceType(null).build(),
            enrolmentChannel.name(), passcodeAppSecretKey, getToken()).then().statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("Bad Request"))
        .body("_embedded.errors[0].message", equalTo("request.resourceIds: must not be null"))
        .body("_embedded.errors[1].message", equalTo("request.resourceType: must not be null"));
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_IdentityNotEnrolledForPush_Conflict(PushEnrolmentChannel pushEnrolmentChannel) {
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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            pushEnrolmentChannel.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void IssuePushChallenges_AuthyIdentityNotEnrolledForBiometric_Conflict() {
    //Enroll identity for Authy
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), passcodeAppSecretKey, getToken());

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void IssuePushChallenges_BiometricIdentityNotEnrolledForAuthy_Conflict() {
    //Enroll identity for Biometric
    SecureHelper.enrolAndVerifyBiometric(getIdentityId(), passcodeAppSharedKey, passcodeAppSecretKey, getToken());

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

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, owts),
            EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void IssuePushChallenges_NoApiKey_BadRequest() {
    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.AUTHY.name(), "", getToken()).then().statusCode(SC_BAD_REQUEST);

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.BIOMETRIC.name(), "", getToken()).then().statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void IssuePushChallenges_InvalidApiKey_Unauthorized() {
    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.AUTHY.name(), "abc", getToken()).then().statusCode(SC_UNAUTHORIZED);

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.BIOMETRIC.name(), "abc", getToken()).then().statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void IssuePushChallenges_InvalidToken_Unauthorized() {
    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, "").then().statusCode(SC_UNAUTHORIZED);

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS,
            List.of(RandomStringUtils.randomNumeric(18))),
        EnrolmentChannel.BIOMETRIC.name(), passcodeAppSecretKey, "").then().statusCode(SC_UNAUTHORIZED);
  }

  private enum PushEnrolmentChannel {
    AUTHY,
    BIOMETRIC,
  }
}