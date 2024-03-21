package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.junit.database.SendsDatabaseHelper;
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
import opc.services.multi.AuthenticationService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SENDS)
public class StartSendPushVerificationTests extends BaseSendsSetup {

    private static final String CHANNEL = EnrolmentChannel.AUTHY.name();

    private static String corporateAuthenticationTokenSource;
    private static String consumerAuthenticationTokenSource;
    private static String corporateCurrencySource;
    private static String consumerCurrencySource;
    private static String corporateIdSource;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccountSource;
    private static Pair<String, CreateManagedCardModel> corporateManagedCardDestination;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountSource;
    private static Pair<String, CreateManagedCardModel> consumerManagedCardDestination;
    private static String consumerAuthenticationTokenDestination;
    private static String corporateAuthenticationTokenDestination;


    @BeforeAll
    public static void Setup() {
        final String adminToken = AdminService.loginAdmin();
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

    @ParameterizedTest
    @ValueSource(strings = {"INITIALISED", "PENDING_SCA", "EXPIRED_SCA", "VERIFIED_SCA"})
    public void StartVerification_PreSubmissionStateNotInPendingChallenge_Conflict(
            final String state) {

        final String finalState;

        switch (state) {
            case "EXPIRED_SCA":
                finalState = "REJECTED";
                break;
            case "PENDING_SCA":
                finalState = "PENDING_CHALLENGE";
                break;
            case "VERIFIED_SCA":
                finalState = "APPROVED";
                break;
            default:
                finalState = "INITIALISED";
        }

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsDatabaseHelper.updateSendState(state, sendId);

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(finalState));
    }

    @Test
    public void StartVerification_UserNotEnrolled_UserNotEnrolledOnChallenge() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKeyScaSendsApp);

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> consumerDestination =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModelDestination, secretKeyScaSendsApp);

        final String managedAccountId = createFundedManagedAccount(
                consumerManagedAccountProfileIdScaSendsApp,
                consumer.getRight());
        final String managedCardId = createPrepaidManagedCard(
                consumerPrepaidManagedCardsProfileIdScaSendsApp,
                Currency.EUR.name(),
                secretKeyScaSendsApp, consumerDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(consumer.getRight(), managedAccountId,
                createConsumerModel.getBaseCurrency(), managedCardId).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void StartVerification_CrossIdentity_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource,
                consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_DifferentIdentity_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyScaSendsApp,
                corporate.getRight());

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_CrossIdentityUser_NotFound() {
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

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_NoSendId_NotFound() {

        SendsService.startSendPushVerification("", CHANNEL, secretKeyScaSendsApp,
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_UnknownSendId_NotFound() {

        SendsService.startSendPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL,
                        secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    public void StartVerification_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, enrolmentChannel.name(), secretKeyScaSendsApp,
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_NoChannel_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, "", secretKeyScaSendsApp,
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_DifferentInnovatorApiKey_Forbidden() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("secretKey");

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, otherInnovatorSecretKey,
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_FORBIDDEN);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_UserLoggedOut_Unauthorised() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyScaSendsApp);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyScaSendsApp,
                corporate.getRight());

        AuthenticationService.logout(secretKeyScaSendsApp, corporate.getRight());

        SendsService.startSendPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL,
                        secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_InvalidApiKey_Unauthorised() {

        SendsService.startSendPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "abc",
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_NoApiKey_BadRequest() {

        SendsService.startSendPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "",
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartVerification_BackofficeImpersonator_Forbidden() {

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.startSendPushVerification(sendId, CHANNEL, secretKeyScaSendsApp,
                        getBackofficeImpersonateToken(corporateIdSource, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, secretKeyScaSendsApp);
        String consumerId = authenticatedConsumer.getLeft();
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
        corporateIdSource = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrencySource = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateIdSource);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateIdSource, secretKeyScaSendsApp,
                corporateAuthenticationTokenSource);
    }
    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, secretKeyScaSendsApp);
        final String consumerIdDestination = authenticatedConsumer.getLeft();
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();
        ConsumersHelper.verifyKyc(secretKeyScaSendsApp, consumerIdDestination);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerIdDestination, secretKeyScaSendsApp,
                consumerAuthenticationTokenDestination);
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, secretKeyScaSendsApp);
        final String corporateIdDestination = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateIdDestination);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateIdDestination, secretKeyScaSendsApp,
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
}
