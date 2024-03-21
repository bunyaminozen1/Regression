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
import opc.models.shared.SpendLimitModel;
import opc.models.innovator.SpendRulesModel;
import opc.models.admin.TransactionAmountModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddManagedCardSpendRulesTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerV1AuthenticationToken;
    private static String consumerV2AuthenticationToken;
    private static String corporateCurrency;
    private static String consumerV1Currency;
    private static String consumerV2Currency;
    private static SpendRulesResponseModel corporateDebitProfileRules;
    private static SpendRulesResponseModel corporatePrepaidProfileRules;
    private static SpendRulesResponseModel consumerDebitProfileRules;
    private static SpendRulesResponseModel consumerPrepaidProfileRules;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerV1Setup();
        consumerV2Setup();

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

    @Test
    public void PostManagedCardSpendRules_PrepaidCorporate_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidCorporateJustSpendRules_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
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
    public void PostManagedCardSpendRules_DebitCorporate_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
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

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PostManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV1_Success(final Currency currency){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, currency.name(), consumerV1AuthenticationToken)
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

        assertSpendRules(response, spendRulesModel, currency.name(), consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV2_Success(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
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

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidConsumer_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PostManagedCardSpendRules_DebitConsumer_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)))
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

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidConsumerKycLevelV1User_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumerV1AuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV1Currency, user.getRight())
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

        assertSpendRules(response, spendRulesModel, consumerV1Currency, consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void PostManagedCardSpendRules_AddRulesAfterDelete_Success(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        InnovatorService
                .deleteCardSpendRules(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

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
    public void PostManagedCardSpendRules_MultipleLimits_Success(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
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
    public void PostManagedCardSpendRules_PrepaidCorporateUserJustSpendRules_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .setAllowAtm(null)
                .setAllowECommerce(null)
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

    @Test
    public void PostManagedCardSpendRules_PrepaidCorporateUser_SpendLimitNotSupportedForPrepaidMode(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidPhysicalCardJustSpendRules_Success(){

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

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

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidPhysicalCard_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PostManagedCardSpendRules_DebitPhysicalCard_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)))
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

        assertSpendRules(response, spendRulesModel, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PostManagedCardSpendRules_CorporatePostUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
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
    public void PostManagedCardSpendRules_DifferentCurrency_SpendLimitCurrencyDifferentFromCardCurrency(){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();
        final String spendLimitCurrency =
                Currency.getRandomWithExcludedCurrency(Currency.valueOf(consumerV2Currency)).toString();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(spendLimitCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void PostManagedCardSpendRules_DuplicateIntervals_SpendLimitDuplicateInterval(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10L), LimitInterval.DAILY)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_DUPLICATE_INTERVAL"));
    }

    @Test
    public void PostManagedCardSpendRules_InvalidLimitAmount_SpendLimitAmountInvalid(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, -100L), LimitInterval.DAILY)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_AMOUNT_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "UNKNOWN" })
    public void PostManagedCardSpendRules_InvalidInterval_BadRequest(final String interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setInterval(interval);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(spendLimitModel))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ABCD" })
    public void PostManagedCardSpendRules_InvalidCurrency_BadRequest(final String currency){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(currency, 100L), LimitInterval.DAILY);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(spendLimitModel))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostManagedCardSpendRules_UnknownCurrency_SpendLimitCurrencyDifferentFromCardCurrency(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount("ABC", 100L), LimitInterval.DAILY);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(spendLimitModel))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void PostManagedCardSpendRules_NullLimitValue_BadRequest(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setLimitAmount(null);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(spendLimitModel))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostManagedCardSpendRules_NullLimitInterval_BadRequest(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setInterval(null);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(spendLimitModel))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, RandomStringUtils.randomNumeric(18), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PostManagedCardSpendRules_NoManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, "", innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PostManagedCardSpendRules_InvalidToken_Unauthorised(){

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostManagedCardSpendRules_DebitCorporateBlockedCard_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
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
    public void PostManagedCardSpendRules_LimitAmountZero_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(0L).setHasValue(true).build())
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(0L).setHasValue(true).build())
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
    public void PostManagedCardSpendRules_InvalidMaximumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(-1L).setHasValue(true).build())
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.maxTransactionAmount: must be greater than or equal to 0"));
    }

    @Test
    public void PostManagedCardSpendRules_InvalidMinimumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(-1L).setHasValue(true).build())
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.minTransactionAmount: must be greater than or equal to 0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void PostManagedCardSpendRules_InvalidAllowedCountry_BadRequest(final String country){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setAllowedMerchantCountries(Collections.singletonList(country))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void PostManagedCardSpendRules_InvalidBlockedCountry_BadRequest(final String country){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setBlockedMerchantCountries(Collections.singletonList(country))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("singleFieldUpdate")
    public void PostManagedCardSpendRules_SingleFieldUpdate_Success(final SpendRulesModel spendRulesModel){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

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
    public void PostManagedCardSpendRules_AlreadyCreated_SpendRuleAlreadySet(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        InnovatorService
                .addCardSpendRules(spendRulesModel, managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        InnovatorService
                .addCardSpendRules(SpendRulesModel.DefaultSpendRulesModel().setAllowedMerchantCountries(Collections.singletonList("ES")).build(),
                        managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_RULE_ALREADY_SET"));

        final ValidatableResponse response =
                InnovatorService
                        .getCardSpendRules(innovatorToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
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

    private static Stream<Arguments> singleFieldUpdate() {
        return Stream.of(
                Arguments.of(SpendRulesModel.builder().setAllowedMerchantCategories(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.builder().setAllowedMerchantIds(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.builder().setBlockedMerchantIds(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.builder().setAllowedMerchantCountries(Collections.singletonList("MT")).build()),
                Arguments.of(SpendRulesModel.builder().setBlockedMerchantCountries(Collections.singletonList("IT")).build()),
                Arguments.of(SpendRulesModel.builder().setAllowAtm("TRUE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowCashback("TRUE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowContactless("TRUE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowCreditAuthorisations("TRUE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowECommerce("TRUE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowAtm("FALSE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowCashback("FALSE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowContactless("FALSE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowCreditAuthorisations("FALSE").build()),
                Arguments.of(SpendRulesModel.builder().setAllowECommerce("FALSE").build()),
                Arguments.of(SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS))).build()));
    }

    private static void consumerV1Setup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);
        consumerV1AuthenticationToken = authenticatedConsumer.getRight();
        consumerV1Currency = createConsumerModel.getBaseCurrency();
    }

    private static void consumerV2Setup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerV2AuthenticationToken = authenticatedConsumer.getRight();
        consumerV2Currency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}
