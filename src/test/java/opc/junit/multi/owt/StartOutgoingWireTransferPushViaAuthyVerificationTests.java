package opc.junit.multi.owt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.enums.Currency;
import commons.enums.State;
import commons.models.MobileNumberModel;
import opc.enums.authy.AuthyMessage;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.OwtType;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.AuthyStartVerificationNotificationModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.multi.users.UsersModel;
import opc.services.admin.AdminService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.OWT)
public class StartOutgoingWireTransferPushViaAuthyVerificationTests extends
    BaseOutgoingWireTransfersSetup {

  private static final String CHANNEL = EnrolmentChannel.AUTHY.name();

  private static String applicationOneCorporateAuthenticationToken;
  private static String applicationOneConsumerAuthenticationToken;
  private static String applicationOneCorporateManagedAccountId;
  private static String applicationOneConsumerManagedAccountId;
  private static String applicationFourCorporateAuthenticationToken;
  private static String applicationFourConsumerAuthenticationToken;
  private static String applicationFourCorporateManagedAccountId;
  private static String applicationFourConsumerManagedAccountId;

  @BeforeAll
  public static void Setup() {
    final String adminToken = AdminService.loginAdmin();
    final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

    AdminHelper.resetProgrammeAuthyLimitsCounter(applicationOne.getProgrammeId(), adminToken);
    AdminHelper.resetProgrammeAuthyLimitsCounter(applicationFour.getProgrammeId(), adminToken);
    AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount,
        adminToken);
    AdminHelper.setProgrammeAuthyChallengeLimit(applicationFour.getProgrammeId(), resetCount,
        adminToken);

    applicationOneCorporateSetup();
    applicationOneConsumerSetup();
    applicationFourCorporateSetup();
    applicationFourConsumerSetup();
  }

  @Test
  public void StartVerification_Corporate_Success() {

    final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId,
        applicationOneCorporateAuthenticationToken);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            applicationOneCorporateAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id,
            applicationOneCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_Consumer_Success() {

    final String id = sendOutgoingWireTransfer(applicationOneConsumerManagedAccountId,
        applicationOneConsumerAuthenticationToken);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            applicationOneConsumerAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id,
            applicationOneConsumerAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_AuthenticatedUser_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
        applicationOneCorporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey, user.getRight());

    final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId,
        user.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            user.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_RootStartUserVerification_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
        applicationOneCorporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey, user.getRight());

    final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId,
        user.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            applicationOneCorporateAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_UserStartRootVerification_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
        applicationOneCorporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey, user.getRight());

    final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId,
        applicationOneCorporateAuthenticationToken);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            user.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id,
            applicationOneCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @ParameterizedTest
  @MethodSource("owtSuccessfulArgs")
  public void StartVerification_NotificationCheck_Success(final Currency currency,
      final OwtType type)
      throws SQLException, JsonProcessingException {

    final long owtAmount = 100L;

    final Pair<String, CreateManagedAccountModel> managedAccount =
        createManagedAccount(consumerManagedAccountProfileId, currency.name(),
            applicationOneConsumerAuthenticationToken);

    fundManagedAccount(managedAccount.getLeft(), currency.name(), 10000L);

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
            managedAccount.getLeft(),
            currency.name(), owtAmount, type).build();

    final String id =
        OutgoingWireTransfersService
            .sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                applicationOneConsumerAuthenticationToken, Optional.empty())
            .then().statusCode(SC_OK).extract().jsonPath().getString("id");

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            applicationOneConsumerAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    final Map<String, String> notification = AuthySimulatorDatabaseHelper.getNotification(
        applicationOne.getProgrammeId()).get(0);
    assertEquals(String.format(AuthyMessage.PAYMENT_INITIATION.getMessage(),
        applicationOne.getProgrammeName()), notification.get("message"));

    AuthyStartVerificationNotificationModel keyValuePair =
        new ObjectMapper().readValue(notification.get("details"),
            AuthyStartVerificationNotificationModel.class);

    assertEquals(new DecimalFormat("#0.00").format(owtAmount / 100), keyValuePair.getAmount());
    assertEquals(getCurrencySymbol(type), keyValuePair.getCurrency());
    assertEquals("Bank Transfer", keyValuePair.getTransactionType());
    assertEquals(outgoingWireTransfersModel.getDestinationBeneficiary().getName(),
        keyValuePair.getTo());

    switch (type) {
      case SEPA:
        final SepaBankDetailsModel sepaBankDetails =
            (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary()
                .getBankAccountDetails();

        assertEquals(sepaBankDetails.getIban(), keyValuePair.getIban());
        assertEquals(sepaBankDetails.getBankIdentifierCode(), keyValuePair.getBic());
        break;

      case FASTER_PAYMENTS:
        final FasterPaymentsBankDetailsModel fasterPaymentsBankDetails =
            (FasterPaymentsBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary()
                .getBankAccountDetails();

        assertEquals(fasterPaymentsBankDetails.getAccountNumber(), keyValuePair.getAccountNumber());
        assertEquals(fasterPaymentsBankDetails.getSortCode(), keyValuePair.getSortCode());
        break;

      default:
        throw new IllegalArgumentException("OWT type not supported");
    }

  }

  @Test
  public void StartVerification_OwtSentBySmsUserNotEnrolledWithAuthy_ChannelNotRegistered() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
            .setBaseCurrency(Currency.EUR.name())
            .build();
    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        secretKey, corporate.getRight());

    final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
            .DefaultCreateManagedAccountModel(
                applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(),
                createCorporateModel.getBaseCurrency()).build();
    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey,
            corporate.getRight());

    fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), 10000L);

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey,
            corporate.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_OwtSentByOkayUserNotEnrolledWithAuthy_ChannelNotRegistered() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();
    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
            passcodeAppSecretKey);

    SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey,
        passcodeAppSecretKey, corporate.getRight());

    final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
            .DefaultCreateManagedAccountModel(passcodeAppCorporateManagedAccountProfileId,
                createCorporateModel.getBaseCurrency()).build();
    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, passcodeAppSecretKey,
            corporate.getRight());

    fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), 10000L);

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight(),
        passcodeAppSecretKey, passcodeAppOutgoingWireTransfersProfileId);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id,
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_UserEnrolmentNotVerified_UserNotEnrolledOnChallenge() {

    final Pair<String, String> consumer =
        ConsumersHelper.createAuthenticatedVerifiedConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);
    AuthenticationFactorsHelper.enrolAuthyPushUser(secretKey, consumer.getRight());

    final String managedAccountId = createFundedManagedAccount(consumerManagedAccountProfileId,
        consumer.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, consumer.getRight());

    OutgoingWireTransfersService
        .startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, consumer.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void StartVerification_UserEnrolmentRejected_ChannelNotRegistered() {

    final Pair<String, String> consumer =
        ConsumersHelper.createAuthenticatedVerifiedConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);
    AuthenticationFactorsHelper.enrolAuthyPushUser(secretKey, consumer.getRight());

    SimulatorHelper.rejectAuthyIdentity(secretKey, consumer.getLeft(), consumer.getRight(),
        State.INACTIVE);

    final String managedAccountId = createFundedManagedAccount(consumerManagedAccountProfileId,
        consumer.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, consumer.getRight());

    OutgoingWireTransfersService
        .startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, consumer.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumer.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Execution(ExecutionMode.SAME_THREAD)
  @ParameterizedTest
  @EnumSource(value = LimitInterval.class)
  public void StartVerification_NoRemainingNotifications_ChallengeLimitExceeded(
      final LimitInterval limitInterval) {

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(),
        ImmutableMap.of(limitInterval, 0));

    final String id = sendOutgoingWireTransfer(applicationFourCorporateManagedAccountId,
        applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey(),
        applicationFour.getOwtProfileId());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(applicationFour.getSecretKey(), id,
            applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Execution(ExecutionMode.SAME_THREAD)
  @Test
  public void StartVerification_NegativeNotificationLimit_ChallengeLimitExceeded() {

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(),
        ImmutableMap.of(LimitInterval.ALWAYS, -1));

    final String id = sendOutgoingWireTransfer(applicationFourCorporateManagedAccountId,
        applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey(),
        applicationFour.getOwtProfileId());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(applicationFour.getSecretKey(), id,
            applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Execution(ExecutionMode.SAME_THREAD)
  @Test
  public void StartVerification_NotificationLimitMultipleIdentities_ChallengeLimitExceeded() {

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(),
        ImmutableMap.of(LimitInterval.ALWAYS, 1));

    final String consumerOwtId = sendOutgoingWireTransfer(applicationFourConsumerManagedAccountId,
        applicationFourConsumerAuthenticationToken, applicationFour.getSecretKey(),
        applicationFour.getOwtProfileId());
    final String corporateOwtId = sendOutgoingWireTransfer(applicationFourCorporateManagedAccountId,
        applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey(),
        applicationFour.getOwtProfileId());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(consumerOwtId, CHANNEL,
            applicationFour.getSecretKey(), applicationFourConsumerAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(corporateOwtId, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(applicationFour.getSecretKey(),
            corporateOwtId, applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Disabled
  @Execution(ExecutionMode.SAME_THREAD)
  @Test
  @DisplayName("StartVerification_NotificationLimitReachedThenIncreased_Success - will be fixed by DEV-2704")
  public void StartVerification_NotificationLimitReachedThenIncreased_Success() {

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(),
        ImmutableMap.of(LimitInterval.ALWAYS, 0));

    final String id = sendOutgoingWireTransfer(applicationFourCorporateManagedAccountId,
        applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey(),
        applicationFour.getOwtProfileId());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(applicationFour.getSecretKey(), id,
            applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(),
        ImmutableMap.of(LimitInterval.ALWAYS, 1));

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Execution(ExecutionMode.SAME_THREAD)
  @Test
  public void StartVerification_SmsNotificationNotAffectingAuthyLimit_Success() {

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(),
        ImmutableMap.of(LimitInterval.ALWAYS, 1));

    final String consumerOwtId =
        sendOutgoingWireTransfer(applicationFourConsumerManagedAccountId,
            applicationFourConsumerAuthenticationToken, applicationFour.getSecretKey(),
            applicationFour.getOwtProfileId());
    final String consumerOwtId1 =
        sendOutgoingWireTransfer(applicationFourConsumerManagedAccountId,
            applicationFourConsumerAuthenticationToken, applicationFour.getSecretKey(),
            applicationFour.getOwtProfileId());
    final String corporateOwtId =
        sendOutgoingWireTransfer(applicationFourCorporateManagedAccountId,
            applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey(),
            applicationFour.getOwtProfileId());

    OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(consumerOwtId,
            EnrolmentChannel.SMS.name(),
            applicationFour.getSecretKey(), applicationFourConsumerAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(consumerOwtId1, CHANNEL,
            applicationFour.getSecretKey(), applicationFourConsumerAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(corporateOwtId, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(applicationFour.getSecretKey(),
            corporateOwtId, applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Execution(ExecutionMode.SAME_THREAD)
  @Test
  public void StartVerification_MultipleIntervalLimits_ChallengeLimitExceeded() {

    final Map<LimitInterval, Integer> limits = new HashMap<>();
    limits.put(LimitInterval.DAILY, 0);
    limits.put(LimitInterval.WEEKLY, 20);
    limits.put(LimitInterval.MONTHLY, 40);
    limits.put(LimitInterval.QUARTERLY, 60);
    limits.put(LimitInterval.YEARLY, 80);
    limits.put(LimitInterval.ALWAYS, 100);

    AdminHelper.setAuthyChallengeLimit(applicationFour.getProgrammeId(), limits);

    final String id = sendOutgoingWireTransfer(applicationFourCorporateManagedAccountId,
        applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey(),
        applicationFour.getOwtProfileId());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            applicationFour.getSecretKey(), applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(applicationFour.getSecretKey(), id,
            applicationFourCorporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  private String sendOutgoingWireTransfer(final String managedAccountId,
      final String token) {

    return sendOutgoingWireTransfer(managedAccountId, token, secretKey,
        outgoingWireTransfersProfileId);
  }

  private String sendOutgoingWireTransfer(final String managedAccountId,
      final String token,
      final String secretKey,
      final String profileId) {

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(profileId,
            managedAccountId,
            Currency.EUR.name(), 100L, OwtType.SEPA).build();

    return OutgoingWireTransfersService
        .sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
        .then()
        .statusCode(SC_OK)
        .extract()
        .jsonPath()
        .get("id");
  }

  private static void applicationOneConsumerSetup() {
    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, secretKey);
    final String applicationOneConsumerId = authenticatedConsumer.getLeft();
    applicationOneConsumerAuthenticationToken = authenticatedConsumer.getRight();

    ConsumersHelper.verifyKyc(secretKey, applicationOneConsumerId);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(applicationOneConsumerId, secretKey,
        applicationOneConsumerAuthenticationToken);

    applicationOneConsumerManagedAccountId = createFundedManagedAccount(
        consumerManagedAccountProfileId, applicationOneConsumerAuthenticationToken);
  }

  private static void applicationOneCorporateSetup() {
    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    final String corporateId = authenticatedCorporate.getLeft();
    applicationOneCorporateAuthenticationToken = authenticatedCorporate.getRight();

    CorporatesHelper.verifyKyb(secretKey, corporateId);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, secretKey,
        applicationOneCorporateAuthenticationToken);

    applicationOneCorporateManagedAccountId = createFundedManagedAccount(
        corporateManagedAccountProfileId, applicationOneCorporateAuthenticationToken);
  }

  private static void applicationFourConsumerSetup() {
    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(applicationFour.getConsumersProfileId())
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, applicationFour.getSecretKey());
    applicationFourConsumerAuthenticationToken = authenticatedConsumer.getRight();

    ConsumersHelper.verifyKyc(applicationFour.getSecretKey(), authenticatedConsumer.getLeft());

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(authenticatedConsumer.getLeft(),
        applicationFour.getSecretKey(), applicationFourConsumerAuthenticationToken);
    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        applicationFour.getSecretKey(), applicationFourConsumerAuthenticationToken);

    applicationFourConsumerManagedAccountId = createFundedManagedAccount(
        applicationFour.getConsumerPayneticsEeaManagedAccountsProfileId(),
        applicationFourConsumerAuthenticationToken, applicationFour.getSecretKey());
  }

  private static void applicationFourCorporateSetup() {
    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(applicationFour.getCorporatesProfileId())
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, applicationFour.getSecretKey());
    final String corporateId = authenticatedCorporate.getLeft();
    applicationFourCorporateAuthenticationToken = authenticatedCorporate.getRight();

    CorporatesHelper.verifyKyb(applicationFour.getSecretKey(), corporateId);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, applicationFour.getSecretKey(),
        applicationFourCorporateAuthenticationToken);

    applicationFourCorporateManagedAccountId = createFundedManagedAccount(
        applicationFour.getCorporatePayneticsEeaManagedAccountsProfileId(),
        applicationFourCorporateAuthenticationToken, applicationFour.getSecretKey());
  }

  private static String createFundedManagedAccount(final String profile,
      final String token) {
    return createFundedManagedAccount(profile, token, secretKey);
  }

  private static String createFundedManagedAccount(final String profile,
      final String token,
      final String secretKey) {
    final String managedAccountId =
        createManagedAccount(profile, Currency.EUR.name(), token, secretKey)
            .getLeft();

    fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

    return managedAccountId;
  }

  private static Stream<Arguments> owtSuccessfulArgs() {
    return Stream.of(Arguments.of(Currency.EUR, OwtType.SEPA),
        Arguments.of(Currency.GBP, OwtType.FASTER_PAYMENTS));
  }

  private String getCurrencySymbol(final OwtType owtType) {
    return owtType.equals(OwtType.SEPA) ? getSymbolByCurrency(Currency.EUR) : getSymbolByCurrency(Currency.GBP);
  }

  private String getSymbolByCurrency(final Currency currency) {

    return java.util.Currency.getAvailableCurrencies().stream().filter(x -> x.getCurrencyCode().equals(currency.name())).findFirst().orElseThrow().getSymbol(Locale.ENGLISH);

  }

  @AfterAll
  public static void resetLimits() {
    final String adminToken = AdminService.loginAdmin();
    final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

    AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount,
        adminToken);
    AdminHelper.setProgrammeAuthyChallengeLimit(applicationFour.getProgrammeId(), resetCount,
        adminToken);
  }
}