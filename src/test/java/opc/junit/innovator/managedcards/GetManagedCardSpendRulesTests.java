package opc.junit.innovator.managedcards;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardMode;
import commons.enums.Currency;
import opc.enums.opc.KycLevel;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.SpendRulesModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.SpendLimitModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetManagedCardSpendRulesTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerV1AuthenticationToken;
    private static String consumerV2AuthenticationToken;
    private static String corporateCurrency;
    private static String consumerV1Currency;
    private static String consumerV2Currency;
    private static CreateCorporateModel corporateDetails;
    private static CreateConsumerModel consumerV2Details;
    private static ManagedCardDetails corporatePrepaidManagedCard;
    private static ManagedCardDetails consumerV2PrepaidManagedCard;
    private static SpendRulesResponseModel corporateDebitProfileRules;
    private static SpendRulesResponseModel corporatePrepaidProfileRules;
    private static SpendRulesResponseModel consumerDebitProfileRules;
    private static SpendRulesResponseModel consumerPrepaidProfileRules;

    @BeforeAll
    public static void Setup() throws InterruptedException {

        corporateSetup();
        consumerV1Setup();
        consumerV2Setup();

        corporatePrepaidManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateDetails.getBaseCurrency(), corporateAuthenticationToken);
        consumerV2PrepaidManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Details.getBaseCurrency(), consumerV2AuthenticationToken);

        corporateDebitProfileRules =
                InnovatorHelper.getProfileSpendRules(innovatorToken, programmeId, corporateDebitManagedCardsProfileId)
                        .as(SpendRulesResponseModel.class);

        corporatePrepaidProfileRules =
                InnovatorHelper.getProfileSpendRules(innovatorToken, programmeId, corporatePrepaidManagedCardsProfileId)
                        .as(SpendRulesResponseModel.class);

        consumerDebitProfileRules =
                InnovatorHelper.getProfileSpendRules(innovatorToken, programmeId, consumerDebitManagedCardsProfileId)
                        .as(SpendRulesResponseModel.class);

        consumerPrepaidProfileRules =
                InnovatorHelper.getProfileSpendRules(innovatorToken, programmeId, consumerPrepaidManagedCardsProfileId)
                        .as(SpendRulesResponseModel.class);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void GetManagedCardSpendRules_DebitCorporate_Success(final LimitInterval interval){
        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel,  managedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void GetManagedCardSpendRules_PrepaidCorporate_Success(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, corporatePrepaidManagedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, corporatePrepaidManagedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void GetManagedCardSpendRules_DebitConsumerV2_Success(final LimitInterval interval){

        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                consumerV2Currency, consumerV2AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel,  managedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedCardSpendRules_PrepaidConsumerV1_Success(final Currency currency) {

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                currency.name(), consumerV1AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel,  managedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, currency.name(), consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void GetManagedCardSpendRules_PrepaidConsumerV2_Success(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, consumerV2PrepaidManagedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, consumerV2PrepaidManagedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void GetManagedCardSpendRules_DebitCorporateUser_Success(final LimitInterval interval){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void GetManagedCardSpendRules_PrepaidCorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void GetManagedCardSpendRules_DebitPhysicalCard_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, corporateAuthenticationToken);

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void GetManagedCardSpendRules_PrepaidPhysicalCard_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, corporateAuthenticationToken);

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void GetManagedCardSpendRules_MultipleLimits_Success(){

        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                consumerV2Currency, consumerV2AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.DAILY)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel,  managedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void GetManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        InnovatorService.getCardSpendRules(innovatorToken, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private void assertSpendRules(final ValidatableResponse response,
                                  final SpendRulesModel spendRulesModel,
                                  final String currency,
                                  final SpendRulesResponseModel spendRulesResponseModel,
                                  final CardMode cardMode,
                                  final Boolean isLevel1) {
        response
                .body("cardLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories() == null
                        ? new ArrayList<>() : spendRulesModel.getAllowedMerchantCategories()))
                .body("cardLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories() == null
                        ? new ArrayList<>() : spendRulesModel.getBlockedMerchantCategories()))
                .body("cardLevelSpendRules.allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds() == null
                        ? new ArrayList<>() : spendRulesModel.getAllowedMerchantIds()))
                .body("cardLevelSpendRules.blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds() == null
                        ? new ArrayList<>() : spendRulesModel.getBlockedMerchantIds()))
                .body("cardLevelSpendRules.allowContactless", equalTo(spendRulesModel.getAllowContactless() == null ? "NULL" : spendRulesModel.getAllowContactless()))
                .body("cardLevelSpendRules.allowAtm", equalTo(spendRulesModel.getAllowAtm() == null ? "NULL" : spendRulesModel.getAllowAtm()))
                .body("cardLevelSpendRules.allowECommerce", equalTo(spendRulesModel.getAllowECommerce() == null ? "NULL" : spendRulesModel.getAllowECommerce()))
                .body("cardLevelSpendRules.allowCashback", equalTo(spendRulesModel.getAllowCashback() == null ? "NULL" : spendRulesModel.getAllowCashback()))
                .body("cardLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations() == null ? "NULL" : spendRulesModel.getAllowCreditAuthorisations()))
                .body("cardLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries() == null
                        ? new ArrayList<>() : spendRulesModel.getAllowedMerchantCountries()))
                .body("cardLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries() == null
                        ? new ArrayList<>() : spendRulesModel.getBlockedMerchantCountries()))
                .body("cardLevelSpendRules.maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount() == null ?
                        null :  String.valueOf(spendRulesModel.getMaxTransactionAmount().getValue().intValue())))
                .body("cardLevelSpendRules.minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount() == null ?
                        null :  String.valueOf(spendRulesModel.getMinTransactionAmount().getValue().intValue())))
                .body("profileLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesResponseModel.getAllowedMerchantCategories()
                        .size() == 0 ? new ArrayList<>() : spendRulesResponseModel.getAllowedMerchantCategories()))
                .body("profileLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesResponseModel.getBlockedMerchantCategories()
                        .size() == 0 ? new ArrayList<>() : spendRulesResponseModel.getBlockedMerchantCategories()))
                .body("profileLevelSpendRules.allowedMerchantIds", equalTo(spendRulesResponseModel.getAllowedMerchantIds()
                        .size() == 0 ? new ArrayList<>() : spendRulesResponseModel.getAllowedMerchantIds()))
                .body("profileLevelSpendRules.blockedMerchantIds", equalTo(spendRulesResponseModel.getBlockedMerchantIds()
                        .size() == 0 ? new ArrayList<>() : spendRulesResponseModel.getBlockedMerchantIds()))
                .body("profileLevelSpendRules.allowContactless", equalTo(spendRulesResponseModel.isAllowContactless()))
                .body("profileLevelSpendRules.allowAtm", equalTo(spendRulesResponseModel.isAllowAtm()))
                .body("profileLevelSpendRules.allowECommerce", equalTo(spendRulesResponseModel.isAllowECommerce()))
                .body("profileLevelSpendRules.allowCashback", equalTo(spendRulesResponseModel.isAllowCashback()))
                .body("profileLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesResponseModel.isAllowCreditAuthorisations()))
                .body("profileLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesResponseModel.getAllowedMerchantCountries()
                        .size() == 0 ? new ArrayList<>() : spendRulesResponseModel.getAllowedMerchantCountries()))
                .body("profileLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesResponseModel.getBlockedMerchantCountries()
                        .size() == 0 ? new ArrayList<>() : spendRulesResponseModel.getBlockedMerchantCountries()))
                .body("profileLevelSpendRules.maxTransactionAmount.value", equalTo(spendRulesResponseModel.getMaxTransactionAmount() == null ?
                        null :  String.valueOf(spendRulesResponseModel.getMaxTransactionAmount().intValue())))
                .body("profileLevelSpendRules.minTransactionAmount.value", equalTo(spendRulesResponseModel.getMinTransactionAmount() == null ?
                        null :  String.valueOf(spendRulesResponseModel.getMinTransactionAmount().intValue())));

        if (cardMode.equals(CardMode.PREPAID)){
            response
                    .body("cardLevelSpendRules.spendLimit", equalTo(new ArrayList<>()))
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        } else {

            if(spendRulesModel.getSpendLimit() != null){
                IntStream.range(0, spendRulesModel.getSpendLimit().size()).forEach(i ->
                        response
                                .body(String.format("cardLevelSpendRules.spendLimit[%s].limitAmount.currency", i), equalTo(spendRulesModel.getSpendLimit().get(i).getLimitAmount().getCurrency()))
                                .body(String.format("cardLevelSpendRules.spendLimit[%s].limitAmount.amount", i), equalTo(spendRulesModel.getSpendLimit().get(i).getLimitAmount().getAmount().toString()))
                                .body(String.format("cardLevelSpendRules.spendLimit[%s].interval", i), equalTo(spendRulesModel.getSpendLimit().get(i).getInterval()))
                                .body("profileLevelSpendRules.spendLimit", nullValue()));
            } else {
                response
                        .body("cardLevelSpendRules.spendLimit[0].limitAmount.currency", equalTo(currency))
                        .body("cardLevelSpendRules.spendLimit[0].limitAmount.amount", equalTo("0"))
                        .body("cardLevelSpendRules.spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                        .body("profileLevelSpendRules.spendLimit", nullValue());
            }
        }

        if (isLevel1){

            final int maxTransactionAmount;
            switch (currency) {
                case "EUR": maxTransactionAmount = 5000;
                    break;
                case "GBP": maxTransactionAmount = 4200;
                    break;
                case "USD": maxTransactionAmount = 5600;
                    break;
                default: throw new IllegalArgumentException("Currency not supported.");

            }

            response
                    .body("identityLevelSpendRules.allowedMerchantCategories", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.allowedMerchantIds", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.blockedMerchantIds", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.allowContactless", equalTo("TRUE"))
                    .body("identityLevelSpendRules.allowAtm", equalTo("FALSE"))
                    .body("identityLevelSpendRules.allowECommerce", equalTo("TRUE"))
                    .body("identityLevelSpendRules.allowCashback", equalTo("TRUE"))
                    .body("identityLevelSpendRules.allowCreditAuthorisations", equalTo("TRUE"))
                    .body("identityLevelSpendRules.allowedMerchantCountries", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.blockedMerchantCountries", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.maxTransactionAmount.value", equalTo(String.valueOf(maxTransactionAmount)))
                    .body("identityLevelSpendRules.maxTransactionAmount.hasValue", equalTo(true))
                    .body("identityLevelSpendRules.minTransactionAmount", nullValue());

            assertEquals(response.extract().jsonPath().getString("identityLevelSpendRules.blockedMerchantCategories"),
                    Arrays.asList(4829, 6010, 6011, 6012, 6051, 6538, 6540, 7995).toString());
        } else {
            response.body("identityLevelSpendRules.allowedMerchantCategories", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.blockedMerchantCategories", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.allowedMerchantIds", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.blockedMerchantIds", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.allowContactless", equalTo("NULL"))
                    .body("identityLevelSpendRules.allowAtm", equalTo("NULL"))
                    .body("identityLevelSpendRules.allowECommerce", equalTo("NULL"))
                    .body("identityLevelSpendRules.allowCashback", equalTo("NULL"))
                    .body("identityLevelSpendRules.allowCreditAuthorisations", equalTo("NULL"))
                    .body("identityLevelSpendRules.allowedMerchantCountries", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.blockedMerchantCountries", equalTo(new ArrayList<>()))
                    .body("identityLevelSpendRules.maxTransactionAmount", nullValue())
                    .body("identityLevelSpendRules.minTransactionAmount", nullValue());
        }
    }

    private static void consumerV1Setup() {
        final CreateConsumerModel consumerV1Details =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerV1Details, KycLevel.KYC_LEVEL_1, secretKey);
        consumerV1AuthenticationToken = authenticatedConsumer.getRight();
        consumerV1Currency = consumerV1Details.getBaseCurrency();
    }

    private static void consumerV2Setup() {
        consumerV2Details =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerV2Details, secretKey);
        consumerV2AuthenticationToken = authenticatedConsumer.getRight();
        consumerV2Currency = consumerV2Details.getBaseCurrency();
    }

    private static void corporateSetup() {

        corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateDetails, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();
    }
}
