package opc.junit.multi.sends;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedInstrumentType;
import commons.enums.State;
import opc.helpers.SendModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.BulkSendResponseModel;
import opc.models.multi.sends.BulkSendsResponseModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.VerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SENDS)
public class SendsLowValueExemptionTests extends BaseSendsSetup {

    private static final String CHANNEL_OTP = EnrolmentChannel.SMS.name();
    private static final String CHANNEL_AUTHY = EnrolmentChannel.AUTHY.name();
    private static final String VERIFICATION_CODE = "123456";


    @BeforeAll
    public static void Setup() {
        final String adminToken = AdminService.loginAdmin();
        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeIdLowValueExemptionApp, adminToken);
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeIdLowValueExemptionApp, resetCount, adminToken);

        AdminHelper.setConsumerLowValueLimit(adminToken);
        AdminHelper.setCorporateLowValueLimit(adminToken);
    }

    /**
     * Low Value Exemption Limits for transactions: maxSum = 100, maxCount = 5.
     */

    @Test
    public void SendLowValueExemption_ConsumerOtpExceedAmountLimit_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedConsumerSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenSource = authenticatedConsumerSource.getRight();
        final String consumerCurrency = createConsumerModelSource.getBaseCurrency();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(consumerCurrency)
                        .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 30L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, consumerAuthenticationTokenSource);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerOtpExceedCountLimit_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedConsumerSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenSource = authenticatedConsumerSource.getRight();
        final String consumerCurrency = createConsumerModelSource.getBaseCurrency();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(consumerCurrency)
                        .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccountSource = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCardDestination = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationTokenSource,
                consumerManagedAccountSource.getLeft(),
                consumerCurrency, consumerManagedCardDestination.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationTokenSource,
                consumerManagedAccountSource.getLeft(),
                consumerCurrency, consumerManagedCardDestination.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, consumerAuthenticationTokenSource);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerOtpExceedCountAndAmountLimits_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationToken = authenticatedConsumer.getRight();
        final String consumerCurrency = createConsumerModelSource.getBaseCurrency();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(consumerCurrency)
                        .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationToken, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, consumerAuthenticationToken);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateOtpExceedAmountLimit_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModel, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelDestination, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenDestination = authenticatedCorporateDestination.getRight();

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, corporateAuthenticationToken);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateOtpExceedCountLimit_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModel, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelDestination, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenDestination = authenticatedCorporateDestination.getRight();

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, corporateAuthenticationToken);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateOtpExceedCountAndAmountLimits_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModel, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelDestination, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenDestination = authenticatedCorporateDestination.getRight();

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, corporateAuthenticationToken);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerAuthyExceedAmountLimit_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKeyLowValueExemptionApp);

        final String consumerAuthenticationToken = consumer.getRight();
        final String consumerCurrency = createConsumerModel.getBaseCurrency();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKeyLowValueExemptionApp,
                consumerAuthenticationToken);

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> consumerDestination =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModelDestination, secretKeyLowValueExemptionApp);

        final String consumerAuthenticationTokenDestination = consumerDestination.getRight();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerDestination.getLeft(), secretKeyLowValueExemptionApp,
                consumerAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 30L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationToken, State.PENDING);

        // Perform verification for the pending send
        startAuthyPushVerification(pendingSendId, consumerAuthenticationToken);
        SimulatorService.acceptAuthySend(secretKeyLowValueExemptionApp, pendingSendId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(pendingSendId, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerOtpNotExceedMaxAmountLimit_Success() {
        final Currency consumerCurrency = Currency.GBP;

        //Set low value limit for the programme wit specific currency and maximum amount
        AdminHelper.setConsumerLowValueLimitWithCurrency(programmeIdLowValueExemptionApp, consumerCurrency, AdminService.loginAdmin());
        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.CurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp, Currency.GBP).build();
        final Pair<String, String> authenticatedConsumerSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenSource = authenticatedConsumerSource.getRight();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(consumerCurrency.name())
                        .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency.name(), consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency.name(), consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 31L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency.name(), consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency.name(), consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, consumerAuthenticationTokenSource);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerOtpExceedMaxAmountLimit_Success() {
        //Set low value limit for the programme wit specific currency and maximum amount
        AdminHelper.setConsumerLowValueLimitWithCurrency(programmeIdLowValueExemptionApp, Currency.EUR, AdminService.loginAdmin());

        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.CurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp, Currency.GBP).build();
        final Pair<String, String> authenticatedConsumerSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenSource = authenticatedConsumerSource.getRight();
        final String consumerCurrency = createConsumerModelSource.getBaseCurrency();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(consumerCurrency)
                        .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Max Value Limits -> PENDING state
        final Long destinationAmount = 33L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("PENDING"))
                .body("send[1].state", equalTo("PENDING"))
                .body("send[2].state", equalTo("PENDING"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, consumerAuthenticationTokenSource);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateOtpNotExceedMaxAmountLimit_Success() {
        //Set low value limit for the programme wit specific currency and maximum amount 25
        AdminHelper.setCorporateLowValueLimitWithCurrency(programmeIdLowValueExemptionApp, Currency.EUR, AdminService.loginAdmin());
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModel, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelDestination, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenDestination = authenticatedCorporateDestination.getRight();

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, corporateAuthenticationToken);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateOtpExceedMaxAmountLimit_Success() {
        //Set low value limit for the programme wit specific currency and maximum amount 25
        AdminHelper.setCorporateLowValueLimitWithCurrency(programmeIdLowValueExemptionApp, Currency.EUR, AdminService.loginAdmin());
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModel, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelDestination, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenDestination = authenticatedCorporateDestination.getRight();

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Max Value Limits -> PENDING state
        final Long destinationAmount = 26L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("PENDING"))
                .body("send[1].state", equalTo("PENDING"))
                .body("send[2].state", equalTo("PENDING"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform OTP verification for the pending send
        startVerification(pendingSendId, corporateAuthenticationToken);
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), pendingSendId,
                        CHANNEL_OTP, secretKeyLowValueExemptionApp, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerAuthyExceedCountLimit_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKeyLowValueExemptionApp);

        final String consumerAuthenticationToken = consumer.getRight();
        final String consumerCurrency = createConsumerModel.getBaseCurrency();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKeyLowValueExemptionApp,
                consumerAuthenticationToken);

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> consumerDestination =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModelDestination, secretKeyLowValueExemptionApp);

        final String consumerAuthenticationTokenDestination = consumerDestination.getRight();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerDestination.getLeft(), secretKeyLowValueExemptionApp,
                consumerAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationToken, State.PENDING);

        // Perform verification for the pending send
        startAuthyPushVerification(pendingSendId, consumerAuthenticationToken);
        SimulatorService.acceptAuthySend(secretKeyLowValueExemptionApp, pendingSendId)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_ConsumerAuthyExceedCountAndAmountLimits_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKeyLowValueExemptionApp);

        final String consumerAuthenticationToken = consumer.getRight();
        final String consumerCurrency = createConsumerModel.getBaseCurrency();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKeyLowValueExemptionApp,
                consumerAuthenticationToken);

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> consumerDestination =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModelDestination, secretKeyLowValueExemptionApp);

        final String consumerAuthenticationTokenDestination = consumerDestination.getRight();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerDestination.getLeft(), secretKeyLowValueExemptionApp,
                consumerAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        sendsInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationToken,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationToken, State.PENDING);

        // Perform verification for the pending send
        startAuthyPushVerification(pendingSendId, consumerAuthenticationToken);
        SimulatorService.acceptAuthySend(secretKeyLowValueExemptionApp, pendingSendId)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateAuthyExceedAmountLimit_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyLowValueExemptionApp);

        final String corporateAuthenticationToken = corporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyLowValueExemptionApp,
                corporateAuthenticationToken);

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyLowValueExemptionApp);

        final String corporateAuthenticationTokenDestination = corporateDestination.getRight();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateDestination.getLeft(), secretKeyLowValueExemptionApp,
                corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform verification for the pending send
        startAuthyPushVerification(pendingSendId, corporateAuthenticationToken);
        SimulatorService.acceptAuthySend(secretKeyLowValueExemptionApp, pendingSendId)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateAuthyExceedCountLimit_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyLowValueExemptionApp);

        final String corporateAuthenticationToken = corporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyLowValueExemptionApp,
                corporateAuthenticationToken);

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyLowValueExemptionApp);

        final String corporateAuthenticationTokenDestination = corporateDestination.getRight();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateDestination.getLeft(), secretKeyLowValueExemptionApp,
                corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform verification for the pending send
        startAuthyPushVerification(pendingSendId, corporateAuthenticationToken);
        SimulatorService.acceptAuthySend(secretKeyLowValueExemptionApp, pendingSendId)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_CorporateAuthyExceedCountAndAmountLimits_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        secretKeyLowValueExemptionApp);

        final String corporateAuthenticationToken = corporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyLowValueExemptionApp,
                corporateAuthenticationToken);

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        secretKeyLowValueExemptionApp);

        final String corporateAuthenticationTokenDestination = corporateDestination.getRight();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateDestination.getLeft(), secretKeyLowValueExemptionApp,
                corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationToken, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        sendsInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft());
        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("COMPLETED"))
                .body("send[1].state", equalTo("COMPLETED"))
                .body("send[2].state", equalTo("COMPLETED"))
                .body("send[3].state", equalTo("COMPLETED"))
                .body("send[4].state", equalTo("COMPLETED"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationToken,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), destinationAmount).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationToken, State.PENDING);

        // Perform Authy verification for the pending send
        startAuthyPushVerification(pendingSendId, corporateAuthenticationToken);
        SimulatorService.acceptAuthySend(secretKeyLowValueExemptionApp, pendingSendId)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertSendState(pendingSendId, corporateAuthenticationToken, State.COMPLETED);
    }

    /**
     * Low Value Exemption Limits for transactions: maxSum = 100, maxCount = 5.
     */

    @Test
    public void SendLowValueExemption_ConsumerExemptionInAffectAfterMultiplePendingSends_Success() {
        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedConsumerSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenSource = authenticatedConsumerSource.getRight();
        final String consumerCurrency = createConsumerModelSource.getBaseCurrency();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                        .setBaseCurrency(consumerCurrency)
                        .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCard = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends above Low Value Limits -> PENDING state
        IntStream.range(0, 5).forEach(i -> identityLowValueDepositAndSendMaToMc(consumerAuthenticationTokenSource, consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), 200L));

        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("PENDING"))
                .body("send[1].state", equalTo("PENDING"))
                .body("send[2].state", equalTo("PENDING"))
                .body("send[3].state", equalTo("PENDING"))
                .body("send[4].state", equalTo("PENDING"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(consumerAuthenticationTokenSource,
                consumerManagedAccount.getLeft(),
                consumerCurrency, consumerManagedCard.getLeft(), 30L).get(0).getLeft();
        assertSendState(pendingSendId, consumerAuthenticationTokenSource, State.COMPLETED);
    }/**
     * Low Value Exemption Limits for transactions: maxSum = 100, maxCount = 5.
     */

    @Test
    public void SendLowValueExemption_CorporateExemptionInAffectAfterMultiplePendingSends_Success() {
        // Set up Identity
        final CreateCorporateModel createCorporateModelSource =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedCorporateSource = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelSource, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenSource = authenticatedCorporateSource.getRight();
        final String corporateCurrency = createCorporateModelSource.getBaseCurrency();

        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdLowValueExemptionApp)
                        .setBaseCurrency(corporateCurrency)
                        .build();
        final Pair<String, String> authenticatedCorporateDestination = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModelDestination, secretKeyLowValueExemptionApp);
        final String corporateAuthenticationTokenDestination = authenticatedCorporateDestination.getRight();

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount = createIdentityManagedAccount(
                corporateManagedAccountProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> corporateManagedCard = createIdentityManagedCard(
                corporatePrepaidManagedCardsProfileIdLowValueExemptionApp,
                corporateCurrency, corporateAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        // Sends above Low Value Limits -> PENDING state
        IntStream.range(0, 5).forEach(i -> identityLowValueDepositAndSendMaToMc(corporateAuthenticationTokenSource, corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), 200L));

        final ValidatableResponse response = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(),
                        corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK);
        response
                .body("send[0].state", equalTo("PENDING"))
                .body("send[1].state", equalTo("PENDING"))
                .body("send[2].state", equalTo("PENDING"))
                .body("send[3].state", equalTo("PENDING"))
                .body("send[4].state", equalTo("PENDING"));

        // Send exceeding the amount limit -> PENDING state
        final String pendingSendId = identityLowValueDepositAndSendMaToMc(corporateAuthenticationTokenSource,
                corporateManagedAccount.getLeft(),
                corporateCurrency, corporateManagedCard.getLeft(), 25L).get(0).getLeft();
        assertSendState(pendingSendId, corporateAuthenticationTokenSource, State.COMPLETED);
    }

    @Test
    public void SendLowValueExemption_LowValueExemptionSendBulk_Success()  {
        // Set up Identity
        final CreateConsumerModel createConsumerModelSource =
            CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();
        final Pair<String, String> authenticatedConsumerSource = ConsumersHelper.createEnrolledVerifiedConsumer(
            createConsumerModelSource, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenSource = authenticatedConsumerSource.getRight();
        final String consumerCurrency = createConsumerModelSource.getBaseCurrency();

        final CreateConsumerModel createConsumerModelDestination =
            CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp)
                .setBaseCurrency(consumerCurrency)
                .build();
        final Pair<String, String> authenticatedConsumerDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
            createConsumerModelDestination, secretKeyLowValueExemptionApp);
        final String consumerAuthenticationTokenDestination = authenticatedConsumerDestination.getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccountSource = createIdentityManagedAccount(
            consumerManagedAccountProfileIdLowValueExemptionApp,
            consumerCurrency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);
        final Pair<String, CreateManagedCardModel> consumerManagedCardDestination = createIdentityManagedCard(
            consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
            consumerCurrency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        fundManagedAccount(consumerManagedAccountSource.getLeft(), consumerCurrency, 10000L);

        SendsService.bulkSendFunds(SendModelHelper.createSendBulkPayments(2, sendsProfileIdLowValueExemptionApp, MANAGED_ACCOUNTS,
                    consumerManagedAccountSource.getLeft(), ManagedInstrumentType.MANAGED_CARDS, consumerManagedCardDestination.getLeft(), consumerCurrency, 30L),
                secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_OK);

        List<String> sends = SendsService.getSends(secretKeyLowValueExemptionApp, Optional.empty(), consumerAuthenticationTokenSource).jsonPath().getList("send.id");
        for (String send : sends) {SendsHelper.checkSendStateById(send, State.COMPLETED.name());}
    }

    @Test
    public void SendLowValueExemption_LowValueExemptionMultipleSendBulkExceededAmount_SuccessPending()  {

        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();

        final String currency = createConsumerModelSource.getBaseCurrency();

        final String consumerAuthenticationTokenSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp).getRight();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).setBaseCurrency(currency)
                        .build();

        final String consumerAuthenticationTokenDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp).getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccountSource = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                currency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);

        final Pair<String, CreateManagedCardModel> consumerManagedCardDestination = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                currency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        fundManagedAccount(consumerManagedAccountSource.getLeft(), currency, 10000L);

        final List<BulkSendResponseModel> bulkSend = SendsService
                .bulkSendFunds(SendModelHelper.createSendBulkPayments(5, sendsProfileIdLowValueExemptionApp, MANAGED_ACCOUNTS,
                                consumerManagedAccountSource.getLeft(), ManagedInstrumentType.MANAGED_CARDS, consumerManagedCardDestination.getLeft(), currency, 25L),
                        secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkSendsResponseModel.class).getResponse();

        bulkSend
                .forEach(send -> SendsHelper.ensureSendState(secretKeyLowValueExemptionApp, send.getId(), consumerAuthenticationTokenSource, State.PENDING));
    }

    @Test
    public void SendLowValueExemption_BeneficiaryAndLowValueExemptionMultipleSingleSendBulk_Success()  {

        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();

        final String currency = createConsumerModelSource.getBaseCurrency();

        final String consumerAuthenticationTokenSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp).getRight();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).setBaseCurrency(currency)
                        .build();

        final String consumerAuthenticationTokenDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp).getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccountSource = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                currency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);

        final Pair<String, CreateManagedCardModel> consumerManagedCardDestination = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                currency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        fundManagedAccount(consumerManagedAccountSource.getLeft(), currency, 10000L);

        final Map<String, State> sendIdStateMap = new HashMap<>();

        IntStream.range(0, 4).forEach(i -> {

            final String sendId =
                    SendsService
                            .bulkSendFunds(SendModelHelper.createSendBulkPayments(1, sendsProfileIdLowValueExemptionApp, MANAGED_ACCOUNTS,
                                            consumerManagedAccountSource.getLeft(), ManagedInstrumentType.MANAGED_CARDS, consumerManagedCardDestination.getLeft(), currency, 30L),
                                    secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .as(BulkSendsResponseModel.class).getResponse().stream().findFirst().orElseThrow().getId();

            sendIdStateMap.put(sendId, i < 3 ? State.COMPLETED : State.PENDING);
        });

        sendIdStateMap
                .forEach((key, value) ->
                        SendsHelper.ensureSendState(secretKeyLowValueExemptionApp, key, consumerAuthenticationTokenSource, value));
    }

    @Test
    public void SendLowValueExemption_LowValueExemptionMultipleSendBulkExceededAmount_LowValueSuccessfulAfterPending()  {

        final CreateConsumerModel createConsumerModelSource =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).build();

        final String currency = createConsumerModelSource.getBaseCurrency();

        final String consumerAuthenticationTokenSource = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelSource, secretKeyLowValueExemptionApp).getRight();

        final CreateConsumerModel createConsumerModelDestination =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileIdLowValueExemptionApp).setBaseCurrency(currency)
                        .build();

        final String consumerAuthenticationTokenDestination = ConsumersHelper.createEnrolledVerifiedConsumer(
                createConsumerModelDestination, secretKeyLowValueExemptionApp).getRight();

        final Pair<String, CreateManagedAccountModel> consumerManagedAccountSource = createIdentityManagedAccount(
                consumerManagedAccountProfileIdLowValueExemptionApp,
                currency, consumerAuthenticationTokenSource, secretKeyLowValueExemptionApp);

        final Pair<String, CreateManagedCardModel> consumerManagedCardDestination = createIdentityManagedCard(
                consumerPrepaidManagedCardsProfileIdLowValueExemptionApp,
                currency, consumerAuthenticationTokenDestination, secretKeyLowValueExemptionApp);

        fundManagedAccount(consumerManagedAccountSource.getLeft(), currency, 10000L);

        final List<BulkSendResponseModel> bulkSend = SendsService
                .bulkSendFunds(SendModelHelper.createSendBulkPayments(5, sendsProfileIdLowValueExemptionApp, MANAGED_ACCOUNTS,
                                consumerManagedAccountSource.getLeft(), ManagedInstrumentType.MANAGED_CARDS, consumerManagedCardDestination.getLeft(), currency, 25L),
                        secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkSendsResponseModel.class).getResponse();

        bulkSend
                .forEach(send -> SendsHelper.ensureSendState(secretKeyLowValueExemptionApp, send.getId(), consumerAuthenticationTokenSource, State.PENDING));

        final List<BulkSendResponseModel> bulkSendLowValueExemption = SendsService
                .bulkSendFunds(SendModelHelper.createSendBulkPayments(4, sendsProfileIdLowValueExemptionApp, MANAGED_ACCOUNTS,
                                consumerManagedAccountSource.getLeft(), ManagedInstrumentType.MANAGED_CARDS, consumerManagedCardDestination.getLeft(), currency, 25L),
                        secretKeyLowValueExemptionApp, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(BulkSendsResponseModel.class).getResponse();

        bulkSendLowValueExemption
                .forEach(send -> SendsHelper.ensureSendState(secretKeyLowValueExemptionApp, send.getId(), consumerAuthenticationTokenSource, State.COMPLETED));
    }

    private void startAuthyPushVerification(final String id,
                                            final String token) {

        SendsService.startSendPushVerification(id, CHANNEL_AUTHY, secretKeyLowValueExemptionApp, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static void sendsInLowValueLimits(final Long destinationAmount,
                                              final String token,
                                              final String identityManagedAccountId,
                                              final String identityCurrency,
                                              final String identityManagedCardId) {
        int count = 0;
        long leftSum = 100L;
        while (count < 5) {
            identityLowValueDepositAndSendMaToMc(token, identityManagedAccountId,
                    identityCurrency, identityManagedCardId, destinationAmount);

            count++;
            leftSum = leftSum - destinationAmount;
            if (leftSum < destinationAmount) {
                break;
            }
        }
    }

    private static Pair<String, CreateManagedAccountModel> createIdentityManagedAccount(
            final String managedAccountProfileId,
            final String identityCurrency,
            final String identityToken,
            final String secretKey) {
        return createManagedAccount(managedAccountProfileId, identityCurrency, secretKey,
                identityToken);
    }

    private static Pair<String, CreateManagedCardModel> createIdentityManagedCard(
            final String managedCardProfileId,
            final String identityCurrency,
            final String identityToken,
            final String secretKey) {
        return createPrepaidManagedCard(managedCardProfileId, identityCurrency, secretKey,
                identityToken);
    }

    private void startVerification(final String id,
                                   final String token) {

        SendsService.startSendOtpVerification(id, CHANNEL_OTP, secretKeyLowValueExemptionApp, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static void assertSendState(final String id, final String token, final State state) {
        TestHelper.ensureAsExpected(120,
                () -> SendsService.getSend(secretKeyLowValueExemptionApp, id, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
                Optional.of(String.format("Expecting 200 with a send in state %s, check logged payload", state.name())));
    }

    private static List<Pair<String, SendFundsModel>> identityLowValueDepositAndSendMaToMc(
            final String token,
            final String identityManagedAccountId,
            final String identityCurrency,
            final String identityManagedCardId,
            final Long amount) {
        final List<Pair<String, SendFundsModel>> identitySendFunds = new ArrayList<>();

        fundManagedAccount(identityManagedAccountId, identityCurrency, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdLowValueExemptionApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(identityCurrency, amount))
                        .setSource(new ManagedInstrumentTypeId(identityManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(identityManagedCardId, MANAGED_CARDS))
                        .build();

        final String id =
                SendsService.sendFunds(sendFundsModel, secretKeyLowValueExemptionApp, token, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        identitySendFunds.add(Pair.of(id, sendFundsModel));

        return identitySendFunds;
    }


}
