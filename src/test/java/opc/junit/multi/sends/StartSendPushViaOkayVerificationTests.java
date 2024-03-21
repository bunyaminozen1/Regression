package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import commons.enums.State;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SENDS)
public class StartSendPushViaOkayVerificationTests extends BaseSendsSetup {
    private static final String CHANNEL = EnrolmentChannel.BIOMETRIC.name();
    private static final String CURRENCY = Currency.EUR.name();

    private static Pair<String, String> authenticatedCorporate;
    private static Pair<String, String> authenticatedConsumer;
    private static Pair<String, String> authenticatedCorporateDestination;
    private static Pair<String, String> authenticatedConsumerDestination;


    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedCardModel> corporateManagedCard;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccount;
    private static Pair<String, CreateManagedCardModel> consumerManagedCard;

    @BeforeAll
    public static void Setup() {
        authenticatedCorporate = corporateSetup();
        authenticatedConsumer = consumerSetup();
        authenticatedCorporateDestination = corporateSetup();
        authenticatedConsumerDestination = consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedCorporate.getRight());
        corporateManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedCorporateDestination.getRight());
        consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedConsumer.getRight());
        consumerManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileIdScaSendsApp, CURRENCY,
                        secretKeyScaSendsApp, authenticatedConsumerDestination.getRight());
    }

    @Test
    public void StartVerification_Corporate_Success() {
        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporate.getRight(),
                corporateManagedAccount.getLeft(),
                CURRENCY, corporateManagedCard.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_Consumer_Success() {
        final String sendId = identityDepositAndSendMaToMc(authenticatedConsumer.getRight(),
                consumerManagedAccount.getLeft(),
                CURRENCY, consumerManagedCard.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0)
                .getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedConsumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        authenticatedConsumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedConsumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_AuthenticatedUser_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporate.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKeyScaSendsApp, secretKeyScaSendsApp, user.getRight());
        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, user.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, authenticatedCorporateDestination.getRight())
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
                authenticatedCorporate.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKeyScaSendsApp, secretKeyScaSendsApp, user.getRight());

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, user.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, authenticatedCorporateDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId,
                CURRENCY, managedCardId, sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, authenticatedCorporate.getRight())
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
                authenticatedCorporate.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKeyScaSendsApp, secretKeyScaSendsApp, user.getRight());

        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporate.getRight(),
                corporateManagedAccount.getLeft(),
                CURRENCY, corporateManagedCard.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_SendBySmsUserNotEnrolledWithOkay_ChannelNotRegistered() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(),
                secretKeyScaSendsApp, corporate.getRight());

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, authenticatedCorporateDestination.getRight())
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
    public void StartVerification_SendByAuthyUserNotEnrolledWithOkay_ChannelNotRegistered() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp,
                authenticatedCorporate.getRight());
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKeyScaSendsApp,
                user.getRight());

        final String sendId = identityDepositAndSendMaToMc(authenticatedCorporate.getRight(),
                corporateManagedAccount.getLeft(),
                CURRENCY, corporateManagedCard.getLeft(), sendsProfileIdScaSendsApp, secretKeyScaSendsApp).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, authenticatedCorporate.getRight())
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

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, authenticatedCorporateDestination.getRight())
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

        SecureHelper.enrolBiometricUser(corporate.getRight(), sharedKeyScaSendsApp);

        SimulatorHelper.rejectOkayIdentity(secretKeyScaSendsApp, corporate.getLeft(), corporate.getRight(),
                State.INACTIVE);

        final String managedAccountId = createFundedManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, corporate.getRight(), secretKeyScaSendsApp);
        final String managedCardId = createPrepaidManagedCard(
                corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, authenticatedCorporateDestination.getRight())
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


    private static Pair<String, String> consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setBaseCurrency(CURRENCY)
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, secretKeyScaSendsApp);
        final String consumerId = authenticatedConsumer.getLeft();
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKeyScaSendsApp, consumerId);

        SecureHelper.enrolAndVerifyBiometric(consumerId, sharedKeyScaSendsApp, secretKeyScaSendsApp, consumerAuthenticationToken);

        return Pair.of(consumerId, consumerAuthenticationToken);
    }

    private static Pair<String, String> corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(CURRENCY)
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(
                createCorporateModel, secretKeyScaSendsApp);
        String corporateId = authenticatedCorporate.getLeft();
        String corporateAuthenticationToken = authenticatedCorporate.getRight();

        SecureHelper.enrolAndVerifyBiometric(corporateId, sharedKeyScaSendsApp, secretKeyScaSendsApp, corporateAuthenticationToken);

        return Pair.of(corporateId, corporateAuthenticationToken);
    }

    private static List<Pair<String, SendFundsModel>> identityDepositAndSendMaToMc(final String token,
                                                                                   final String identityManagedAccountId,
                                                                                   final String identityCurrency,
                                                                                   final String identityManagedCardId,
                                                                                   final String sendsProfileId,
                                                                                   final String secretKey) {
        final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

        fundManagedAccount(identityManagedAccountId, identityCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(identityCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(identityManagedAccountId, MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(identityManagedCardId, MANAGED_CARDS))
                            .build();

            final String id =
                    SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
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
                                                     final String secretKey) {
        final String managedAccountId =
                createManagedAccount(profile, Currency.EUR.name(), secretKey, token)
                        .getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        return managedAccountId;
    }
}
