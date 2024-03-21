package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import commons.enums.State;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.admin.AdminService;
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
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
public class VerifyAuthyPushSendTests extends BaseSendsSetup {

    private static final String CHANNEL = EnrolmentChannel.AUTHY.name();

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
        final String adminToken = AdminService.loginAdmin();
        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeIdScaSendsApp, adminToken);
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeIdScaSendsApp, resetCount, adminToken);

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

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
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

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void VerifySend_AcceptAuthenticatedUser_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKeyScaSendsApp,
                user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                corporateAuthenticationTokenDestination);
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(userDestination.getLeft(), secretKeyScaSendsApp,
                userDestination.getRight());


        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, user.getRight());
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
                corporateCurrencySource, managedCardId).get(0).getLeft();

        startVerification(sendId, user.getRight());

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, user.getRight(), State.COMPLETED);
    }

    @Test
    public void VerifySend_MultipleEnrolmentsAcceptedByAuthy_Success() {

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                secretKeyScaSendsApp, corporateAuthenticationTokenSource);

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, corporateAuthenticationTokenSource);

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
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

        SimulatorService.rejectAuthySend(secretKeyScaSendsApp, sendId)
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

        SimulatorService.rejectAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, consumerAuthenticationTokenSource, State.REJECTED);
    }

    @Test
    public void VerifySend_RejectAuthenticatedUser_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKeyScaSendsApp,
                user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                corporateAuthenticationTokenDestination);
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(userDestination.getLeft(), secretKeyScaSendsApp,
                userDestination.getRight());


        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, user.getRight());
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
                corporateCurrencySource, managedCardId).get(0).getLeft();

        startVerification(sendId, user.getRight());

        SimulatorService.rejectAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, user.getRight(), State.REJECTED);
    }

    @Test
    public void VerifySend_StartedBySmsUserNotEnrolledWithAuthy_NotFound() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                secretKeyScaSendsApp, corporate.getRight());

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                secretKeyScaSendsApp, corporateDestination.getRight());


        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight());
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, corporateDestination.getRight())
                .getLeft();

        final long sendAmount = 100L;
        fundManagedAccount(managedAccountId, corporateCurrencySource, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, corporate.getRight(),
                                Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        SendsService.startSendOtpVerification(sendId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp,
                        corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        assertSendState(sendId, corporate.getRight(), State.PENDING_CHALLENGE);
    }

    @Test
    public void VerifySend_StartedBySmsUserEnrolledWithAuthy_NotFound() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                secretKeyScaSendsApp, corporate.getRight());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyScaSendsApp,
                corporate.getRight());

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                secretKeyScaSendsApp, corporateDestination.getRight());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateDestination.getLeft(), secretKeyScaSendsApp,
                corporateDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight());
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, corporateDestination.getRight())
                .getLeft();

        final long sendAmount = 100L;
        fundManagedAccount(managedAccountId, corporateCurrencySource, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, corporate.getRight(),
                                Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        SendsService.startSendOtpVerification(sendId, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp,
                        corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        assertSendState(sendId, corporate.getRight(), State.PENDING_CHALLENGE);
    }

    @Test
    public void VerifySend_UnknownOwtId_NotFound() {

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

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

        SimulatorService.acceptAuthySend(otherInnovatorSecretKey, sendId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_FOUND"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_InvalidApiKey_NotFound() {

        SimulatorService.acceptAuthySend("abc", RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifySend_NoApiKey_BadRequest() {

        SimulatorService.acceptAuthySend("", RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifySend_ChallengeExpired_Expired() throws SQLException, InterruptedException {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        AuthySimulatorDatabaseHelper.expirePaymentInitiationRequest(sendId);
        TimeUnit.SECONDS.sleep(2);

        SimulatorService.acceptAuthySend(secretKeyScaSendsApp, sendId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EXPIRED"));

        assertSendState(sendId, corporateAuthenticationTokenSource, State.PENDING_CHALLENGE);
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

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerId, secretKeyScaSendsApp,
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

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, secretKeyScaSendsApp,
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

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerId, secretKeyScaSendsApp,
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

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, secretKeyScaSendsApp,
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

    private static void assertSendState(final String id, final String token, final State state) {
        TestHelper.ensureAsExpected(120,
                () -> SendsService.getSend(secretKeyScaSendsApp, id, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
                Optional.of(String.format("Expecting 200 with a send in state %s, check logged payload", state.name())));
    }

}
