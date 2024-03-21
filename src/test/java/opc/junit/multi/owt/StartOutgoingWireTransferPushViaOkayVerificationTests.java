package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import commons.enums.State;
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
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.OWT)
public class StartOutgoingWireTransferPushViaOkayVerificationTests extends
    BaseOutgoingWireTransfersSetup {

  private static final String CHANNEL = EnrolmentChannel.BIOMETRIC.name();

  private static String corporateAuthenticationToken;
  private static String consumerAuthenticationToken;
  private static String corporateManagedAccountId;
  private static String consumerManagedAccountId;


  @BeforeAll
  public static void Setup() {
    corporateSetup();
    consumerSetup();
  }

  @Test
  public void StartVerification_Corporate_Success() {

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, corporateAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id,
            corporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_Consumer_Success() {

    final String id = sendOutgoingWireTransfer(consumerManagedAccountId,
        consumerAuthenticationToken);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, consumerAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id,
            consumerAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_AuthenticatedUser_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
        corporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

    SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey,
        passcodeAppSecretKey, user.getRight());

    final String managedAccountId = createFundedManagedAccount(
        passcodeAppCorporateManagedAccountProfileId, user.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, user.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id, user.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_RootStartUserVerification_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
        corporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

    SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey,
        passcodeAppSecretKey, user.getRight());

    final String managedAccountId = createFundedManagedAccount(
        passcodeAppCorporateManagedAccountProfileId, user.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, corporateAuthenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id, user.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_UserStartRootVerification_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
        corporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

    SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey,
        passcodeAppSecretKey, user.getRight());

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, user.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id,
            corporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void StartVerification_OwtSentBySmsUserNotEnrolledWithOkay_ChannelNotRegistered() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();
    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
            passcodeAppSecretKey);

    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        passcodeAppSecretKey, corporate.getRight());

    final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
            .DefaultCreateManagedAccountModel(passcodeAppCorporateManagedAccountProfileId,
                createCorporateModel.getBaseCurrency()).build();
    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, passcodeAppSecretKey,
            corporate.getRight());

    fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), 10000L);

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());

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
  public void StartVerification_OwtSentByAuthyUserNotEnrolledWithOkay_ChannelNotRegistered() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();
    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
            passcodeAppSecretKey);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), passcodeAppSecretKey,
        corporate.getRight());

    final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
            .DefaultCreateManagedAccountModel(passcodeAppCorporateManagedAccountProfileId,
                createCorporateModel.getBaseCurrency()).build();
    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, passcodeAppSecretKey,
            corporate.getRight());

    fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), 10000L);

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());

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
            CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build(),
            passcodeAppSecretKey);

    SecureHelper.enrolBiometricUser(consumer.getRight(), passcodeAppSharedKey);

    final String managedAccountId = createFundedManagedAccount(
        passcodeAppConsumerManagedAccountProfileId, consumer.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, consumer.getRight());

    OutgoingWireTransfersService
        .startOutgoingWireTransferPushVerification(id, CHANNEL, passcodeAppSecretKey,
            consumer.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
  }

  @Test
  public void StartVerification_UserEnrolmentRejected_ChannelNotRegistered() {

    final Pair<String, String> consumer =
        ConsumersHelper.createAuthenticatedVerifiedConsumer(
            CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build(),
            passcodeAppSecretKey);
    SecureHelper.enrolBiometricUser(consumer.getRight(), passcodeAppSharedKey);

    SimulatorHelper.rejectOkayIdentity(passcodeAppSecretKey, consumer.getLeft(),
        consumer.getRight(), State.INACTIVE);

    final String managedAccountId = createFundedManagedAccount(
        passcodeAppConsumerManagedAccountProfileId, consumer.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, consumer.getRight());

    OutgoingWireTransfersService
        .startOutgoingWireTransferPushVerification(id, CHANNEL, passcodeAppSecretKey,
            consumer.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id,
            consumer.getRight())
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  private String sendOutgoingWireTransfer(final String managedAccountId,
      final String token) {

    return sendOutgoingWireTransfer(managedAccountId, token, passcodeAppSecretKey,
        passcodeAppOutgoingWireTransfersProfileId);
  }

  private String sendOutgoingWireTransfer(final String managedAccountId,
      final String token,
      final String passcodeAppSecretKey,
      final String profileId) {

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(profileId,
            managedAccountId,
            Currency.EUR.name(), 100L, OwtType.SEPA).build();

    return OutgoingWireTransfersService
        .sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, token,
            Optional.empty())
        .then()
        .statusCode(SC_OK)
        .extract()
        .jsonPath()
        .get("id");
  }

  private static String createFundedManagedAccount(final String profile,
      final String token) {
    return createFundedManagedAccount(profile, token, passcodeAppSecretKey);
  }

  private static String createFundedManagedAccount(final String profile,
      final String token,
      final String passcodeAppSecretKey) {
    final String managedAccountId =
        createManagedAccount(profile, Currency.EUR.name(), token, passcodeAppSecretKey)
            .getLeft();

    fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

    return managedAccountId;
  }

  private static void consumerSetup() {
    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(
        createConsumerModel, passcodeAppSecretKey);
    consumerAuthenticationToken = authenticatedConsumer.getRight();

    SecureHelper.enrolAndVerifyBiometric(authenticatedConsumer.getLeft(), passcodeAppSharedKey,
        passcodeAppSecretKey, consumerAuthenticationToken);

    consumerManagedAccountId = createFundedManagedAccount(
        passcodeAppConsumerManagedAccountProfileId, consumerAuthenticationToken);
  }

  private static void corporateSetup() {
    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(
        createCorporateModel, passcodeAppSecretKey);
    corporateAuthenticationToken = authenticatedCorporate.getRight();

    SecureHelper.enrolAndVerifyBiometric(authenticatedCorporate.getLeft(), passcodeAppSharedKey,
        passcodeAppSecretKey, corporateAuthenticationToken);

    corporateManagedAccountId = createFundedManagedAccount(
        passcodeAppCorporateManagedAccountProfileId, corporateAuthenticationToken);
  }
}
