package opc.junit.multi.multipleapps;

import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.ManagedInstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.SendsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class SendsTests extends BaseApplicationsSetup {

    private static CreateCorporateModel createCorporateModel;
    private static CreateConsumerModel createConsumerModel;
    private static String corporateProfileId;
    private static String consumerProfileId;
    private static String managedAccountsProfileId;
    private static String managedCardsProfileId;
    private static String sendsProfileId;
    private static String secretKey;
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;

    @BeforeAll
    public static void TestSetup() {
        corporateProfileId = applicationFour.getCorporatesProfileId();
        consumerProfileId = applicationFour.getConsumersProfileId();
        managedAccountsProfileId = applicationFour.getCorporatePayneticsEeaManagedAccountsProfileId();
        managedCardsProfileId = applicationFour.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        sendsProfileId = applicationFour.getSendProfileId();
        secretKey = applicationFour.getSecretKey();

        corporateSetup();
        consumerSetup();
    }

    @Test
    public void SendFunds_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Long feeAmount = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.MA_TO_MC_SEND_FEE).getAmount();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(managedAccountsProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationToken, secretKey);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(managedCardsProfileId, createConsumerModel.getBaseCurrency(), consumerAuthenticationToken, secretKey);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(),
                createCorporateModel.getBaseCurrency(),
                depositAmount,
                secretKey,
                corporateAuthenticationToken);

        assertSuccessfulSend(Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCard.getLeft(), MANAGED_CARDS), createCorporateModel.getBaseCurrency(), sendAmount, corporateAuthenticationToken);

        final int sourceBalance = preSendBalance - sendAmount.intValue() - feeAmount.intValue();

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCard.getLeft(), consumerAuthenticationToken, sendAmount.intValue());
    }

    @Test
    public void SendFunds_ConsumerPaymentsKey_Forbidden() {

        final Long sendAmount = 500L;
        final String currency = createCorporateModel.getBaseCurrency();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(managedAccountsProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationToken, secretKey);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(managedCardsProfileId, createConsumerModel.getBaseCurrency(), consumerAuthenticationToken, secretKey);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, applicationTwo.getSecretKey(), corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendFunds_BusinessPurchasingKey_Forbidden() {
        final Long sendAmount = 500L;
        final String currency = createCorporateModel.getBaseCurrency();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(managedAccountsProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationToken, secretKey);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(managedCardsProfileId, createConsumerModel.getBaseCurrency(), consumerAuthenticationToken, secretKey);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, applicationThree.getSecretKey(), corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendFunds_SendToOtherApplication_DestinationNotFound() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final String otherApplicationToken =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        applicationThree.getSecretKey()).getRight();
        final String otherApplicationManagedCardId =
                createPrepaidManagedCard(applicationThree.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), createCorporateModel.getBaseCurrency(), otherApplicationToken, applicationThree.getSecretKey()).getLeft();

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final String currency = createCorporateModel.getBaseCurrency();

        final String managedAccountId =
                createManagedAccount(managedAccountsProfileId, currency, corporateAuthenticationToken, secretKey).getLeft();

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccountId,
                currency,
                depositAmount,
                secretKey,
                corporateAuthenticationToken);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(otherApplicationManagedCardId, MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_NOT_FOUND"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, preSendBalance);
        assertManagedCardBalance(otherApplicationManagedCardId, otherApplicationToken, 0);
    }

    @Test
    public void GetSend_GetSendFromOtherApplication_NotFound() {

        final String otherApplicationToken =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(applicationFour.getCorporatesProfileId(),
                        applicationFour.getSecretKey()).getRight();

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final String currency = createCorporateModel.getBaseCurrency();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(managedAccountsProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationToken, secretKey);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(managedCardsProfileId, createConsumerModel.getBaseCurrency(), consumerAuthenticationToken, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(),
                createCorporateModel.getBaseCurrency(),
                depositAmount,
                secretKey,
                corporateAuthenticationToken);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then().statusCode(SC_OK).extract().jsonPath().get("id");

        SendsService.getSend(applicationFour.getSecretKey(), sendId, otherApplicationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetSends_GetSendsFromOtherApplication_DestinationNotFound() {

        final String otherApplicationToken =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(applicationFour.getCorporatesProfileId(),
                        applicationFour.getSecretKey()).getRight();

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final String currency = createCorporateModel.getBaseCurrency();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(managedAccountsProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationToken, secretKey);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(managedCardsProfileId, createConsumerModel.getBaseCurrency(), consumerAuthenticationToken, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(),
                createCorporateModel.getBaseCurrency(),
                depositAmount,
                secretKey,
                corporateAuthenticationToken);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCard.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then().statusCode(SC_OK);

        SendsService.getSends(applicationFour.getSecretKey(), Optional.empty(), otherApplicationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    private void assertSuccessfulSend(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                      final Pair<String, ManagedInstrumentType> destinationInstrument,
                                      final String currency,
                                      final Long sendAmount,
                                      final String token){
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceInstrument.getLeft(), sourceInstrument.getRight()))
                        .setDestination(new ManagedInstrumentTypeId(destinationInstrument.getLeft(), destinationInstrument.getRight()))
                        .build();


        SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(sendsProfileId))
                .body("tag", equalTo(sendFundsModel.getTag()))
                .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
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

    private void assertManagedCardBalance(final String managedCardId, final String token, final int balance){
        ManagedCardsService.getManagedCard(secretKey, managedCardId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private static void corporateSetup() {
        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }

    private static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }
}