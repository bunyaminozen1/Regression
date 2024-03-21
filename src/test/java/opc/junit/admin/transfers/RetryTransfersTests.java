package opc.junit.admin.transfers;

import io.restassured.response.Response;
import opc.enums.opc.CannedResponseType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.ResumeTransactionModel;
import opc.models.admin.RetryTransfersModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.simulator.SetCannedResponseModel;
import opc.services.admin.AdminService;
import opc.services.multi.TransfersService;
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
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

@Execution(ExecutionMode.SAME_THREAD)
public class RetryTransfersTests extends BaseTransferSetup {
    private static String corporateAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerAuthenticationToken;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void RetryTransfer_CorporateMaToMa_Success() {

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), corporateAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_CorporateMaToMc_Success() {

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 1);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), corporateAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_CorporateMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 1);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedCard(managedCard.getLeft(), corporateCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), corporateAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_CorporateMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedCard(managedCardSource.getLeft(), corporateCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), corporateAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_ConsumerMaToMa_Success() {

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfers_ConsumerMaToMa_Success() {

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty());

        Response response = TransfersService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken);

        String transferId = getLastTransfer(response);
        List<String> transferIds = new ArrayList<>();
        transferIds.add(transferId);

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty());

        response = TransfersService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken);

        transferId = getLastTransfer(response);
        transferIds.add(transferId);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfers(AdminService.loginAdmin(), new RetryTransfersModel(transferIds, RandomStringUtils.randomAlphabetic(5)))
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_ConsumerMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 1);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_ConsumerMcToMa_Success() {

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 1);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        fundManagedCard(managedCard.getLeft(), consumerCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void RetryTransfer_ConsumerMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        fundManagedCard(managedCardSource.getLeft(), consumerCurrency, depositAmount);

        setCannedResponse(CannedResponseType.UNKNOWN_SUCCESS);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty());

        final Response response = TransfersService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken);

        final String transferId = getLastTransfer(response);

        clearCannedResponse(innovatorId);

        AdminService.retryTransfer(AdminService.loginAdmin(), new ResumeTransactionModel(RandomStringUtils.randomAlphabetic(5)), transferId)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    private String getLastTransfer(final Response response) {
        List<String> transferIds = response.path("transfer.id");
        if (!transferIds.isEmpty()) {
            return transferIds.get(0);
        } else {
            return null; // or handle the case when no matching element is found
        }
    }

    private static void setCannedResponse(final CannedResponseType cannedResponse) {
        SimulatorService.setManagedCardCannedResponse(new SetCannedResponseModel(cannedResponse), innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);

        SimulatorService.setManagedAccountCannedResponse(new SetCannedResponseModel(cannedResponse), innovatorId)
                .then()
                .statusCode(SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.getManagedCardCannedResponse(innovatorId),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("type").equals(cannedResponse.name()),
                Optional.of(String.format("Expecting 200 with a caned response of type %s, check logged payload", cannedResponse.name())));
        TestHelper.ensureAsExpected(15,
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

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }
}
