package opc.junit.backoffice.managedcards;

import commons.enums.Currency;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.backoffice.SpendLimitModel;
import opc.models.backoffice.SpendRulesModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostManagedCardSpendRulesTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerV1AuthenticationToken;
    private static String consumerV2AuthenticationToken;
    private static String corporateCurrency;
    private static String consumerV1Currency;
    private static String consumerV2Currency;
    private static String corporateId;
    private static String consumerV1Id;
    private static String consumerV2Id;
    private static SpendRulesResponseModel corporateDebitProfileRules;
    private static SpendRulesResponseModel corporatePrepaidProfileRules;
    private static SpendRulesResponseModel consumerDebitProfileRules;
    private static SpendRulesResponseModel consumerPrepaidProfileRules;
    private static String corporateImpersonateToken;
    private static String consumerV1ImpersonateToken;
    private static String consumerV2ImpersonateToken;

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

        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        consumerV1ImpersonateToken = BackofficeHelper.impersonateIdentity(consumerV1Id, IdentityType.CONSUMER, secretKey);
        consumerV2ImpersonateToken = BackofficeHelper.impersonateIdentity(consumerV2Id, IdentityType.CONSUMER, secretKey);
    }

    @Test
    public void PostManagedCardSpendRules_PrepaidCorporate_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
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
        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[1].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[2].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit"))
                .body("validationErrors[2].fieldName", equalTo("interval"));
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("RANGE"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit.value.currency"));
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
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
                        .setValue(null);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(spendLimitModel))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit.value"));
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit.interval"));
    }

    @Test
    public void PostManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("PostManagedCardSpendRules_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void PostManagedCardSpendRules_NoManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, "", corporateImpersonateToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void PostManagedCardSpendRules_CrossIdentityImpersonation_Unauthorised() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostManagedCardSpendRules_OtherCorporateIdentityImpersonation_Unauthorised() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String newCorporateId = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey).getLeft();

        final String token = BackofficeHelper.impersonateIdentity(newCorporateId, IdentityType.CORPORATE, secretKey);

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostManagedCardSpendRules_OtherConsumerIdentityImpersonation_Unauthorised() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String newConsumerId = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey).getLeft();

        final String token = BackofficeHelper.impersonateIdentity(newConsumerId, IdentityType.CONSUMER, secretKey);

        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostManagedCardSpendRules_InvalidToken_Unauthorised(){

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostManagedCardSpendRules_InvalidApiKey_Unauthorised(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, "abc", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostManagedCardSpendRules_NoApiKey_BadRequest(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, "", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void PostManagedCardSpendRules_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PostManagedCardSpendRules_CorporateToken_Forbidden(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PostManagedCardSpendRules_ConsumerToken_Forbidden(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), consumerV2AuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PostManagedCardSpendRules_DebitCorporate_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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
                .setMaxTransactionAmount(0L)
                .setMinTransactionAmount(0L)
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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
                .setMaxTransactionAmount(-1L)
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].message", equalTo("maxTransactionAmount must be greater than or equal to 0"))
                .body("validationErrors[0].error", equalTo("AT_LEAST"))
                .body("validationErrors[0].fieldName", equalTo("maxTransactionAmount"));
    }

    @Test
    public void PostManagedCardSpendRules_InvalidMinimumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMinTransactionAmount(-1L)
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].message", equalTo("minTransactionAmount must be greater than or equal to 0"))
                .body("validationErrors[0].error", equalTo("AT_LEAST"))
                .body("validationErrors[0].fieldName", equalTo("minTransactionAmount"));
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_REQUEST"));
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST).body("validationErrors[0].error", equalTo("INVALID_REQUEST"));
    }

    @ParameterizedTest
    @MethodSource("singleFieldUpdate")
    public void PostManagedCardSpendRules_SingleFieldUpdate_Success(final SpendRulesModel spendRulesModel){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .postManagedCardsSpendRules(SpendRulesModel.DefaultSpendRulesModel().setAllowedMerchantCountries(Collections.singletonList("ES")).build(),
                        secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_RULE_ALREADY_SET"));

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PostManagedCardSpendRules_DebitCorporate_AllowedNullsAsItem_Success(){
        final String managedCardId =
            createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
            .build();

        final SpendRulesModel spendRulesModelWithNulls = SpendRulesModel.nullableSpendRulesModel()
            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
            .build();

        BackofficeMultiService
            .postManagedCardsSpendRules(spendRulesModelWithNulls, secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
            BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PostManagedCardSpendRules_OnlyNullAsItems_AllowedNullsAsItem_Success(){
        final String managedCardId =
            createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
            .setBlockedMerchantCategories(Collections.singletonList(null))
            .setBlockedMerchantIds(Collections.singletonList(null))
            .setAllowedMerchantCategories(Collections.singletonList(null))
            .setAllowedMerchantIds(Collections.singletonList(null))
            .setBlockedMerchantCountries(Collections.singletonList(null))
            .setAllowedMerchantCountries(Collections.singletonList(null))
            .build();

        BackofficeMultiService
            .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
            .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_OK)
            .body("$", not(hasItem("allowedMerchantCategories")))
            .body("$", not(hasItem("blockedMerchantCategories")))
            .body("$", not(hasItem("allowedMerchantIds")))
            .body("$", not(hasItem("blockedMerchantIds")))
            .body("$", not(hasItem("allowedMerchantCountries")))
            .body("$", not(hasItem("blockedMerchantCountries")));
    }

    private void assertSpendRules(final ValidatableResponse response,
                                  final SpendRulesModel spendRulesModel,
                                  final String currency,
                                  final SpendRulesResponseModel spendRulesResponseModel,
                                  final CardMode cardMode,
                                  final Boolean isLevel1) {
        response
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount", equalTo(spendRulesModel.getMaxTransactionAmount() == null ?
                        null :  spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount() == null ?
                        null :  spendRulesModel.getMinTransactionAmount().intValue()))
                .body("cardLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("cardLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("cardLevelSpendRules.allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("cardLevelSpendRules.blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("cardLevelSpendRules.allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                .body("cardLevelSpendRules.allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                .body("cardLevelSpendRules.allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                .body("cardLevelSpendRules.allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                .body("cardLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()))
                .body("cardLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("cardLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("cardLevelSpendRules.maxTransactionAmount", equalTo(spendRulesModel.getMaxTransactionAmount() == null ?
                        null :  spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("cardLevelSpendRules.minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount() == null ?
                        null :  spendRulesModel.getMinTransactionAmount().intValue()))
                .body("profileLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesResponseModel.getAllowedMerchantCategories()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantCategories()))
                .body("profileLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesResponseModel.getBlockedMerchantCategories()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantCategories()))
                .body("profileLevelSpendRules.allowedMerchantIds", equalTo(spendRulesResponseModel.getAllowedMerchantIds()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantIds()))
                .body("profileLevelSpendRules.blockedMerchantIds", equalTo(spendRulesResponseModel.getBlockedMerchantIds()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantIds()))
                .body("profileLevelSpendRules.allowContactless", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowContactless())))
                .body("profileLevelSpendRules.allowAtm", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowAtm())))
                .body("profileLevelSpendRules.allowECommerce", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowECommerce())))
                .body("profileLevelSpendRules.allowCashback", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowCashback())))
                .body("profileLevelSpendRules.allowCreditAuthorisations", equalTo(Boolean.valueOf(spendRulesResponseModel.isAllowCreditAuthorisations())))
                .body("profileLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesResponseModel.getAllowedMerchantCountries()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantCountries()))
                .body("profileLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesResponseModel.getBlockedMerchantCountries()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantCountries()))
                .body("profileLevelSpendRules.maxTransactionAmount", equalTo(spendRulesResponseModel.getMaxTransactionAmount() == null ?
                        null :  spendRulesResponseModel.getMaxTransactionAmount().intValue()))
                .body("profileLevelSpendRules.minTransactionAmount", equalTo(spendRulesResponseModel.getMinTransactionAmount() == null ?
                        null :  spendRulesResponseModel.getMinTransactionAmount().intValue()));

        if (cardMode.equals(CardMode.PREPAID)){
            response
                    .body("spendLimit", nullValue())
                    .body("cardLevelSpendRules.spendLimit", nullValue())
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        } else {

            if(spendRulesModel.getSpendLimit() != null){
                IntStream.range(0, spendRulesModel.getSpendLimit().size()).forEach(i ->
                        response
                                .body(String.format("spendLimit[%s].value.currency", i), equalTo(spendRulesModel.getSpendLimit().get(i).getValue().getCurrency()))
                                .body(String.format("spendLimit[%s].value.amount", i), equalTo(spendRulesModel.getSpendLimit().get(i).getValue().getAmount().intValue()))
                                .body(String.format("spendLimit[%s].interval", i), equalTo(spendRulesModel.getSpendLimit().get(i).getInterval()))
                                .body(String.format("cardLevelSpendRules.spendLimit[%s].value.currency", i), equalTo(spendRulesModel.getSpendLimit().get(i).getValue().getCurrency()))
                                .body(String.format("cardLevelSpendRules.spendLimit[%s].value.amount", i), equalTo(spendRulesModel.getSpendLimit().get(i).getValue().getAmount().intValue()))
                                .body(String.format("cardLevelSpendRules.spendLimit[%s].interval", i), equalTo(spendRulesModel.getSpendLimit().get(i).getInterval()))
                                .body("profileLevelSpendRules.spendLimit", nullValue()));
            } else {
                response
                        .body("spendLimit[0].value.currency", equalTo(currency))
                        .body("spendLimit[0].value.amount", equalTo(0))
                        .body("spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                        .body("cardLevelSpendRules.spendLimit[0].value.currency", equalTo(currency))
                        .body("cardLevelSpendRules.spendLimit[0].value.amount", equalTo(0))
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
                    .body("identityLevelSpendRules.allowedMerchantCategories", nullValue())
                    .body("identityLevelSpendRules.allowedMerchantIds", nullValue())
                    .body("identityLevelSpendRules.blockedMerchantIds", nullValue())
                    .body("identityLevelSpendRules.allowContactless", equalTo(true))
                    .body("identityLevelSpendRules.allowAtm", equalTo(false))
                    .body("identityLevelSpendRules.allowECommerce", equalTo(true))
                    .body("identityLevelSpendRules.allowCashback", equalTo(true))
                    .body("identityLevelSpendRules.allowCreditAuthorisations", equalTo(true))
                    .body("identityLevelSpendRules.allowedMerchantCountries", nullValue())
                    .body("identityLevelSpendRules.blockedMerchantCountries", nullValue())
                    .body("identityLevelSpendRules.maxTransactionAmount", equalTo(maxTransactionAmount))
                    .body("identityLevelSpendRules.minTransactionAmount", nullValue());

            assertEquals(response.extract().jsonPath().getString("identityLevelSpendRules.blockedMerchantCategories"),
                    Arrays.asList(4829, 6010, 6011, 6012, 6051, 6538, 6540, 7995).toString());
        } else {
            assertEquals("{}", response.extract().jsonPath().get("identityLevelSpendRules").toString());
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
                Arguments.of(SpendRulesModel.builder().setAllowAtm(true).build()),
                Arguments.of(SpendRulesModel.builder().setAllowCashback(true).build()),
                Arguments.of(SpendRulesModel.builder().setAllowContactless(true).build()),
                Arguments.of(SpendRulesModel.builder().setAllowCreditAuthorisations(true).build()),
                Arguments.of(SpendRulesModel.builder().setAllowECommerce(true).build()),
                Arguments.of(SpendRulesModel.builder().setAllowAtm(false).build()),
                Arguments.of(SpendRulesModel.builder().setAllowCashback(false).build()),
                Arguments.of(SpendRulesModel.builder().setAllowContactless(false).build()),
                Arguments.of(SpendRulesModel.builder().setAllowCreditAuthorisations(false).build()),
                Arguments.of(SpendRulesModel.builder().setAllowECommerce(false).build()),
                Arguments.of(SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS))).build()));
    }

    private static void consumerV1Setup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);
        consumerV1Id = authenticatedConsumer.getLeft();
        consumerV1AuthenticationToken = authenticatedConsumer.getRight();
        consumerV1Currency = createConsumerModel.getBaseCurrency();
    }

    private static void consumerV2Setup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerV2Id = authenticatedConsumer.getLeft();
        consumerV2AuthenticationToken = authenticatedConsumer.getRight();
        consumerV2Currency = createConsumerModel.getBaseCurrency();
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
}
