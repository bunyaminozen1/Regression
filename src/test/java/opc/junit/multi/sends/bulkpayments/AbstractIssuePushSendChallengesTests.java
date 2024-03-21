package opc.junit.multi.sends.bulkpayments;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import opc.enums.opc.ResourceType;
import commons.enums.State;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.SendModelHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.multi.sends.BaseSendsSetup;
import opc.models.multi.sends.BulkSendFundsModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.ChallengesService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag(MultiTags.BULK_SENDS)
public abstract class AbstractIssuePushSendChallengesTests extends BaseSendsSetup {
  protected abstract String getToken();

  protected abstract String getIdentityId();

  protected abstract String getCurrency();

  protected abstract String getPrepaidManagedCardProfileId();

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
   * 1. Issue Push (Authy/Biometric) Challenges for all valid Sends
   * 2. Issue Push (Authy/Biometric) Challenges for all invalid Sends
   * 3. Issue Push (Authy/Biometric) Challenges for mix of valid and invalid Sends
   * 4. Conflict based on business logic
   * 5. Unhappy path
   */

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AllValidSend_Success(PushEnrolmentChannel pushEnrolmentChannel) {

    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

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


    ChallengesService.issuePushChallenges(
            ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, sends),
            pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK);

    for (String send : sends) {SendsHelper.checkSendStateById(send, State.PENDING_SCA.name());}
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AllInvalidSend_Conflict(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

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

    response.then().statusCode(SC_OK);

    List<String> sends = response.jsonPath().getList("response.id");
    for (String send : sends) {SendsHelper.checkSendStateById(send, State.INVALID.name());}

    ChallengesService.issuePushChallenges(ChallengesModelHelper
                .issueChallengesModel(ResourceType.SENDS, sends),
            pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_IssueOnlyOneChallengeFromSameBulk_Success(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

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

    final String firstSend = sends.get(0);
    final String secondSend = sends.get(1);

    ChallengesService.issuePushChallenges(ChallengesModelHelper
                .issueChallengesModel(ResourceType.SENDS, List.of(firstSend)),
            pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK);

    SendsHelper.checkSendStateById(firstSend, State.PENDING_SCA.name());
    SendsHelper.checkSendStateById(secondSend, State.REQUIRES_SCA.name());
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_BadResourceType_NotFound(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

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

    ChallengesService.issuePushChallenges(
            ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, sends),
            pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_MixOfValidAndInvalidSend_NotFound(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

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

    sends.add(2, RandomStringUtils.randomNumeric(18));

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, sends),
        pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AlreadyIssued_Conflict(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

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

    final String firstSend = sends.get(0);
    final String secondSend = sends.get(1);

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(firstSend)),
            pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_OK);

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(firstSend)),
            pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("STATE_INVALID"));

    SendsHelper.checkSendStateById(firstSend, State.PENDING_SCA.name());
    SendsHelper.checkSendStateById(secondSend, State.REQUIRES_SCA.name());
  }

  @ParameterizedTest
  @EnumSource(PushEnrolmentChannel.class)
  public void IssuePushChallenges_AllNonExistent_NotFound(PushEnrolmentChannel pushEnrolmentChannel) {
    // Enroll identity for push
    if (pushEnrolmentChannel == PushEnrolmentChannel.AUTHY) {
      AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKeyScaSendsApp, getToken());
    } else {
      SecureHelper.enrolAndVerifyBiometric(getIdentityId(), sharedKeyScaSendsApp, secretKeyScaSendsApp, getToken());
    }

    ChallengesService.issuePushChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.SENDS, List.of(
            RandomStringUtils.randomNumeric(18),RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18))),
        pushEnrolmentChannel.name(), secretKeyScaSendsApp, getToken()).then().statusCode(SC_NOT_FOUND);
  }

  private enum PushEnrolmentChannel {
    AUTHY,
    BIOMETRIC,
  }
}
