package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import commons.models.MobileNumberModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SENDS)
public class VerifyOkayPushSendTests extends BaseSendsSetup {

  private static final String CHANNEL = EnrolmentChannel.BIOMETRIC.name();
  private static String corporateAuthenticationTokenSource;
  private static String consumerAuthenticationTokenSource;
  private static String corporateCurrencySource;
  private static String consumerCurrencySource;
  private static Pair<String, CreateManagedAccountModel> corporateManagedAccountSource;
  private static Pair<String, CreateManagedCardModel> corporateManagedCardDestination;
  private static Pair<String, CreateManagedAccountModel> consumerManagedAccountSource;
  private static Pair<String, CreateManagedCardModel> consumerManagedCardDestination;
  private static String consumerAuthenticationTokenDestination;
  private static String corporateAuthenticationTokenDestination;

  @BeforeAll
  public static void Setup() {

    corporateSetupSource();
    consumerSetupSource();
    corporateSetupDestination();
    consumerSetupDestination();

    corporateManagedAccountSource =
        createManagedAccount(corporateManagedAccountProfileIdScaSendsApp, corporateCurrencySource,
            secretKeyScaSendsApp, corporateAuthenticationTokenSource);
    corporateManagedCardDestination =
        createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, corporateCurrencySource,
            secretKeyScaSendsApp, corporateAuthenticationTokenDestination);
    consumerManagedAccountSource =
        createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerCurrencySource,
            secretKeyScaSendsApp, consumerAuthenticationTokenSource);
    consumerManagedCardDestination =
        createPrepaidManagedCard(consumerPrepaidManagedCardsProfileIdScaSendsApp, consumerCurrencySource,
            secretKeyScaSendsApp, consumerAuthenticationTokenDestination);
  }

  @Test
  public void VerifySend_AcceptCorporate_Success() {

    final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
        corporateManagedAccountSource.getLeft(),
        corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, corporateAuthenticationTokenSource);

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, corporateAuthenticationTokenSource, State.COMPLETED);
  }

  @Test
  public void VerifySend_AcceptConsumer_Success() {

    final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource,
        consumerManagedAccountSource.getLeft(),
        consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, consumerAuthenticationTokenSource);

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, consumerAuthenticationTokenSource, State.COMPLETED);
  }

  @Test
  public void VerifySend_AcceptAuthenticatedUser_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
        corporateAuthenticationTokenSource);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

    SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKeyScaSendsApp, secretKeyScaSendsApp,
        user.getRight());

    final String managedAccountId = createFundedManagedAccount(
        corporateManagedAccountProfileIdScaSendsApp, user.getRight());

    final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
        corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, user.getRight());

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, user.getRight(), State.COMPLETED);
  }

  @Test
  public void VerifySend_MultipleEnrolmentsAcceptedByOkay_Success() {

    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        secretKeyScaSendsApp, corporateAuthenticationTokenSource);

    final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
        corporateManagedAccountSource.getLeft(),
        corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, corporateAuthenticationTokenSource);

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, corporateAuthenticationTokenSource, State.COMPLETED);
  }

  @Test
  public void VerifySend_RejectCorporate_Success() {

    final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
        corporateManagedAccountSource.getLeft(),
        corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, corporateAuthenticationTokenSource);

    SimulatorService.rejectOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, corporateAuthenticationTokenSource, State.REJECTED);
  }

  @Test
  public void VerifySend_RejectConsumer_Success() {

    final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource,
        consumerManagedAccountSource.getLeft(),
        consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, consumerAuthenticationTokenSource);

    SimulatorService.rejectOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, consumerAuthenticationTokenSource, State.REJECTED);
  }

  @Test
  public void VerifySend_RejectAuthenticatedUser_Success() {

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
        corporateAuthenticationTokenSource);
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

    SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKeyScaSendsApp, secretKeyScaSendsApp,
        user.getRight());

    final String managedAccountId = createFundedManagedAccount(
        corporateManagedAccountProfileIdScaSendsApp, user.getRight());

    final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
        corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, user.getRight());

    SimulatorService.rejectOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_NO_CONTENT);

    assertSendState(sendId, user.getRight(), State.REJECTED);
  }

  @ParameterizedTest
  @EnumSource(value = OkayEnrolled.class)
  public void VerifySend_StartedBySmsUserEnrolledOrNotWithOkay_NotFound(final OkayEnrolled okayEnrolled) {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
            secretKeyScaSendsApp);

    AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
        secretKeyScaSendsApp, corporate.getRight());

    if (okayEnrolled == OkayEnrolled.ENROLLED) {
      SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), sharedKeyScaSendsApp,
          secretKeyScaSendsApp, corporate.getRight());
    }

    final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
            .DefaultCreateManagedAccountModel(corporateManagedAccountProfileIdScaSendsApp,
                createCorporateModel.getBaseCurrency()).build();

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaSendsApp,
            corporate.getRight());

    fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), 10000L);

    final String sendId = identityDepositAndSendMaToMc(corporate.getRight(),
        managedAccountId, Currency.EUR.name(), corporateManagedCardDestination.getLeft()).get(0).getLeft();

    SendsService.startSendOtpVerification(sendId, EnrolmentChannel.SMS.name(),
            secretKeyScaSendsApp, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));

    assertSendState(sendId, corporate.getRight(), State.PENDING_CHALLENGE);
  }

  @ParameterizedTest
  @EnumSource(value = OkayEnrolled.class)
  public void VerifySend_StartedByAuthyUserEnrolledOrNotWithOkay_NotFound(final OkayEnrolled okayEnrolled) {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
            secretKeyScaSendsApp);

    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(),
        secretKeyScaSendsApp, corporate.getRight());

    if (okayEnrolled == OkayEnrolled.ENROLLED) {
      SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), sharedKeyScaSendsApp,
          secretKeyScaSendsApp, corporate.getRight());
    }

    final CreateManagedAccountModel createManagedAccountModel =
        CreateManagedAccountModel
            .DefaultCreateManagedAccountModel(corporateManagedAccountProfileIdScaSendsApp,
                createCorporateModel.getBaseCurrency()).build();

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaSendsApp,
            corporate.getRight());

    fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), 10000L);

    final String sendId = identityDepositAndSendMaToMc(corporate.getRight(),
        managedAccountId, Currency.EUR.name(), corporateManagedCardDestination.getLeft()).get(0).getLeft();

    SendsService.startSendPushVerification(sendId, EnrolmentChannel.AUTHY.name(),
            secretKeyScaSendsApp, corporate.getRight())
        .then()
        .statusCode(SC_NO_CONTENT);

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, sendId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));

    assertSendState(sendId, corporate.getRight(), State.PENDING_CHALLENGE);
  }

  @Test
  public void VerifySend_UnknownSendId_NotFound() {

    SimulatorService.acceptOkaySend(secretKeyScaSendsApp, RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));
  }

  @Test
  public void VerifySend_DifferentInnovatorApiKey_NotFound() {

    final Triple<String, String, String> innovator =
        TestHelper.registerLoggedInInnovatorWithProgramme();

    final String otherInnovatorSecretKey =
        InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
            .get("secretKey");

    final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
        corporateManagedAccountSource.getLeft(),
        corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

    startVerification(sendId, corporateAuthenticationTokenSource);

    SimulatorService.acceptOkaySend(otherInnovatorSecretKey, sendId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("REQUEST_NOT_FOUND"));

    SendsService.getSend(secretKeyScaSendsApp, sendId,
            corporateAuthenticationTokenSource)
        .then()
        .statusCode(SC_OK)
        .body("state", equalTo("PENDING_CHALLENGE"));
  }

  @Test
  public void VerifySend_InvalidApiKey_NotFound() {

    SimulatorService.acceptOkaySend("abc", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void VerifySend_NoApiKey_BadRequest() {

    SimulatorService.acceptOkaySend("", RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_BAD_REQUEST);
  }


  private static void consumerSetupSource() {

    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, secretKeyScaSendsApp);
    final String consumerId = authenticatedConsumer.getLeft();
    consumerAuthenticationTokenSource = authenticatedConsumer.getRight();
    consumerCurrencySource = createConsumerModel.getBaseCurrency();

    ConsumersHelper.verifyKyc(secretKeyScaSendsApp, consumerId);

    SecureHelper.enrolAndVerifyBiometric(consumerId, sharedKeyScaSendsApp, secretKeyScaSendsApp,
        consumerAuthenticationTokenSource);
  }

  private static void corporateSetupSource() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKeyScaSendsApp);
    final String corporateId = authenticatedCorporate.getLeft();
    corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
    corporateCurrencySource = createCorporateModel.getBaseCurrency();

    CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateId);

    SecureHelper.enrolAndVerifyBiometric(corporateId, sharedKeyScaSendsApp, secretKeyScaSendsApp,
        corporateAuthenticationTokenSource);
  }

  private static void consumerSetupDestination() {

    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, secretKeyScaSendsApp);
    final String consumerId = authenticatedConsumer.getLeft();
    consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();

    ConsumersHelper.verifyKyc(secretKeyScaSendsApp, consumerId);

    SecureHelper.enrolAndVerifyBiometric(consumerId, sharedKeyScaSendsApp, secretKeyScaSendsApp,
        consumerAuthenticationTokenDestination);
  }

  private static void corporateSetupDestination() {

    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
            .setBaseCurrency(Currency.EUR.name())
            .build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKeyScaSendsApp);

    final String corporateId = authenticatedCorporate.getLeft();

    corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

    CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateId);

    SecureHelper.enrolAndVerifyBiometric(corporateId, sharedKeyScaSendsApp, secretKeyScaSendsApp,
        corporateAuthenticationTokenDestination);
  }

  private static List<Pair<String, SendFundsModel>> identityDepositAndSendMaToMc(final String token,
                                                                                 final String identityManagedAccountId,
                                                                                 final String identityCurrency,
                                                                                 final String identityManagedCardId) {

    final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

    fundManagedAccount(identityManagedAccountId, identityCurrency, 10000L);

    IntStream.range(0, 2).forEach(i -> {
      final SendFundsModel sendFundsModel =
          SendFundsModel.newBuilder()
              .setProfileId(sendsProfileIdScaSendsApp)
              .setTag(RandomStringUtils.randomAlphabetic(5))
              .setDestinationAmount(new CurrencyAmount(identityCurrency, 100L))
              .setSource(new ManagedInstrumentTypeId(identityManagedAccountId, MANAGED_ACCOUNTS))
              .setDestination(new ManagedInstrumentTypeId(identityManagedCardId, MANAGED_CARDS))
              .build();

      final String id =
          SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, token, Optional.empty())
              .then()
              .statusCode(SC_OK)
              .extract()
              .jsonPath()
              .get("id");

      identitySendFunds.add(Pair.of(id, sendFundsModel));
    });
    return identitySendFunds;
  }

  private static String createFundedManagedAccount(final String profile,
                                                   final String token) {
    final String managedAccountId =
        createManagedAccount(profile, Currency.EUR.name(), secretKeyScaSendsApp, token)
            .getLeft();

    fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

    return managedAccountId;
  }

  private static void startVerification(final String sendId,
                                       final String token) {
    SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, token)
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  private static void assertSendState(final String id,
                                      final String token,
                                      final State state) {
    TestHelper.ensureAsExpected(120,
            () -> SendsService.getSend(secretKeyScaSendsApp, id, token),
            x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
            Optional.of(String.format("Expecting 200 with a send in state %s, check logged payload", state.name())));
  }

  private enum OkayEnrolled {
    ENROLLED,
    NOT_ENROLLED
  }
}