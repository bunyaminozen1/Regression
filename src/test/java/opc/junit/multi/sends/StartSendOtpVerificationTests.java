package opc.junit.multi.sends;

import opc.enums.mailhog.MailHogSms;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsSimulatorDatabaseHelper;
import opc.junit.database.SendsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.admin.ImpersonateIdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import commons.models.MobileNumberModel;
import opc.services.admin.AdminService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.text.DecimalFormat;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(MultiTags.SENDS)
public class StartSendOtpVerificationTests extends BaseSendsSetup {

    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";
    private static final long DEFAULT_SEND_AMOUNT = 100L;

    private static String corporateAuthenticationTokenSource;
    private static String consumerAuthenticationTokenSource;
    private static String corporateCurrencySource;
    private static String consumerCurrencySource;
    private static String consumerIdSource;
    private static String corporateIdSource;

    private static Pair<String, CreateManagedAccountModel> corporateManagedAccountSource;
    private static Pair<String, CreateManagedCardModel> corporateManagedCardDestination;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountSource;
    private static Pair<String, CreateManagedCardModel> consumerManagedCardDestination;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountDestination;

    private static CreateCorporateModel createCorporateModelSource;
    private static CreateConsumerModel createConsumerModelSource;
    private static CreateConsumerModel createConsumerModelDestination;
    private static String consumerAuthenticationTokenDestination;
    private static String corporateAuthenticationTokenDestination;

    private static Pair<String, String> userSource;
    private static Pair<String, String> userDestination;


    @BeforeAll
    public static void Setup() throws SQLException {

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
        consumerManagedAccountDestination =
                createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, createConsumerModelDestination.getBaseCurrency(), secretKeyScaSendsApp, consumerAuthenticationTokenDestination);

    }

    @Test
    public void StartVerification_Corporate_Success() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_Consumer_Success() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifySenderId_CorporateUK_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+44", String.format("0203%s", RandomStringUtils.randomNumeric(7))))
                                .build())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyScaSendsApp);
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, corporateAuthenticationToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals("WEAVR", newsSms.getFrom());

        final String corporateManagedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, corporateAuthenticationToken);

        final String owtTransactionId = OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileIdSca, corporateManagedAccountId,
                new CurrencyAmount("EUR", 100L), secretKeyScaSendsApp, corporateAuthenticationToken).getLeft();
        OutgoingWireTransfersHelper.verifyOwtOtp(owtTransactionId, secretKeyScaSendsApp, corporateAuthenticationToken);

        final MailHogMessageResponse newsSmsOWT = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals("WEAVR", newsSmsOWT.getFrom());
    }

    @Test
    public void VerifySenderId_ConsumerUK_Success() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+44", String.format("0203%s", RandomStringUtils.randomNumeric(7))))
                                .build())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKeyScaSendsApp);
        final String consumerId = authenticatedConsumer.getLeft();
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, consumerAuthenticationToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals("WEAVR", newsSms.getFrom());

        final String consumerManagedAccountId = createFundedManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerAuthenticationToken);

        final String transactionId = OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileIdSca, consumerManagedAccountId,
                new CurrencyAmount("EUR", 100L), secretKeyScaSendsApp, consumerAuthenticationToken).getLeft();
        OutgoingWireTransfersHelper.verifyOwtOtp(transactionId, secretKeyScaSendsApp, consumerAuthenticationToken);

        final MailHogMessageResponse newsSmsOWT = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals("WEAVR", newsSmsOWT.getFrom());
    }

    @Test
    public void VerifySenderId_CorporateUSA_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+1", String.format("201555%s", RandomStringUtils.randomNumeric(4))))
                                .build())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyScaSendsApp);
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, corporateAuthenticationToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-us@weavr.io", newsSms.getFrom());

        final String corporateManagedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, corporateAuthenticationToken);

        final String owtTransactionId = OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileIdSca, corporateManagedAccountId,
                new CurrencyAmount("EUR", 100L), secretKeyScaSendsApp, corporateAuthenticationToken).getLeft();
        OutgoingWireTransfersHelper.verifyOwtOtp(owtTransactionId, secretKeyScaSendsApp, corporateAuthenticationToken);

        final MailHogMessageResponse newsSmsOWT = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-us@weavr.io", newsSmsOWT.getFrom());
    }

    @Test
    public void VerifySenderId_ConsumerUSA_Success() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setMobile(new MobileNumberModel("+1", String.format("201555%s", RandomStringUtils.randomNumeric(4))))
                                .build())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKeyScaSendsApp);
        final String consumerId = authenticatedConsumer.getLeft();
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, consumerAuthenticationToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-us@weavr.io", newsSms.getFrom());

        final String consumerManagedAccountId = createFundedManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerAuthenticationToken);

        final String transactionId = OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileIdSca, consumerManagedAccountId,
                new CurrencyAmount("EUR", 100L), secretKeyScaSendsApp, consumerAuthenticationToken).getLeft();
        OutgoingWireTransfersHelper.verifyOwtOtp(transactionId, secretKeyScaSendsApp, consumerAuthenticationToken);

        final MailHogMessageResponse newsSmsOWT = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-us@weavr.io", newsSmsOWT.getFrom());
    }

    @Test
    public void VerifySenderId_CorporateNonUK_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyScaSendsApp);
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, corporateAuthenticationToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-qa@weavr.io", newsSms.getFrom());

        final String corporateManagedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, corporateAuthenticationToken);

        final String owtTransactionId = OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileIdSca, corporateManagedAccountId,
                new CurrencyAmount("EUR", 100L), secretKeyScaSendsApp, corporateAuthenticationToken).getLeft();
        OutgoingWireTransfersHelper.verifyOwtOtp(owtTransactionId, secretKeyScaSendsApp, corporateAuthenticationToken);

        final MailHogMessageResponse newsSmsOWT = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-qa@weavr.io", newsSmsOWT.getFrom());
    }

    @Test
    public void VerifySenderId_ConsumerNonUK_Success() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKeyScaSendsApp);
        final String consumerId = authenticatedConsumer.getLeft();
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, consumerAuthenticationToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-qa@weavr.io", newsSms.getFrom());

        final String consumerManagedAccountId = createFundedManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerAuthenticationToken);

        final String transactionId = OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileIdSca, consumerManagedAccountId,
                new CurrencyAmount("EUR", 100L), secretKeyScaSendsApp, consumerAuthenticationToken).getLeft();
        OutgoingWireTransfersHelper.verifyOwtOtp(transactionId, secretKeyScaSendsApp, consumerAuthenticationToken);

        final MailHogMessageResponse newsSmsOWT = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals("noreply-qa@weavr.io", newsSmsOWT.getFrom());
    }

    @Test
    public void StartVerification_AuthenticatedUser_Success() {
        userSource = setupUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        userDestination = setupUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, userSource.getRight());
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(userSource.getRight(), managedAccountId, corporateCurrencySource, managedCardId).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, userSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, userSource.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, userSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_CorporateSmsChecks_Success() throws SQLException {
        final List<Pair<String, SendFundsModel>> identitySend = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft());

        SendsService.startSendOtpVerification(identitySend.get(0).getLeft(), CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createCorporateModelSource.getRootUser().getMobile().getNumber());

        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertNull(sms.getSubject());
        assertEquals(String.format("%s%s@weavr.io", createCorporateModelSource.getRootUser().getMobile().getCountryCode(),
                createCorporateModelSource.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format("ScaSendsApp verification code for payment of %s%s to Card Number: **** %s is %s. It will expire in 1 minutes.",
                        corporateCurrencySource,
                        new DecimalFormat("0.00").format(DEFAULT_SEND_AMOUNT / 100),
                        ManagedCardsHelper.getCardNumberLastFour(secretKeyScaSendsApp, identitySend.get(0).getRight().getDestination().getId(), corporateAuthenticationTokenDestination),
                        AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(corporateIdSource).get(0).get("token")),
                sms.getBody());
    }

    @Test
    public void StartVerification_ConsumerSmsChecks_Success() throws SQLException {
        final List<Pair<String, SendFundsModel>> identitySend = identityDepositAndSendMaToMa(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedAccountDestination.getLeft());

        SendsService.startSendOtpVerification(identitySend.get(0).getLeft(), CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createConsumerModelSource.getRootUser().getMobile().getNumber());

        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertNull(sms.getSubject());
        assertEquals(String.format("%s%s@weavr.io", createConsumerModelSource.getRootUser().getMobile().getCountryCode(),
                createConsumerModelSource.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format("ScaSendsApp verification code for payment of %s%s to %s %s. Account is %s. It will expire in 1 minutes.",
                        consumerCurrencySource,
                        new DecimalFormat("0.00").format(DEFAULT_SEND_AMOUNT / 100),
                        createConsumerModelDestination.getRootUser().getName(),
                        createConsumerModelDestination.getRootUser().getSurname().charAt(0),
                        AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(consumerIdSource).get(0).get("token")),
                sms.getBody());
    }

    @Test
    public void StartVerification_AuthenticatedUserSmsChecks_Success() throws SQLException {
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

        final SendFundsModel sendFundsModel =
                SendFundsModel.DefaultSendsModel(sendsProfileIdScaSendsApp, managedAccountId, managedCardId,
                        Currency.EUR.name(), DEFAULT_SEND_AMOUNT).build();

        final String sendId = sendFunds(sendFundsModel, user.getRight());

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(updateUser.getMobile().getNumber());

        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertNull(sms.getSubject());
        assertEquals(String.format("%s%s@weavr.io", updateUser.getMobile().getCountryCode(),
                updateUser.getMobile().getNumber()), sms.getTo());
        assertEquals(String.format("ScaSendsApp verification code for payment of %s%s to Card Number: **** %s is %s. It will expire in 1 minutes.",
                        corporateCurrencySource,
                        new DecimalFormat("0.00").format(DEFAULT_SEND_AMOUNT / 100),
                        ManagedCardsHelper.getCardNumberLastFour(secretKeyScaSendsApp, managedCardId, userDestination.getRight()),
                        AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(user.getLeft()).get(0).get("token")),
                sms.getBody());
    }

    @Test
    public void StartVerification_RootStartUserVerification_Success() {
        userSource = setupUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        userDestination = setupUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, userSource.getRight());
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(userSource.getRight(), managedAccountId, corporateCurrencySource, managedCardId).get(0).getLeft();

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, userSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_UserStartRootVerification_Success() {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKeyScaSendsApp, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, user.getRight());

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INITIALISED", "COMPLETED", "REJECTED", "FAILED"})
    public void StartVerification_PostSubmissionStateNotInPendingChallenge_Conflict(final String state) {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsDatabaseHelper.updateSendState(state, sendId);

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(state));
    }

    @Test
    public void StartVerification_UserNotEnrolled_UserNotEnrolledOnChallenge() {
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp).build(), secretKeyScaSendsApp);

        final Pair<String, String> consumerDestination =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp).build(), secretKeyScaSendsApp);

        final String managedAccountId = createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerCurrencySource, secretKeyScaSendsApp, consumer.getRight()).getLeft();
        final String managedCardId = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileIdScaSendsApp, consumerCurrencySource, secretKeyScaSendsApp, consumerDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(consumer.getRight(),
                managedAccountId, consumerCurrencySource, managedCardId)
                .get(0).getLeft();

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void StartVerification_UserEnrolmentNotVerified_UserNotEnrolledOnChallenge() {
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp).build(), secretKeyScaSendsApp);
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, consumer.getRight());

        final Pair<String, String> consumerDestination =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp).build(), secretKeyScaSendsApp);
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKeyScaSendsApp, consumerDestination.getRight());

        final String managedAccountId = createManagedAccount(consumerManagedAccountProfileIdScaSendsApp, consumerCurrencySource, secretKeyScaSendsApp, consumer.getRight()).getLeft();
        final String managedCardId = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileIdScaSendsApp, consumerCurrencySource, secretKeyScaSendsApp, consumerDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(consumer.getRight(),
                managedAccountId, consumerCurrencySource, managedCardId)
                .get(0).getLeft();

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void StartVerification_CrossIdentity_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_DifferentIdentity_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        final Pair<String, String> consumer =
                ConsumersHelper.createEnrolledVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdScaSendsApp).build(), secretKeyScaSendsApp);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));
    }

    @Test
    public void StartVerification_CrossIdentityUser_NotFound() {
        userSource = setupUser(secretKeyScaSendsApp, corporateAuthenticationTokenSource);
        userDestination = setupUser(secretKeyScaSendsApp, corporateAuthenticationTokenDestination);

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileIdScaSendsApp, userSource.getRight());
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdScaSendsApp, Currency.EUR.name(),
                secretKeyScaSendsApp, userDestination.getRight())
                .getLeft();

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                managedAccountId, corporateCurrencySource, managedCardId)
                .get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, userSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_NoSendId_NotFound() {
        SendsService.startSendOtpVerification("", CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_EmptySendId_NotFound() {
        SendsService.startSendOtpVerification("", CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_UnknownSendId_NotFound() {
        SendsService.startSendOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    @DisplayName("StartVerification_UnknownChannel_BadRequest - DEV-6848 opened to return 404")
    public void StartVerification_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, enrolmentChannel.name(), secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartVerification_NoChannel_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, "", secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_EmptyChannelValue_NotFound() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, "", secretKeyScaSendsApp, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_DifferentInnovatorApiKey_Forbidden() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, otherInnovatorSecretKey, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartVerification_UserLoggedOut_Unauthorised() {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);

        AuthenticationService.logout(secretKeyScaSendsApp, corporate.getRight());

        SendsService.startSendOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, secretKeyScaSendsApp, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_InvalidApiKey_Unauthorised() {
        SendsService.startSendOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "abc", corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_NoApiKey_BadRequest() {
        SendsService.startSendOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "", corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartVerification_BackofficeImpersonator_Forbidden() {
        final String sendId = identityDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, getBackofficeImpersonateToken(consumerIdSource, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartVerification_PerpetualTokenCorporate_Success() {

        final String adminToken = AdminService.loginAdmin();
        final ImpersonateIdentityModel impersonateIdentityModelScaApp = new ImpersonateIdentityModel(programmeIdScaSendsApp, corporateIdSource);
        final String perpetualToken = AdminService.impersonateIdentity(impersonateIdentityModelScaApp, adminToken);

        final String sendId = identityDepositAndSendMaToMc(perpetualToken, corporateManagedAccountSource.getLeft(),
                corporateCurrencySource, corporateManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("COMPLETED"));
    }

    @Test
    public void StartVerification_ConsumerCallEndpointTwice_Success() {
        final String sendId = identityDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccountSource.getLeft(),
                consumerCurrencySource, consumerManagedCardDestination.getLeft()).get(0).getLeft();

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);

        SendsService.startSendOtpVerification(sendId, CHANNEL, secretKeyScaSendsApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        SendsService.getSend(secretKeyScaSendsApp, sendId, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    private static void consumerSetupSource() {
        createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelSource, secretKeyScaSendsApp);
        consumerIdSource = authenticatedConsumer.getLeft();
        consumerAuthenticationTokenSource = authenticatedConsumer.getRight();
        consumerCurrencySource = createConsumerModelSource.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKeyScaSendsApp, consumerIdSource);
    }

    private static void corporateSetupSource() {
        createCorporateModelSource =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelSource, secretKeyScaSendsApp);
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrencySource = createCorporateModelSource.getBaseCurrency();
        corporateIdSource = authenticatedCorporate.getLeft();

        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, authenticatedCorporate.getLeft());
    }

    private static void consumerSetupDestination() {
        createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModelDestination, secretKeyScaSendsApp);
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKeyScaSendsApp, authenticatedConsumer.getLeft());
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdScaSendsApp).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelDestination, secretKeyScaSendsApp);
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
                            .setDestinationAmount(new CurrencyAmount(identityCurrency, DEFAULT_SEND_AMOUNT))
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

    private static List<Pair<String, SendFundsModel>> identityDepositAndSendMaToMa(final String token,
                                                                                   final String sourceIdentityManagedAccountId,
                                                                                   final String identityCurrency,
                                                                                   final String destinationIdentityManagedAccountId) {
        final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

        fundManagedAccount(sourceIdentityManagedAccountId, identityCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileIdScaSendsApp)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(identityCurrency, DEFAULT_SEND_AMOUNT))
                            .setSource(new ManagedInstrumentTypeId(sourceIdentityManagedAccountId, MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(destinationIdentityManagedAccountId, MANAGED_ACCOUNTS))
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

    private String sendFunds(final SendFundsModel sendFundsModel,
                             final String token) {
        return SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static Pair<String, String> setupUser(String secretKey, String token) {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, token);
        final UsersModel updateUser = UsersModel.DefaultUsersModel().build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());
        return user;
    }

}
