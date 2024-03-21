package opc.junit.admin.sends;

import io.restassured.response.Response;
import opc.enums.opc.CannedResponseType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.ResumeSendsModel;
import opc.models.admin.ResumeTransactionModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.simulator.SetCannedResponseModel;
import opc.services.admin.AdminService;
import opc.services.multi.SendsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static opc.junit.helpers.TestHelper.ensureAsExpected;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

@Execution(ExecutionMode.SAME_THREAD)
public class RetrySendsTests extends BaseSendsSetup {
    private static String consumerAuthenticationTokenSource;
    private static String consumerCurrencySource;
    private static String corporateAuthenticationTokenSource;
    private static String corporateCurrencySource;
    private static String consumerAuthenticationTokenDestination;
    private static String corporateAuthenticationTokenDestination;

    @BeforeAll
    public static void Setup() {
        corporateSetupSource();
        consumerSetupSource();

        corporateSetupDestination();
        consumerSetupDestination();
    }

    @Test
    public void ResumeSend_CorporateMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 100L;
        final Pair<String, CreateManagedAccountModel> manageAccountSource = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> manageAccountDestination = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedAccount(manageAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                corporateAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ResumeSends_CorporateMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 100L;
        final Pair<String, CreateManagedAccountModel> manageAccountSource = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> manageAccountDestination = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedAccount(manageAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                corporateAuthenticationTokenSource, Optional.empty());

        Response response = SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource);

        String sendId = getLastSend(response);

        List<String> sends = new ArrayList<>();
        sends.add(sendId);

        SendsService.sendFunds(sendFundsModel, secretKey,
                corporateAuthenticationTokenSource, Optional.empty());

        response = SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource);

        sendId = getLastSend(response);
        sends.add(sendId);

        clearCannedResponse(innovatorId);

        AdminService.retrySends(AdminService.loginAdmin(), new ResumeSendsModel(sends, RandomStringUtils.randomAlphabetic(5)))
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ResumeSend_CorporateMaToMc_Success() {

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedAccountModel> manageAccountSource = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedAccount(manageAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                corporateAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void ResumeSend_CorporateMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> manageAccountDestination = createManagedAccount(corporateManagedAccountsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        fundManagedCard(manageCardSource.getLeft(), corporateCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                corporateAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void ResumeSend_CorporateMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(manageCardSource.getLeft(), corporateCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(manageCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                corporateAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void ResumeSend_ConsumerMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 100L;
        final Pair<String, CreateManagedAccountModel> manageAccountSource = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> manageAccountDestination = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedAccount(manageAccountSource.getLeft(), consumerCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerAuthenticationTokenSource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                consumerAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), consumerAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ResumeSend_ConsumerMaToMc_Success() {

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedAccountModel> manageAccountSource = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedAccount(manageAccountSource.getLeft(), consumerCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                consumerAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), consumerAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void ResumeSend_ConsumerMcToMa_Success() {

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> manageAccountDestination = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);

        fundManagedCard(manageCardSource.getLeft(), consumerCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                consumerAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), consumerAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void ResumeSend_ConsumerMcToMc_Success() {

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedCard(manageCardSource.getLeft(), consumerCurrencySource, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(manageCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(manageCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey,
                consumerAuthenticationTokenSource, Optional.empty());


        final Response response = SendsService.getSends(secretKey, Optional.empty(), consumerAuthenticationTokenSource);

        final String sendId = getLastSend(response);

        clearCannedResponse(innovatorId);


        AdminService.retrySend(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), sendId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }


    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerIdSource = authenticatedConsumer.getLeft();
        consumerAuthenticationTokenSource = authenticatedConsumer.getRight();
        consumerCurrencySource = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerIdSource);
    }

    private static void corporateSetupSource() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateIdSource = authenticatedCorporate.getLeft();
        final String corporateNameSource = createCorporateModel.getCompany().getName();
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrencySource = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateIdSource);
    }

    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerIdDestination = authenticatedConsumer.getLeft();
        final String consumerNameDestination = String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname());
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerIdDestination);
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateIdDestination = authenticatedCorporate.getLeft();
        final String corporateNameDestination = createCorporateModel.getCompany().getName();
        corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateIdDestination);
    }

    private static void setCannedResponse(final CannedResponseType cannedResponse) {
        SimulatorService.setManagedCardCannedResponse(new SetCannedResponseModel(cannedResponse), innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);
        SimulatorService.setManagedAccountCannedResponse(new SetCannedResponseModel(cannedResponse), innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ensureAsExpected(30,
                () -> SimulatorService.getManagedCardCannedResponse(innovatorId),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("type").equals(cannedResponse.name()),
                Optional.of(String.format("Expecting 200 with a caned response of type %s, check logged payload", cannedResponse.name())));

        ensureAsExpected(30,
                () -> SimulatorService.getManagedAccountCannedResponse(innovatorId),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("type").equals(cannedResponse.name()),
                Optional.of(String.format("Expecting 200 with a caned response of type %s, check logged payload", cannedResponse.name())));
    }

    private static void clearCannedResponse(final String innovatorId) {
        SimulatorService.clearManagedCardCannedResponse(innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);
        SimulatorService.clearManagedAccountCannedResponse(innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String getLastSend(final Response response) {
        List<String> sendIds = response.path("send.id");
        if (!sendIds.isEmpty()) {
            return sendIds.get(0);
        } else {
            return null; // or handle the case when no matching element is found
        }
    }
}
