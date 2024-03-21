package opc.junit.multi.multipleapps;

import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class TransfersTests extends BaseApplicationsSetup {

    private static CreateConsumerModel createConsumerModel;
    private static String consumerProfileId;
    private static String managedAccountsProfileId;
    private static String transfersProfileId;
    private static String secretKey;
    private static String authenticationToken;

    @BeforeAll
    public static void TestSetup() {
        consumerProfileId = applicationTwo.getConsumersProfileId();
        managedAccountsProfileId = applicationTwo.getConsumerPayneticsEeaManagedAccountsProfileId();
        transfersProfileId = applicationTwo.getTransfersProfileId();
        secretKey = applicationTwo.getSecretKey();

        consumerSetup();
    }

    @Test
    public void TransferFunds_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();
        final Long feeAmount = TestHelper.getFees(currency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount();

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(managedAccountsProfileId, currency, authenticationToken, 2, secretKey);

        final int preTransferBalance = simulateManagedAccountDepositAndCheckBalance(managedAccounts.get(0).getLeft(),
                currency,
                depositAmount,
                secretKey,
                authenticationToken);

        assertSuccessfulTransfer(Pair.of(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS), currency, transferAmount, authenticationToken);

        final int sourceBalance = preTransferBalance - transferAmount.intValue() - feeAmount.intValue();

        assertManagedAccountBalance(managedAccounts.get(0).getLeft(), authenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.get(1).getLeft(), authenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_BusinessPurchasingProfile_Forbidden() {

        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(managedAccountsProfileId, currency, authenticationToken, 2, secretKey);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(applicationThree.getTransfersProfileId())
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void TransferFunds_BusinessPayoutsKey_Forbidden() {

        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(managedAccountsProfileId, currency, authenticationToken, 2, secretKey);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, applicationFour.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void TransferFunds_BusinessPurchasingKey_Forbidden() {

        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(managedAccountsProfileId, currency, authenticationToken, 2, secretKey);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, applicationThree.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void TransferFunds_TransferToOtherApplication_DestinationNotFound() {

        final String consumerAuthenticationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationFour.getConsumersProfileId(),
                        applicationFour.getSecretKey()).getRight();
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(applicationFour.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), Currency.getRandomCurrency().name())
                        .build();
        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, applicationFour.getSecretKey(), consumerAuthenticationToken);

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();

        final String managedAccountId =
                createManagedAccount(managedAccountsProfileId, currency, authenticationToken, secretKey).getLeft();

        final int preTransferBalance = simulateManagedAccountDepositAndCheckBalance(managedAccountId,
                currency,
                depositAmount,
                secretKey,
                authenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_NOT_FOUND"));

        assertManagedAccountBalance(managedAccountId, authenticationToken, preTransferBalance);
    }

    @Test
    public void GetTransfer_GetTransferFromOtherApplication_NotFound() {

        final String otherApplicationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationFour.getConsumersProfileId(),
                        applicationFour.getSecretKey()).getRight();

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(managedAccountsProfileId, currency, authenticationToken, 2, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccounts.get(0).getLeft(),
                currency,
                depositAmount,
                secretKey,
                authenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String transferId =
                TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty())
                        .then().statusCode(SC_OK).extract().jsonPath().get("id");

       TransfersService.getTransfer(applicationFour.getSecretKey(), transferId, otherApplicationToken)
               .then()
               .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTransfers_GetTransfersFromOtherApplication_DestinationNotFound() {

        final String otherApplicationToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationFour.getConsumersProfileId(),
                        applicationFour.getSecretKey()).getRight();

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String currency = createConsumerModel.getBaseCurrency();

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(managedAccountsProfileId, currency, authenticationToken, 2, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccounts.get(0).getLeft(),
                currency,
                depositAmount,
                secretKey,
                authenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty())
                .then().statusCode(SC_OK);

        TransfersService.getTransfers(applicationFour.getSecretKey(), Optional.empty(), otherApplicationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    private void assertSuccessfulTransfer(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                          final Pair<String, ManagedInstrumentType> destinationInstrument,
                                          final String currency,
                                          final Long transferAmount,
                                          final String token){
        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceInstrument.getLeft(), sourceInstrument.getRight()))
                        .setDestination(new ManagedInstrumentTypeId(destinationInstrument.getLeft(), destinationInstrument.getRight()))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
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
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final int balance){
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        authenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }
}
