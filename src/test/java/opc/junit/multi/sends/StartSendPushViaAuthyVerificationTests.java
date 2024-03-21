package opc.junit.multi.sends;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.enums.Currency;
import commons.enums.State;
import opc.enums.authy.AuthyMessage;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.ScaConfigModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.outgoingwiretransfers.AuthyStartVerificationNotificationModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.admin.AdminService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.SENDS)
public class StartSendPushViaAuthyVerificationTests extends BaseSendsSetup {

    /**
     * Two programs are used for the tests: ScaApp and ApplicationFour. ScaApp: default program
     * ApplicationFour: only for tests running in ExecutionMode.SAME_THREAD. These are tests with
     * changing Authy limits.
     */

    private static final String CHANNEL = EnrolmentChannel.AUTHY.name();
    private static final String CURRENCY = Currency.EUR.name();
    private static final String CURRENCY_SIGN =
            java.util.Currency.getAvailableCurrencies().stream().filter(x -> x.getCurrencyCode().equals(CURRENCY)).findFirst().orElseThrow().getSymbol();

    private static Pair<String, String> authenticatedCorporateScaAppSource;
    private static Pair<String, String> authenticatedCorporateScaAppDestination;
    private static Pair<String, String> authenticatedCorporateAppFourSource;
    private static Pair<String, String> authenticatedConsumerScaAppSource;
    private static Pair<String, String> authenticatedConsumerAppFourSource;

    private static Pair<String, CreateManagedAccountModel> corporateManagedAccountScaApp;
    private static Pair<String, CreateManagedCardModel> corporateManagedCardScaApp;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountScaApp;
    private static Pair<String, CreateManagedCardModel> consumerManagedCardScaApp;

    private static Pair<String, CreateManagedAccountModel> corporateManagedAccountAppFour;
    private static Pair<String, CreateManagedCardModel> corporateManagedCardAppFour;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountAppFour;
    private static Pair<String, CreateManagedCardModel> consumerManagedCardAppFour;
    private static String innovatorToken;

    @BeforeAll
    public static void Setup() {
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        String adminToken = AdminService.loginAdmin();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeIdScaSendsApp, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeIdScaSendsApp, resetCount, adminToken);

        AdminHelper.resetProgrammeAuthyLimitsCounter(applicationFourProgrammeId, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationFourProgrammeId, resetCount, adminToken);

        authenticatedCorporateScaAppSource = corporateSetup(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        authenticatedConsumerScaAppSource = consumerSetup(consumerProfileIdScaSendsApp, secretKeyScaSendsApp);
        authenticatedCorporateAppFourSource = corporateSetup(applicationFourCorporateProfileId, applicationFourSecretKey);
        authenticatedConsumerAppFourSource = consumerSetup(applicationFourConsumerProfileId, applicationFourSecretKey);
        authenticatedCorporateScaAppDestination = corporateSetup(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final Pair<String, String> authenticatedConsumerScaAppDestination = consumerSetup(consumerProfileIdScaSendsApp, secretKeyScaSendsApp);
        final Pair<String, String> authenticatedCorporateAppFourDestination = corporateSetup(applicationFourCorporateProfileId, applicationFourSecretKey);
        final Pair<String, String> authenticatedConsumerAppFourDestination = consumerSetup(applicationFourConsumerProfileId, applicationFourSecretKey);

        corporateManagedAccountScaApp =
                createManagedAccount(corporateManagedAccountProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedCorporateScaAppSource.getRight());
        corporateManagedCardScaApp =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedCorporateScaAppDestination.getRight());
        consumerManagedAccountScaApp =
                createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedConsumerScaAppSource.getRight());
        consumerManagedCardScaApp =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedConsumerScaAppDestination.getRight());

        corporateManagedAccountAppFour =
                createManagedAccount(applicationFourCorporateManagedAccountProfileId, CURRENCY,
                        applicationFourSecretKey, authenticatedCorporateAppFourSource.getRight());
        corporateManagedCardAppFour =
                createPrepaidManagedCard(applicationFourCorporatePrepaidManagedCardsProfileId, CURRENCY,
                        applicationFourSecretKey, authenticatedCorporateAppFourDestination.getRight());
        consumerManagedAccountAppFour =
                createManagedAccount(applicationFourConsumerManagedAccountProfileId, CURRENCY,
                        applicationFourSecretKey, authenticatedConsumerAppFourSource.getRight());
        consumerManagedCardAppFour =
                createPrepaidManagedCard(applicationFourConsumerPrepaidManagedCardsProfileId, CURRENCY,
                        applicationFourSecretKey, authenticatedConsumerAppFourDestination.getRight());

        enableSendsSca();
    }

    @AfterAll
    public static void disableSCA() {
        disableSendsSca();
    }

    @Test
    public void StartVerification_Corporate_Success() {
        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporateScaAppSource.getRight(),
                corporateManagedAccountScaApp.getLeft(),
                CURRENCY, corporateManagedCardScaApp.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(
                0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_Consumer_Success() {
        final String sendId = identityDepositAndSendMaToMc(authenticatedConsumerScaAppSource.getRight(),
                consumerManagedAccountScaApp.getLeft(),
                CURRENCY, consumerManagedCardScaApp.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0)
                .getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedConsumerScaAppSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedConsumerScaAppSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedConsumerScaAppSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_AuthenticatedUser_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporateScaAppSource.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKeyScaSendsApp,
                user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporateScaAppDestination.getRight());
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(userDestination.getLeft(), secretKeyScaSendsApp,
                userDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, user.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
                CURRENCY, managedCardId, sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_RootStartUserVerification_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporateScaAppSource.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKeyScaSendsApp,
                user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporateScaAppDestination.getRight());
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(userDestination.getLeft(), secretKeyScaSendsApp,
                userDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, user.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
                CURRENCY, managedCardId, sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_UserStartRootVerification_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporateScaAppSource.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKeyScaSendsApp,
                user.getRight());

        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporateScaAppSource.getRight(),
                corporateManagedAccountScaApp.getLeft(),
                CURRENCY, corporateManagedCardScaApp.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(
                0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_NotificationCheckMaToMc_Success()
            throws SQLException, JsonProcessingException {
        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, authenticatedCorporateScaAppSource.getRight(),
                secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, authenticatedCorporateScaAppDestination.getRight())
                .getLeft();

        final long sendAmount = 100L;
        fundManagedAccount(managedAccountId, CURRENCY, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(CURRENCY, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp,
                                authenticatedCorporateScaAppSource.getRight(),
                                Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> notification = AuthySimulatorDatabaseHelper.getNotification(
                programmeIdScaSendsApp);
        assertEquals(String.format(AuthyMessage.PAYMENT_INITIATION.getMessage(), programmeNameScaSendsApp),
                notification.get(0).get("message"));

        final AuthyStartVerificationNotificationModel keyValuePair =
                new ObjectMapper().readValue(notification.get(0).get("details"),
                        AuthyStartVerificationNotificationModel.class);

        final String managedCardLastFourDigits =
                ManagedCardsHelper.getCardNumberLastFour(secretKeyScaSendsApp, managedCardId, authenticatedCorporateScaAppDestination.getRight());

        assertEquals(new DecimalFormat("0.00").format(sendAmount / 100), keyValuePair.getAmount());
        assertEquals(CURRENCY_SIGN, keyValuePair.getCurrency());
        assertEquals(String.format("Card Number: **** %s", managedCardLastFourDigits), keyValuePair.getTo());
    }

    @Test
    public void StartVerification_NotificationCheckMaToCorporateMa_Success()
            throws SQLException, JsonProcessingException {
        final String sourceManagedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, authenticatedCorporateScaAppSource.getRight(),
                secretKeyScaSendsApp);

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();

        final Pair<String, String> corporateDestination = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKeyScaSendsApp);

        final Pair<String, CreateManagedAccountModel> destinationManagedAccountId = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, CURRENCY,
                secretKeyScaSendsApp, corporateDestination.getRight());

        final long sendAmount = 100L;
        fundManagedAccount(sourceManagedAccountId, CURRENCY, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(CURRENCY, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp,
                                authenticatedCorporateScaAppSource.getRight(),
                                Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedCorporateScaAppSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> notification = AuthySimulatorDatabaseHelper.getNotification(
                programmeIdScaSendsApp);
        assertEquals(String.format(AuthyMessage.PAYMENT_INITIATION.getMessage(), programmeNameScaSendsApp),
                notification.get(0).get("message"));

        final AuthyStartVerificationNotificationModel keyValuePair =
                new ObjectMapper().readValue(notification.get(0).get("details"),
                        AuthyStartVerificationNotificationModel.class);

        assertEquals(new DecimalFormat("0.00").format(sendAmount / 100), keyValuePair.getAmount());
        assertEquals(CURRENCY_SIGN, keyValuePair.getCurrency());
        assertEquals(String.format("%s %s. Account", createCorporateModel.getRootUser().getName(),
                createCorporateModel.getRootUser().getSurname().charAt(0)), keyValuePair.getTo());
    }

    @Test
    public void StartVerification_NotificationCheckMaToConsumerMa_Success()
        throws SQLException, JsonProcessingException {
        final String sourceManagedAccountId = createFundedManagedAccount(
            corporateManagedAccountProfileIdScaSendsApp, authenticatedCorporateScaAppSource.getRight(),
            secretKeyScaSendsApp);

        final CreateConsumerModel createConsumerModel =
            CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp).build();

        final Pair<String, String> consumerDestination = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKeyScaSendsApp);

        final Pair<String, CreateManagedAccountModel> destinationManagedAccountId = createManagedAccount(
            consumerManagedAccountProfileIdScaSendsApp, CURRENCY,
            secretKeyScaSendsApp, consumerDestination.getRight());

        final long sendAmount = 100L;
        fundManagedAccount(sourceManagedAccountId, CURRENCY, 10000L);

        final SendFundsModel sendFundsModel =
            SendFundsModel.newBuilder()
                .setProfileId(sendsProfileIdScaSendsApp)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(CURRENCY, sendAmount))
                .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId.getLeft(), MANAGED_ACCOUNTS))
                .build();

        final String sendId =
            SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp,
                    authenticatedCorporateScaAppSource.getRight(),
                    Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                authenticatedCorporateScaAppSource.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> notification = AuthySimulatorDatabaseHelper.getNotification(
            programmeIdScaSendsApp);
        assertEquals(String.format(AuthyMessage.PAYMENT_INITIATION.getMessage(), programmeNameScaSendsApp),
            notification.get(0).get("message"));

        final AuthyStartVerificationNotificationModel keyValuePair =
            new ObjectMapper().readValue(notification.get(0).get("details"),
                AuthyStartVerificationNotificationModel.class);

        assertEquals(new DecimalFormat("0.00").format(sendAmount / 100), keyValuePair.getAmount());
        assertEquals(CURRENCY_SIGN, keyValuePair.getCurrency());
        assertEquals(String.format("%s %s. Account", createConsumerModel.getRootUser().getName(),
            createConsumerModel.getRootUser().getSurname().charAt(0)), keyValuePair.getTo());
    }

    @Test
    public void StartVerification_SentBySmsUserNotEnrolledWithAuthy_ChannelNotRegistered() {
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
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, corporateDestination.getRight())
                .getLeft();

        final long sendAmount = 100L;
        fundManagedAccount(managedAccountId, CURRENCY, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(CURRENCY, sendAmount))
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

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_UserEnrolmentNotVerified_UserNotEnrolledOnChallenge() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAuthyPushUser(secretKeyScaSendsApp, corporate.getRight());

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAuthyPushUser(secretKeyScaSendsApp, corporateDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, corporateDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(corporate.getRight(), managedAccountId,
                createCorporateModel.getBaseCurrency(), managedCardId, sendsProfileIdScaSendsApp,
                secretKeyScaSendsApp).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_UserEnrolmentRejected_ChannelNotRegistered() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAuthyPushUser(secretKeyScaSendsApp, corporate.getRight());

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAuthyPushUser(secretKeyScaSendsApp, corporateDestination.getRight());

        SimulatorHelper.rejectAuthyIdentity(secretKeyScaSendsApp, corporate.getLeft(), corporate.getRight(),
                State.INACTIVE);

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, corporateDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(corporate.getRight(), managedAccountId,
                createCorporateModel.getBaseCurrency(), managedCardId, sendsProfileIdScaSendsApp,
                secretKeyScaSendsApp).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void StartVerification_NoRemainingNotifications_ChallengeLimitExceeded(
            final LimitInterval limitInterval) {
        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId, ImmutableMap.of(limitInterval, 0));

        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporateAppFourSource.getRight(),
                corporateManagedAccountAppFour.getLeft(),
                CURRENCY, corporateManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, applicationFourSecretKey,
                        authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        SendsService.getSend(applicationFourSecretKey, sendId, authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void StartVerification_NegativeNotificationLimit_ChallengeLimitExceeded() {
        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId,
                ImmutableMap.of(LimitInterval.ALWAYS, -1));

        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporateAppFourSource.getRight(),
                corporateManagedAccountAppFour.getLeft(),
                CURRENCY, corporateManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, applicationFourSecretKey,
                        authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        SendsService.getSend(applicationFourSecretKey, sendId, authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void StartVerification_NotificationLimitMultipleIdentities_ChallengeLimitExceeded() {
        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId, ImmutableMap.of(LimitInterval.ALWAYS, 1));

        final String corporateSendId = identityDepositAndSendMaToMc(
                authenticatedCorporateAppFourSource.getRight(),
                corporateManagedAccountAppFour.getLeft(),
                CURRENCY, corporateManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        final String consumerSendId = identityDepositAndSendMaToMc(
                authenticatedConsumerAppFourSource.getRight(),
                consumerManagedAccountAppFour.getLeft(),
                CURRENCY, consumerManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        SendsService.startSendPushVerification(corporateSendId, CHANNEL, applicationFourSecretKey,
                        authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.startSendPushVerification(consumerSendId, CHANNEL, applicationFourSecretKey,
                        authenticatedConsumerAppFourSource.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        SendsService.getSend(applicationFourSecretKey, consumerSendId, authenticatedConsumerAppFourSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Disabled
    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    @DisplayName("StartVerification_NotificationLimitReachedThenIncreased_Success - will be fixed by DEV-2704")
    public void StartVerification_NotificationLimitReachedThenIncreased_Success() {
        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId, ImmutableMap.of(LimitInterval.ALWAYS, 0));

        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporateAppFourSource.getRight(),
                corporateManagedAccountAppFour.getLeft(),
                CURRENCY, corporateManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, applicationFourSecretKey,
                        authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        SendsService.getSend(applicationFourSecretKey, sendId, authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId, ImmutableMap.of(LimitInterval.ALWAYS, 1));
        SendsService.startSendPushVerification(sendId, CHANNEL, applicationFourSecretKey,
                        authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void StartVerification_SmsNotificationNotAffectingAuthyLimit_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFourCorporateProfileId)
                        .setBaseCurrency(CURRENCY)
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        applicationFourSecretKey);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                applicationFourSecretKey, corporate.getRight());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), applicationFourSecretKey,
                corporate.getRight());

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFourCorporateProfileId)
                        .setBaseCurrency(CURRENCY)
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        applicationFourSecretKey);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                applicationFourSecretKey, corporateDestination.getRight());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateDestination.getLeft(), applicationFourSecretKey,
                corporateDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(
                applicationFourCorporateManagedAccountProfileId, corporate.getRight(), applicationFourSecretKey);
        final String managedCardId = createPrepaidManagedCard(
                applicationFourCorporatePrepaidManagedCardsProfileId, CURRENCY,
                applicationFourSecretKey, corporateDestination.getRight())
                .getLeft();

        final String corporateSendId = identityDepositAndSendMaToMc(corporate.getRight(),
                managedAccountId,
                createCorporateModel.getBaseCurrency(), managedCardId, applicationFourSendsProfileId, applicationFourSecretKey).get(0)
                .getLeft();
        final String corporateSendId1 = identityDepositAndSendMaToMc(corporate.getRight(),
                managedAccountId,
                createCorporateModel.getBaseCurrency(), managedCardId, applicationFourSendsProfileId, applicationFourSecretKey).get(0)
                .getLeft();
        final String consumerSendId = identityDepositAndSendMaToMc(
                authenticatedConsumerAppFourSource.getRight(),
                consumerManagedAccountAppFour.getLeft(),
                CURRENCY, consumerManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId, ImmutableMap.of(LimitInterval.ALWAYS, 1));

        SendsService.startSendOtpVerification(corporateSendId, EnrolmentChannel.SMS.name(),
                        applicationFourSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.startSendPushVerification(corporateSendId1, CHANNEL, applicationFourSecretKey,
                        corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.startSendPushVerification(consumerSendId, CHANNEL, applicationFourSecretKey,
                        authenticatedConsumerAppFourSource.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        SendsService.getSend(applicationFourSecretKey, consumerSendId, authenticatedConsumerAppFourSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
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

        AdminHelper.setAuthyChallengeLimit(applicationFourProgrammeId, limits);

        final String corporateSendId = identityDepositAndSendMaToMc(
                authenticatedCorporateAppFourSource.getRight(),
                corporateManagedAccountAppFour.getLeft(),
                CURRENCY, corporateManagedCardAppFour.getLeft(), applicationFourSendsProfileId, applicationFourSecretKey).get(0).getLeft();

        SendsService.startSendPushVerification(corporateSendId, CHANNEL, applicationFourSecretKey,
                        authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        SendsService.getSend(applicationFourSecretKey, corporateSendId, authenticatedCorporateAppFourSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }


    private static Pair<String, String> consumerSetup(final String applicationFourConsumerProfileId,
                                                      final String applicationFourSecretKey) {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationFourConsumerProfileId)
                        .setBaseCurrency(CURRENCY)
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, applicationFourSecretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(applicationFourSecretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerId, applicationFourSecretKey,
                consumerAuthenticationToken);

        return Pair.of(consumerId, consumerAuthenticationToken);
    }

    private static Pair<String, String> corporateSetup(final String applicationFourCorporateProfileId,
                                                       final String applicationFourSecretKey) {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFourCorporateProfileId)
                        .setBaseCurrency(CURRENCY)
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, applicationFourSecretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(applicationFourSecretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, applicationFourSecretKey,
                corporateAuthenticationToken);

        return Pair.of(corporateId, corporateAuthenticationToken);
    }

    private static void enableSendsSca() {
        UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setScaConfig(new ScaConfigModel(true, false))
                        .build();
        InnovatorHelper.enableSendsSca(updateProgrammeModel, applicationFourProgrammeId, innovatorToken);
    }

    private static void disableSendsSca() {
        UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setScaConfig(new ScaConfigModel(false, false))
                        .build();
        InnovatorHelper.disableSendsSca(updateProgrammeModel, applicationFourProgrammeId, innovatorToken);
    }

    private static List<Pair<String, SendFundsModel>> identityDepositAndSendMaToMc(final String token,
                                                                                   final String identityManagedAccountId,
                                                                                   final String identityCurrency,
                                                                                   final String identityManagedCardId,
                                                                                   final String applicationFourSendsProfileId,
                                                                                   final String applicationFourSecretKey) {
        final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

        fundManagedAccount(identityManagedAccountId, identityCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(applicationFourSendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(identityCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(identityManagedAccountId, MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(identityManagedCardId, MANAGED_CARDS))
                            .build();

            final String id =
                    SendsService.sendFunds(sendFundsModel, applicationFourSecretKey, token, Optional.empty())
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
                                                     final String token,
                                                     final String applicationFourSecretKey) {
        final String managedAccountId =
                createManagedAccount(profile, Currency.EUR.name(), applicationFourSecretKey, token)
                        .getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        return managedAccountId;
    }

}
