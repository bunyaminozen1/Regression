package opc.junit.multi.transfers;

import commons.enums.State;
import io.restassured.response.Response;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.TransfersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.admin.SetLimitModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static opc.enums.opc.ManagedInstrumentType.UNKNOWN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CreateTransfersTests extends BaseTransfersSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static String innovatorId;

    @BeforeAll
    public static void Setup() {

        innovatorId = applicationOne.getInnovatorId();

        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void TransferFunds_CorporateMaToMa_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), currency.name(), depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), currency.name(), sendAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(currency.name()).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), corporateAuthenticationToken, sendAmount.intValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void TransferFunds_CorporateMaToMaUnderNonFpsEnabledTenant_Success(final Currency currency) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsTenant.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsTenant.getSecretKey());

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), currency.name(),
                        nonFpsTenant.getSecretKey(), corporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), currency.name(), depositAmount, nonFpsTenant.getInnovatorId());

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS), Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS),
                nonFpsTenant.getTransfersProfileId(), currency.name(), sendAmount, nonFpsTenant.getSecretKey(), corporate.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(currency.name()).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), nonFpsTenant.getSecretKey(), corporate.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), nonFpsTenant.getSecretKey(), corporate.getRight(), sendAmount.intValue());
    }

    @Test
    public void TransferFunds_ConsumerMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), consumerCurrency, transferAmount, consumerAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(consumerCurrency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), consumerAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), consumerAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateUserMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, user.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), corporateCurrency, transferAmount, user.getRight());

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), user.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), user.getRight(), transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        fundManagedCard(managedCards.get(0).getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCards.get(0).getLeft(), MANAGED_CARDS),
                Pair.of(managedCards.get(1).getLeft(), MANAGED_CARDS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MC_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCards.get(0).getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCards.get(1).getLeft(), corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_ConsumerMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedCard(managedCards.get(0).getLeft(), consumerCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCards.get(0).getLeft(), MANAGED_CARDS),
                Pair.of(managedCards.get(1).getLeft(), MANAGED_CARDS), consumerCurrency, transferAmount, consumerAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(consumerCurrency).get(FeeType.MC_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCards.get(0).getLeft(), consumerAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCards.get(1).getLeft(), consumerAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateUserMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight(), 2);

        fundManagedCard(managedCards.get(0).getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCards.get(0).getLeft(), MANAGED_CARDS),
                Pair.of(managedCards.get(1).getLeft(), MANAGED_CARDS), corporateCurrency, transferAmount, user.getRight());

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MC_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCards.get(0).getLeft(), user.getRight(), sourceBalance);
        assertManagedCardBalance(managedCards.get(1).getLeft(), user.getRight(), transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedCard(managedCard.getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCard.getLeft(), MANAGED_CARDS),
                Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MC_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCard.getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_ConsumerMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        fundManagedCard(managedCard.getLeft(), consumerCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCard.getLeft(), MANAGED_CARDS),
                Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS), consumerCurrency, transferAmount, consumerAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(consumerCurrency).get(FeeType.MC_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCard.getLeft(), consumerAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateUserMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, user.getRight());

        fundManagedCard(managedCard.getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCard.getLeft(), MANAGED_CARDS),
                Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS), corporateCurrency, transferAmount, user.getRight());

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MC_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCard.getLeft(), user.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccount.getLeft(), user.getRight(), transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCard.getLeft(), MANAGED_CARDS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCard.getLeft(), corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_ConsumerMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), consumerCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCard.getLeft(), MANAGED_CARDS), consumerCurrency, transferAmount, consumerAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(consumerCurrency).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCard.getLeft(), consumerAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateUserMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, user.getRight());
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCard.getLeft(), MANAGED_CARDS), corporateCurrency, transferAmount, user.getRight());

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccount.getLeft(), user.getRight(), sourceBalance);
        assertManagedCardBalance(managedCard.getLeft(), user.getRight(), transferAmount.intValue());
    }

    @Test
    public void TransferFunds_SourceManagedAccountLinkedToDebitCard_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);
        createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccounts.get(0).getLeft(), corporateAuthenticationToken);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_DestinationManagedAccountLinkedToDebitCard_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);
        createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccounts.get(1).getLeft(), corporateAuthenticationToken);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_RequiredFieldsOnly_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrency, depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(transfersProfileId))
                .body("tag", equalTo(transferFundsModel.getTag()))
                .body("source.type", equalTo(transferFundsModel.getSource().getType()))
                .body("source.id", equalTo(transferFundsModel.getSource().getId()))
                .body("destination.type", equalTo(transferFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(transferFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(transferFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(transferFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), corporateAuthenticationToken, sendAmount.intValue());
    }

    @Test
    public void TransferFunds_SameIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(TransfersService.transferFunds(transferFundsModel, secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(TransfersService.transferFunds(transferFundsModel, secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response ->

                response.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(transfersProfileId))
                        .body("tag", equalTo(transferFundsModel.getTag()))
                        .body("source.type", equalTo(transferFundsModel.getSource().getType()))
                        .body("source.id", equalTo(transferFundsModel.getSource().getId()))
                        .body("destination.type", equalTo(transferFundsModel.getDestination().getType()))
                        .body("destination.id", equalTo(transferFundsModel.getDestination().getId()))
                        .body("destinationAmount.currency", equalTo(transferFundsModel.getDestinationAmount().getCurrency()))
                        .body("destinationAmount.amount", equalTo(transferFundsModel.getDestinationAmount().getAmount().intValue()))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                TransfersService.getTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    @DisplayName("TransferFunds_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void TransferFunds_SameIdempotencyRefDifferentPayload_PreconditionFailed() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final TransferFundsModel transferFundsModel1 =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        TransfersService.transferFunds(transferFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        TransfersService.transferFunds(transferFundsModel1,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void TransferFunds_DifferentIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(TransfersService.transferFunds(transferFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(TransfersService.transferFunds(transferFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)));
        responses.add(TransfersService.transferFunds(transferFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.empty()));

        responses.forEach(response ->

                response.then()
                        .statusCode(SC_OK));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                TransfersService.getTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void TransferFunds_DifferentIdempotencyRefDifferentPayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();
        final TransferFundsModel transferFundsModel1 =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final Map<Response, TransferFundsModel> responses = new HashMap<>();

        responses.put(TransfersService.transferFunds(transferFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                transferFundsModel);
        responses.put(TransfersService.transferFunds(transferFundsModel1,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)),
                transferFundsModel1);
        responses.put(TransfersService.transferFunds(transferFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.empty()),
                transferFundsModel);

        responses.forEach((key, value) ->

                key.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(transfersProfileId))
                        .body("tag", equalTo(value.getTag()))
                        .body("source.type", equalTo(value.getSource().getType()))
                        .body("source.id", equalTo(value.getSource().getId()))
                        .body("destination.type", equalTo(value.getDestination().getType()))
                        .body("destination.id", equalTo(value.getDestination().getId()))
                        .body("destinationAmount.currency", equalTo(value.getDestinationAmount().getCurrency()))
                        .body("destinationAmount.amount", equalTo(value.getDestinationAmount().getAmount().intValue()))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertNotEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertNotEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responseList.get(1).jsonPath().getString("id"), responseList.get(2).jsonPath().getString("id"));
        assertNotEquals(responseList.get(1).jsonPath().getString("creationTimestamp"), responseList.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                TransfersService.getTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void TransferFunds_LongIdempotencyRef_RequestTooLong() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);

        TransfersService.transferFunds(transferFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_REQUEST_TOO_LONG);
    }

    @Test
    public void TransferFunds_ExpiredIdempotencyRef_NewRequestSuccess() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(TransfersService.transferFunds(transferFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        TimeUnit.SECONDS.sleep(18);

        responses.add(TransfersService.transferFunds(transferFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response -> response.then()
                    .statusCode(SC_OK)
                    .body("id", notNullValue())
                    .body("profileId", equalTo(transfersProfileId))
                    .body("tag", equalTo(transferFundsModel.getTag()))
                    .body("source.type", equalTo(transferFundsModel.getSource().getType()))
                    .body("source.id", equalTo(transferFundsModel.getSource().getId()))
                    .body("destination.type", equalTo(transferFundsModel.getDestination().getType()))
                    .body("destination.id", equalTo(transferFundsModel.getDestination().getId()))
                    .body("destinationAmount.currency", equalTo(transferFundsModel.getDestinationAmount().getCurrency()))
                    .body("destinationAmount.amount", equalTo(transferFundsModel.getDestinationAmount().getAmount().intValue()))
                    .body("state", equalTo("COMPLETED"))
                    .body("creationTimestamp", notNullValue()));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("2",
                TransfersService.getTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void TransferFunds_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel.Builder transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        TransfersService.transferFunds(transferFundsModel.setProfileId("123").build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_NOT_FOUND);

        TransfersService.transferFunds(transferFundsModel.setProfileId(transfersProfileId).build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        assertEquals("1",
                TransfersService.getTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void TransferFunds_SameIdempotencyRefSamePayloadWithChange_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final Map<Response, State> responses = new HashMap<>();

        final Response initialResponse =
                TransfersService.transferFunds(transferFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference));

        responses.put(initialResponse, State.SCHEDULED);

        TransfersHelper.cancelScheduledTransfer(secretKey, initialResponse.jsonPath().getString("id"), authenticatedCorporate.getRight());

        responses.put(TransfersService.transferFunds(transferFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                State.CANCELLED);

        responses.forEach((key, value) ->

                key.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(transfersProfileId))
                        .body("tag", equalTo(transferFundsModel.getTag()))
                        .body("source.type", equalTo(transferFundsModel.getSource().getType()))
                        .body("source.id", equalTo(transferFundsModel.getSource().getId()))
                        .body("destination.type", equalTo(transferFundsModel.getDestination().getType()))
                        .body("destination.id", equalTo(transferFundsModel.getDestination().getId()))
                        .body("destinationAmount.currency", equalTo(transferFundsModel.getDestinationAmount().getCurrency()))
                        .body("destinationAmount.amount", equalTo(transferFundsModel.getDestinationAmount().getAmount().intValue()))
                        .body("state", equalTo(value.name()))
                        .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                TransfersService.getTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void TransferFunds_ManagedAccountStatement_Success() {
        final Long depositAmount = 10000L;
        final Long depositFeeAmount = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long sendAmount = 500L;
        final Long sendFeeAmount = TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MA_SEND_FEE).getAmount();
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccounts.get(0).getLeft(),
                corporateCurrency,
                depositAmount,
                secretKey,
                corporateAuthenticationToken);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), corporateCurrency, sendAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (preSendBalance - sendAmount -
                        sendFeeAmount);

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), corporateAuthenticationToken, sendAmount.intValue());

        final Response sourceManagedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedAccounts.get(0).getLeft(), secretKey, corporateAuthenticationToken, 2);

        final Response destinationManagedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedAccounts.get(1).getLeft(), secretKey, corporateAuthenticationToken, 1);

        sourceManagedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(sendFeeAmount.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.destinationInstrumentType", equalTo(MANAGED_ACCOUNTS.getValue()))
                .body("entry[0].additionalFields.destinationInstrumentId", equalTo(managedAccounts.get(1).getLeft()))
                .body("entry[0].additionalFields.destinationInstrumentFriendlyName", equalTo(managedAccounts.get(1).getRight().getFriendlyName()))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(depositFeeAmount.intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        destinationManagedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].actualBalanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.sourceInstrumentType", equalTo(MANAGED_ACCOUNTS.getValue()))
                .body("entry[0].additionalFields.sourceInstrumentId", equalTo(managedAccounts.get(0).getLeft()))
                .body("entry[0].additionalFields.sourceInstrumentFriendlyName", equalTo(managedAccounts.get(0).getRight().getFriendlyName()))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void TransferFunds_ManagedCardStatement_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Long sendFeeAmount = TestHelper.getFees(consumerCurrency).get(FeeType.MC_TO_MC_SEND_FEE).getAmount();
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        final int preSendBalance = simulateManagedCardDepositAndCheckBalance(consumerManagedAccountProfileId,
                managedCards.get(0).getLeft(),
                consumerCurrency,
                depositAmount,
                secretKey,
                consumerAuthenticationToken);

        assertSuccessfulTransfer(Pair.of(managedCards.get(0).getLeft(), MANAGED_CARDS),
                Pair.of(managedCards.get(1).getLeft(), MANAGED_CARDS), consumerCurrency, sendAmount, consumerAuthenticationToken);

        final int sourceBalance =
                (int) (preSendBalance - sendAmount -
                        sendFeeAmount);

        assertManagedCardBalance(managedCards.get(0).getLeft(), consumerAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCards.get(1).getLeft(), consumerAuthenticationToken, sendAmount.intValue());

        final Response sourceManagedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCards.get(0).getLeft(), secretKey, consumerAuthenticationToken, 2);

        final Response destinationCardAccountStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCards.get(1).getLeft(), secretKey, consumerAuthenticationToken, 1);

        sourceManagedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(sendFeeAmount.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.destinationInstrumentType", equalTo(MANAGED_CARDS.getValue()))
                .body("entry[0].additionalFields.destinationInstrumentId", equalTo(managedCards.get(1).getLeft()))
                .body("entry[0].additionalFields.destinationInstrumentFriendlyName", equalTo(managedCards.get(1).getRight().getFriendlyName()))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(preSendBalance))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(preSendBalance))
                .body("entry[1].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        destinationCardAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].actualBalanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.sourceInstrumentType", equalTo(MANAGED_CARDS.getValue()))
                .body("entry[0].additionalFields.sourceInstrumentId", equalTo(managedCards.get(0).getLeft()))
                .body("entry[0].additionalFields.sourceInstrumentFriendlyName", equalTo(managedCards.get(0).getRight().getFriendlyName()))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void TransferFunds_SendToNonActiveAccount_InstrumentDeniedTransaction() {

        final Currency currency = Currency.GBP;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), 10000L);

        final Pair<String, CreateManagedAccountModel> pendingApprovalManagedAccount =
                createPendingApprovalManagedAccount(corporateManagedAccountProfileId, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(pendingApprovalManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DENIED_TRANSACTION"));
    }

    @Test
    public void TransferFunds_DebitManagedCardToManagedAccount_SourceCannotBeDebitModeCard() {
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedCardModel> managedCard =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void TransferFunds_ManagedAccountToDebitManagedCard_DestinationCannotBeDebitModeCard() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedCardModel> managedCard =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void TransferFunds_ManagedCardToDebitManagedCard_DestinationCannotBeDebitModeCard() {
        final Pair<String, CreateManagedCardModel> prepaidManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedCardModel> debitManagedCard =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(prepaidManagedCard.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(debitManagedCard.getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void TransferFunds_TransferToSameManagedAccountLinkedToDebitManagedCard_DestinationCannotBeDebitModeCard() {
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedCardModel> managedCard =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccountForFunding.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void TransferFunds_UnknownProfileId_NotFound() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(RandomStringUtils.randomNumeric(18))
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void TransferFunds_UnknownSourceInstrumentId_SourceNotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void TransferFunds_UnknownDestinationInstrumentId_DestinationNotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void TransferFunds_NoProfileId_Conflict(final String profileId) {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(profileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void TransferFunds_NoSourceInstrumentId_BadRequest(final String instrumentId) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(instrumentId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void TransferFunds_NoDestinationInstrumentId_BadRequest(final String instrumentId) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(instrumentId, MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void TransferFunds_UnknownSourceInstrumentType_BadRequest() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), UNKNOWN))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void TransferFunds_UnknownDestinationInstrumentType_BadRequest() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), UNKNOWN))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void TransferFunds_CrossIdentityAuthenticationCheck_SourceNotFound() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void TransferFunds_CrossIdentityTransferCheck_DestinationNotFound() {
        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationToken);

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumerAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(consumerManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_NOT_FOUND"));
    }

    @Test
    public void TransferFunds_NoFunds_FundsInsufficient() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void TransferFunds_NotEnoughFunds_FundsInsufficient() {
        final Long depositAmount = 400L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void TransferFunds_DestinationManagedAccountLimitReachedAndNotEnoughFundsNoFundMovements_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount1 =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount2 =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount1.getLeft(),
                Currency.EUR.name(),
                3000000L,
                secretKey, corporate.getRight());

        final TransferFundsModel.Builder transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 1000000L))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount1.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(corporateManagedAccount2.getLeft(), MANAGED_ACCOUNTS));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        transferFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 700000L));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> expectedSourceManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> expectedDestinationManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount2.getLeft()).get(0);

        transferFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 2000000L));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> actualSourceManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> actualDestinationManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount2.getLeft()).get(0);


        assertEquals(expectedSourceManagedAccountBalances.get("available"), actualSourceManagedAccountBalances.get("available"));
        assertEquals(expectedSourceManagedAccountBalances.get("pending"), actualSourceManagedAccountBalances.get("pending"));
        assertEquals(expectedDestinationManagedAccountBalances.get("available"), actualDestinationManagedAccountBalances.get("available"));
        assertEquals(expectedDestinationManagedAccountBalances.get("pending"), actualDestinationManagedAccountBalances.get("pending"));
    }

    @Test
    public void TransferFunds_DestinationPrepaidCardLimitReachedAndNotEnoughFundsNoFundMovements_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount1 =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedCardModel> corporateManagedAccount2 =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, Currency.EUR.name(), corporate.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount1.getLeft(),
                Currency.EUR.name(),
                3000000L,
                secretKey, corporate.getRight());

        final TransferFundsModel.Builder transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 1000000L))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount1.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(corporateManagedAccount2.getLeft(), MANAGED_CARDS));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        transferFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 700000L));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> expectedSourceAccountBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> expectedDestinationCardBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(corporateManagedAccount2.getLeft()).get(0);

        transferFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 2000000L));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> actualSourceAccountBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> actualDestinationCardBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(corporateManagedAccount2.getLeft()).get(0);

        assertEquals(expectedSourceAccountBalance.get("available"), actualSourceAccountBalance.get("available"));
        assertEquals(expectedSourceAccountBalance.get("pending"), actualSourceAccountBalance.get("pending"));
        assertEquals(expectedDestinationCardBalance.get("available"), actualDestinationCardBalance.get("available"));
        assertEquals(expectedDestinationCardBalance.get("pending"), actualDestinationCardBalance.get("pending"));
    }

    @Test
    public void TransferFunds_ManagedAccountFundsInAvailableBalanceNotPending_Success() throws SQLException {

        final Long depositAmount = 2000000L;
        final Long depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long transferFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount();
        final Long exceedLimitSendAmount = 3000000L;
        final Long validSendAmount = 1000L;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount1 =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount2 =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount1.getLeft(),
                Currency.EUR.name(),
                depositAmount,
                secretKey, corporate.getRight());

        final TransferFundsModel.Builder transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), exceedLimitSendAmount))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount1.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(corporateManagedAccount2.getLeft(), MANAGED_ACCOUNTS));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> preTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> preTransferDestinationBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount2.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee), preTransferSourceBalance.get("available"));
        assertEquals("0", preTransferSourceBalance.get("pending"));

        assertEquals("0", preTransferDestinationBalance.get("available"));
        assertEquals("0", preTransferDestinationBalance.get("pending"));

        transferFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), validSendAmount));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> postTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> postTransferDestinationBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount2.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee - validSendAmount - transferFee), postTransferSourceBalance.get("available"));
        assertEquals("0", postTransferSourceBalance.get("pending"));

        assertEquals(String.valueOf(validSendAmount), postTransferDestinationBalance.get("available"));
        assertEquals("0", postTransferDestinationBalance.get("pending"));
    }

    @Test
    public void TransferFunds_PrepaidManagedCardFundsInAvailableBalanceNotPending_Success() throws SQLException {

        final Long depositAmount = 2000000L;
        final Long depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long transferFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount();
        final Long exceedLimitSendAmount = 3000000L;
        final Long validSendAmount = 1000L;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount1 =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedCardModel> corporateManagedAccount2 =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, Currency.EUR.name(), corporate.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount1.getLeft(),
                Currency.EUR.name(),
                depositAmount,
                secretKey, corporate.getRight());

        final TransferFundsModel.Builder transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), exceedLimitSendAmount))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount1.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(corporateManagedAccount2.getLeft(), MANAGED_CARDS));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> preTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> preTransferDestinationBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(corporateManagedAccount2.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee), preTransferSourceBalance.get("available"));
        assertEquals("0", preTransferSourceBalance.get("pending"));

        assertEquals("0", preTransferDestinationBalance.get("available"));
        assertEquals("0", preTransferDestinationBalance.get("pending"));

        transferFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), validSendAmount));

        TransfersService.transferFunds(transferFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> postTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount1.getLeft()).get(0);

        final Map<String, String> postTransferDestinationBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(corporateManagedAccount2.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee - validSendAmount - transferFee), postTransferSourceBalance.get("available"));
        assertEquals("0", postTransferSourceBalance.get("pending"));

        assertEquals(String.valueOf(validSendAmount), postTransferDestinationBalance.get("available"));
        assertEquals("0", postTransferDestinationBalance.get("pending"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -300L})
    public void TransferFunds_NegativeAmount_AmountInvalid(final long sendAmount) {
        final Long depositAmount = 10000L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        fundManagedAccount(managedAccounts.get(1).getLeft(), consumerCurrency, depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), consumerAuthenticationToken, depositAmount.intValue());
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), consumerAuthenticationToken, depositAmount.intValue());
    }

    @Test
    public void TransferFunds_TransferToSelf_Conflict() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), consumerCurrency, depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_AND_DESTINATION_MUST_BE_DIFFERENT"));
    }

    @Test
    public void TransferFunds_OtherCurrency_CurrencyMismatch() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), consumerCurrency, depositAmount);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency
                                .getRandomWithExcludedCurrency(Currency.valueOf(consumerCurrency)).name(), transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    public void TransferFunds_InvalidCurrency_BadRequest() {

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount("ABCD", 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void TransferFunds_SourceManagedCardBlocked_SourceInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        fundManagedCard(managedCards.get(0).getLeft(), corporateCurrency, depositAmount);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCards.get(0).getLeft(), corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCards.get(0).getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCards.get(1).getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void TransferFunds_DestinationManagedCardRemoved_DestinationInstrumentDestroyed() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        fundManagedCard(managedCards.get(0).getLeft(), corporateCurrency, depositAmount);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCards.get(1).getLeft(), corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCards.get(0).getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCards.get(1).getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void TransferFunds_DestinationManagedCardBlocked_DestinationInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        fundManagedCard(managedCards.get(0).getLeft(), corporateCurrency, depositAmount);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCards.get(1).getLeft(), corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCards.get(0).getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCards.get(1).getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void TransferFunds_SourceManagedAccountBlocked_SourceInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrency, depositAmount);

        ManagedAccountsHelper.blockManagedAccount(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void TransferFunds_DestinationManagedAccountRemoved_DestinationInstrumentDestroyed() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedCard(managedCard.getLeft(), corporateCurrency, depositAmount);

        ManagedAccountsHelper.removeManagedAccount(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void TransferFunds_DestinationManagedAccountBlocked_DestinationInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        fundManagedCard(managedCard.getLeft(), corporateCurrency, depositAmount);

        ManagedAccountsHelper.blockManagedAccount(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void TransferFunds_InvalidApiKey_Unauthorised() {
        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, "abc", consumerAuthenticationToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void TransferFunds_NoApiKey_BadRequest() {
        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, "", consumerAuthenticationToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void TransferFunds_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void TransferFunds_RootUserLoggedOut_Unauthorised() {

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void TransferFunds_BackofficeCorporateImpersonator_Forbidden() {

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void TransferFunds_BackofficeConsumerImpersonator_Forbidden() {

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrency, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }


    private void assertSuccessfulTransfer(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                          final Pair<String, ManagedInstrumentType> destinationInstrument,
                                          final String currency,
                                          final Long transferAmount,
                                          final String token) {
        assertSuccessfulTransfer(sourceInstrument, destinationInstrument, transfersProfileId, currency, transferAmount, secretKey, token);
    }

    private void assertSuccessfulTransfer(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                          final Pair<String, ManagedInstrumentType> destinationInstrument,
                                          final String profile,
                                          final String currency,
                                          final Long transferAmount,
                                          final String secretKey,
                                          final String token) {
        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(profile)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceInstrument.getLeft(), sourceInstrument.getRight()))
                        .setDestination(new ManagedInstrumentTypeId(destinationInstrument.getLeft(), destinationInstrument.getRight()))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(profile))
                .body("tag", equalTo(transferFundsModel.getTag()))
                .body("source.type", equalTo(transferFundsModel.getSource().getType()))
                .body("source.id", equalTo(transferFundsModel.getSource().getId()))
                .body("destination.type", equalTo(transferFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(transferFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(transferFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(transferFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("description", notNullValue())
                .body("creationTimestamp", notNullValue());
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final int balance) {
        assertManagedAccountBalance(managedAccountId, secretKey, token, balance);
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String secretKey, final String token, final int balance) {
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private void assertManagedCardBalance(final String managedCardId, final String token, final int balance) {
        ManagedCardsService.getManagedCard(secretKey, managedCardId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }


    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private void setCorporateLimits(final String corporateId) {

        final String adminToken = AdminService.loginAdmin();
        final String token = AdminService.impersonateTenant(innovatorId, adminToken);

        final SetLimitModel corporateVelocityLimits = SetLimitModel.builder()
                .setLimitType("CORPORATE_VELOCITY_AGGREGATE")
                .setBaseLimit(new CurrencyAmount(Currency.EUR.name(), 500000000L))
                .setCurrencyLimit(Arrays.asList(new CurrencyAmount(Currency.USD.name(), 10000000L),
                        new CurrencyAmount(Currency.GBP.name(), 10000000L)))
                .build();

        AdminService
                .setCorporateLimit(corporateVelocityLimits, token, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SetLimitModel corporateFundsLimit = SetLimitModel.builder()
                .setLimitType("CORPORATE_FUNDS_SOURCE_AGGREGATE")
                .setBaseLimit(new CurrencyAmount(Currency.EUR.name(), 500000000L))
                .setCurrencyLimit(Arrays.asList(new CurrencyAmount(Currency.USD.name(), 10000000L),
                        new CurrencyAmount(Currency.GBP.name(), 10000000L)))
                .build();

        AdminService
                .setCorporateLimit(corporateFundsLimit, token, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

}
