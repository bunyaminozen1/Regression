package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import commons.enums.State;
import opc.junit.database.SendsDatabaseHelper;
import opc.junit.helpers.TestHelper;
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
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SENDS)
public class VerifyOtpSendTests extends BaseSendsSetup {
    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";

    private static String corporateAuthenticationTokenSource;
    private static String consumerAuthenticationTokenSource;
    private static String corporateCurrencySource;
    private static String consumerCurrencySource;
    private static String consumerIdSource;
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
                createManagedAccount(corporateManagedAccountProfileIdScaSendsApp, corporateCurrencySource, secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        corporateManagedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, corporateCurrencySource, secretKeyScaSendsApp, corporateAuthenticationTokenDestination);
        consumerManagedAccountSource =
                createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerCurrencySource, secretKeyScaSendsApp, consumerAuthenticationTokenSource);
        consumerManagedCardDestination =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileIdScaSendsApp, consumerCurrencySource, secretKeyScaSendsApp, consumerAuthenticationTokenDestination);
    }

    @Test
    public void VerifySend_Corporate_Success() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, corporateAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void VerifySend_Consumer_Success() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void VerifySend_AuthenticatedUser_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, userDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, user.getRight());
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId, corporateCurrencySource, managedCardId).get(0).getLeft();

        startVerification(sendId, user.getRight());

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, user.getRight(), State.COMPLETED);
    }

    @Test
    public void VerifySend_RootVerifySendStartedByUser_ChannelNotRegistered() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, userDestination.getRight());


        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, user.getRight());
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(), managedAccountId, corporateCurrencySource, managedCardId).get(0).getLeft();
        startVerification(sendId, user.getRight());

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_UserVerifySendStartedByRoot_ChannelNotRegistered() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, userDestination.getRight());

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INITIALISED", "COMPLETED", "REJECTED", "FAILED"})
    public void VerifySend_StateNotInPendingVerification_Conflict(final String state) {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsDatabaseHelper.updateSendState(state, sendId);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(state));
    }

    @Test
    public void VerifySend_DifferentIdentity_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void VerifySend_CrossIdentity_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_CrossIdentityUser_NotFound() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, user.getRight());

        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);
        final UsersModel updateUserDestination = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUserDestination, secretKeyScaSendsApp, userDestination.getLeft(), userDestination.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, userDestination.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, user.getRight());
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(user.getRight(),
                managedAccountId, corporateCurrencySource, managedCardId)
                .get(0).getLeft();

        startVerification(sendId, user.getRight());

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_NoSendId_NotFound() {
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), "",
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifySend_UnknownSendId_NotFound() {

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @DisplayName("VerifySend_UnknownChannel_BadRequest - DEV-6848 opened to return 404")
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    public void VerifySend_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        enrolmentChannel.name(), secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    @DisplayName("VerifySend_NoChannel_NotFound - DEV-6848 opened to return 404")
    public void VerifySend_NoChannel_NotFound() {

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        "", secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();
        startVerification(sendId, corporateAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, otherInnovatorSecretKey, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_FORBIDDEN);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_UserLoggedOut_Unauthorised() {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, corporate.getRight());

        final Pair<String, String> corporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);

        final String managedCardIdDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, corporateDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(corporate.getRight(), managedAccountId,
                "EUR", managedCardIdDestination).get(0).getLeft();
        startVerification(sendId, corporate.getRight());

        AuthenticationService.logout(secretKeyScaSendsApp, corporate.getRight());

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySend_InvalidApiKey_Unauthorised() {

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                        CHANNEL, "abc", corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySend_NoApiKey_BadRequest() {

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                        CHANNEL, "", corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifySend_BackofficeImpersonator_Forbidden() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, getBackofficeImpersonateToken(consumerIdSource, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_UnknownVerificationCode_Conflict() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(RandomStringUtils.randomNumeric(6)), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void VerifySend_NoVerificationCode_BadRequest(final String verificationCode) {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(verificationCode), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_ChallengeExpired_Expired() throws InterruptedException {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, corporateAuthenticationTokenSource);

        Thread.sleep(61000);

        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));

        assertSendState(sendId, corporateAuthenticationTokenSource, State.PENDING_CHALLENGE);
    }

    @Test
    public void VerifySend_UnknownVerificationCode_VerificationCodeInvalid() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(RandomStringUtils.randomNumeric(6)), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "12345"})
    public void VerifySend_VerificationCodeLengthChecks_BadRequest(final String verificationCode) {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(verificationCode), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: size must be between 6 and 6"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySend_InvalidVerificationCode_BadRequest() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        startVerification(sendId, consumerAuthenticationTokenSource);

        SendsService.verifySendOtp(new VerificationModel(RandomStringUtils.randomAlphabetic(6)), sendId,
                        CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must match \"^[0-9]*$\""));

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, secretKeyScaSendsApp);
        consumerIdSource = authenticatedConsumer.getLeft();
        consumerAuthenticationTokenSource = authenticatedConsumer.getRight();
        consumerCurrencySource = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKeyScaSendsApp, consumerIdSource);
    }

    private static void corporateSetupSource() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKeyScaSendsApp);
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrencySource = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, authenticatedCorporate.getLeft());
    }

    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, secretKeyScaSendsApp);
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKeyScaSendsApp, authenticatedConsumer.getLeft());
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKeyScaSendsApp);
        corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, authenticatedCorporate.getLeft());
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

    private void startVerification(final String id,
                                   final String token) {

        SendsService.startSendOtpVerification(id, CHANNEL, secretKeyScaSendsApp, token)
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
