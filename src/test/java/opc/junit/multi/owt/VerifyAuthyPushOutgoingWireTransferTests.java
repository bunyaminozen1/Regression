package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
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
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.OWT)
public class VerifyAuthyPushOutgoingWireTransferTests extends BaseOutgoingWireTransfersSetup {

  private static final String CHANNEL = EnrolmentChannel.AUTHY.name();

  private static String corporateAuthenticationToken;
  private static String consumerAuthenticationToken;
  private static String corporateManagedAccountId;
  private static String consumerManagedAccountId;

  @BeforeAll
  public static void Setup() {

    final String adminToken = AdminService.loginAdmin();
    final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

    AdminHelper.resetProgrammeAuthyLimitsCounter(passcodeAppProgrammeId, adminToken);
    AdminHelper.setProgrammeAuthyChallengeLimit(passcodeAppProgrammeId, resetCount, adminToken);

    corporateSetup();
    consumerSetup();
  }

  @Test
  public void VerifyTransfer_AcceptCorporate_Success() {

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);
    startVerification(id, corporateAuthenticationToken);

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.COMPLETED);
  }

  @Test
  public void VerifyTransfer_AcceptConsumer_Success() {

    final String id = sendOutgoingWireTransfer(consumerManagedAccountId,
        consumerAuthenticationToken);
    startVerification(id, consumerAuthenticationToken);

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertOutgoingWireTransferState(id, consumerAuthenticationToken, State.COMPLETED);
  }

  @Test
  public void VerifyTransfer_AcceptAuthenticatedUser_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
        corporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), passcodeAppSecretKey,
        user.getRight());

    final String managedAccountId = createFundedManagedAccount(
        passcodeAppCorporateManagedAccountProfileId, user.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());
    startVerification(id, user.getRight());

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertOutgoingWireTransferState(id, user.getRight(), State.COMPLETED);
  }

  @Test
  public void VerifyTransfer_MultipleEnrolmentsAcceptedByAuthy_Success() {

    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        passcodeAppSecretKey, corporateAuthenticationToken);

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);
    startVerification(id, corporateAuthenticationToken);

    SimulatorHelper.acceptAuthyOwt(passcodeAppSecretKey, id);

    assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.COMPLETED);
  }

  @Test
  public void VerifyTransfer_RejectCorporate_Success() {

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);
    startVerification(id, corporateAuthenticationToken);

    SimulatorService.rejectAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.REJECTED);
  }

  @Test
  public void VerifyTransfer_RejectConsumer_Success() {

    final String id = sendOutgoingWireTransfer(consumerManagedAccountId,
        consumerAuthenticationToken);
    startVerification(id, consumerAuthenticationToken);

    SimulatorService.rejectAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertOutgoingWireTransferState(id, consumerAuthenticationToken, State.REJECTED);
  }

  @Test
  public void VerifyTransfer_RejectAuthenticatedUser_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(passcodeAppSecretKey,
        corporateAuthenticationToken);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), passcodeAppSecretKey,
        user.getRight());

    final String managedAccountId = createFundedManagedAccount(
        passcodeAppCorporateManagedAccountProfileId, user.getRight());
    final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());
    startVerification(id, user.getRight());

    SimulatorService.rejectAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertOutgoingWireTransferState(id, user.getRight(), State.REJECTED);
  }

  @Test
  public void VerifyTransfer_StartedBySmsUserNotEnrolledWithAuthy_NotFound() {

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
    OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id,
            EnrolmentChannel.SMS.name(),
            passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("NOT_FOUND"));

    assertOutgoingWireTransferState(id, corporate.getRight(), State.PENDING_CHALLENGE);
  }

  @Test
  public void VerifyTransfer_StartedByOkayUserNotEnrolledWithAuthy_NotFound() {

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

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());
    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id,
            EnrolmentChannel.BIOMETRIC.name(),
            passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("NOT_FOUND"));

    assertOutgoingWireTransferState(id, corporate.getRight(), State.PENDING_CHALLENGE);
  }

  @Test
  public void VerifyTransfer_StartedBySmsUserEnrolledWithOkay_NotFound() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();
    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
            passcodeAppSecretKey);

    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        passcodeAppSecretKey, corporate.getRight());

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

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());
    OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id,
            EnrolmentChannel.SMS.name(),
            passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptOkayOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));

    assertOutgoingWireTransferState(id, corporate.getRight(), State.PENDING_CHALLENGE);
  }

  @Test
  public void VerifyTransfer_StartedByOkayUserEnrolledWithAuthy_NotFound() {

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

    final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());
    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id,
            EnrolmentChannel.BIOMETRIC.name(),
            passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("NOT_FOUND"));

    assertOutgoingWireTransferState(id, corporate.getRight(), State.PENDING_CHALLENGE);
  }

  @Test
  public void VerifyTransfer_UnknownOwtId_NotFound() {

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("NOT_FOUND"));
  }

  @Test
  public void VerifyTransfer_DifferentInnovatorApiKey_NotFound() {

    final Triple<String, String, String> innovator =
        TestHelper.registerLoggedInInnovatorWithProgramme();

    final String otherInnovatorSecretKey =
        InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
            .get("secretKey");

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);
    startVerification(id, corporateAuthenticationToken);

    SimulatorService.acceptAuthyOwt(otherInnovatorSecretKey, id)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("NOT_FOUND"));

    OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id,
            corporateAuthenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void VerifyTransfer_InvalidApiKey_NotFound() {

    SimulatorService.acceptAuthyOwt("abc", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void VerifyTransfer_NoApiKey_BadRequest() {

    SimulatorService.acceptAuthyOwt("", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void VerifyTransfer_ChallengeExpired_Expired() throws SQLException, InterruptedException {

    final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
        corporateAuthenticationToken);
    startVerification(id, corporateAuthenticationToken);

    AuthySimulatorDatabaseHelper.expirePaymentInitiationRequest(id);
    TimeUnit.SECONDS.sleep(2);

    SimulatorService.acceptAuthyOwt(passcodeAppSecretKey, id)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("EXPIRED"));

    assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.PENDING_CHALLENGE);
  }

  private String sendOutgoingWireTransfer(final String managedAccountId,
      final String token) {

    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(
            passcodeAppOutgoingWireTransfersProfileId,
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

  private void startVerification(final String id,
      final String token) {

    OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
            passcodeAppSecretKey, token)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  private static void consumerSetup() {
    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, passcodeAppSecretKey);
    final String consumerId = authenticatedConsumer.getLeft();
    consumerAuthenticationToken = authenticatedConsumer.getRight();

    ConsumersHelper.verifyKyc(passcodeAppSecretKey, consumerId);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerId, passcodeAppSecretKey,
        consumerAuthenticationToken);

    consumerManagedAccountId = createFundedManagedAccount(
        passcodeAppConsumerManagedAccountProfileId, consumerAuthenticationToken);
  }

  private static void corporateSetup() {
    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, passcodeAppSecretKey);
    final String corporateId = authenticatedCorporate.getLeft();
    corporateAuthenticationToken = authenticatedCorporate.getRight();

    CorporatesHelper.verifyKyb(passcodeAppSecretKey, corporateId);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, passcodeAppSecretKey,
        corporateAuthenticationToken);

    corporateManagedAccountId = createFundedManagedAccount(
        passcodeAppCorporateManagedAccountProfileId, corporateAuthenticationToken);
  }

  private static String createFundedManagedAccount(final String profile,
      final String token) {
    final String managedAccountId =
        createManagedAccount(profile, Currency.EUR.name(), token, passcodeAppSecretKey)
            .getLeft();

    fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

    return managedAccountId;
  }

  private static void assertOutgoingWireTransferState(final String id, final String token, final State state) {
    TestHelper.ensureAsExpected(120,
            () -> OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id, token),
             x-> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
            Optional.of(String.format("Expecting 200 with an OWT in state %s, check logged payload", state.name())));
  }
}