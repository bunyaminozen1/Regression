package opc.junit.admin.managedcards;

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
import opc.models.admin.SpendRulesModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteManagedCardSpendRulesTests extends BaseManagedCardsSetup {

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
    public void DeleteManagedCardSpendRules_PrepaidCorporate_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void DeleteManagedCardSpendRules_DebitCorporate_Success(final LimitInterval interval){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void DeleteManagedCardSpendRules_PrepaidConsumerKycLevelV1_Success(final Currency currency){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, currency.name(), consumerV1AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, currency.name(), consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void DeleteManagedCardSpendRules_PrepaidConsumerKycLevelV2_Success(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_PrepaidConsumerKycLevelV1User_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumerV1AuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV1Currency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, consumerV1Currency, consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void DeleteManagedCardSpendRules_DebitConsumer_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_MultipleLimits_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_PrepaidCorporateUser_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_PrepaidPhysicalCard_Success(){

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_DebitPhysicalCard_Success(){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_UnknownManagedCardId_NotFound() {

        AdminService
                .deleteCardSpendRules(RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeleteManagedCardSpendRules_NoManagedCardId_NotFound() {

        AdminService
                .deleteCardSpendRules("", adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeleteManagedCardSpendRules_InvalidToken_Unauthorised(){

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, RandomStringUtils.randomAlphanumeric(7))
                .then()
                .statusCode(SC_UNAUTHORIZED);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporatePrepaidProfileRules);
    }

    @Test
    public void DeleteManagedCardSpendRules_DebitCorporateBlockedCard_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_NoRulesSet_NotFound(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeleteManagedCardSpendRules_RulesDeleted_NotFound(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                AdminService
                        .getCardSpendRules(adminToken, managedCardId)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);

        AdminService
                .deleteCardSpendRules(managedCardId, adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private void assertEmptySpendRules(final ValidatableResponse response,
                                       final String currency,
                                       final SpendRulesResponseModel spendRulesResponseModel,
                                       final CardMode cardMode,
                                       final Boolean isLevel1) {
        response
                .body("cardLevelSpendRules.allowedMerchantCategories", equalTo(new ArrayList<>()))
                .body("cardLevelSpendRules.blockedMerchantCategories", equalTo(new ArrayList<>()))
                .body("cardLevelSpendRules.allowedMerchantIds", equalTo(new ArrayList<>()))
                .body("cardLevelSpendRules.blockedMerchantIds", equalTo(new ArrayList<>()))
                .body("cardLevelSpendRules.allowContactless", equalTo("NULL"))
                .body("cardLevelSpendRules.allowAtm", equalTo("NULL"))
                .body("cardLevelSpendRules.allowECommerce", equalTo("NULL"))
                .body("cardLevelSpendRules.allowCashback", equalTo("NULL"))
                .body("cardLevelSpendRules.allowCreditAuthorisations", equalTo("NULL"))
                .body("cardLevelSpendRules.allowedMerchantCountries", equalTo(new ArrayList<>()))
                .body("cardLevelSpendRules.blockedMerchantCountries", equalTo(new ArrayList<>()))
                .body("cardLevelSpendRules.maxTransactionAmount", nullValue())
                .body("cardLevelSpendRules.minTransactionAmount", nullValue())
                .body("profileLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesResponseModel.getAllowedMerchantCategories()))
                .body("profileLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesResponseModel.getBlockedMerchantCategories()))
                .body("profileLevelSpendRules.allowedMerchantIds", equalTo(spendRulesResponseModel.getAllowedMerchantIds()))
                .body("profileLevelSpendRules.blockedMerchantIds", equalTo(spendRulesResponseModel.getBlockedMerchantIds()))
                .body("profileLevelSpendRules.allowContactless", equalTo(spendRulesResponseModel.isAllowContactless()))
                .body("profileLevelSpendRules.allowAtm", equalTo(spendRulesResponseModel.isAllowAtm()))
                .body("profileLevelSpendRules.allowECommerce", equalTo(spendRulesResponseModel.isAllowECommerce()))
                .body("profileLevelSpendRules.allowCashback", equalTo(spendRulesResponseModel.isAllowCashback()))
                .body("profileLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesResponseModel.isAllowCreditAuthorisations()))
                .body("profileLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesResponseModel.getAllowedMerchantCountries()))
                .body("profileLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesResponseModel.getBlockedMerchantCountries()))
                .body("profileLevelSpendRules.maxTransactionAmount", equalTo(spendRulesResponseModel.getMaxTransactionAmount() == null ?
                        null :  spendRulesResponseModel.getMaxTransactionAmount().intValue()))
                .body("profileLevelSpendRules.minTransactionAmount", equalTo(spendRulesResponseModel.getMinTransactionAmount() == null ?
                        null :  spendRulesResponseModel.getMinTransactionAmount().intValue()));

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

        if (cardMode.equals(CardMode.DEBIT)){
            response
                    .body("cardLevelSpendRules.spendLimit[0].limitAmount.currency", equalTo(currency))
                    .body("cardLevelSpendRules.spendLimit[0].limitAmount.amount", equalTo("0"))
                    .body("cardLevelSpendRules.spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        } else {
            response
                    .body("spendLimit", nullValue())
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        }
    }

    private void assertSpendRules(final ValidatableResponse response,
                                  final SpendRulesModel spendRulesModel,
                                  final SpendRulesResponseModel spendRulesResponseModel) {
        response
                .body("cardLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("cardLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("cardLevelSpendRules.allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("cardLevelSpendRules.blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("cardLevelSpendRules.allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("cardLevelSpendRules.allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("cardLevelSpendRules.allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("cardLevelSpendRules.allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("cardLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("cardLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("cardLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
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
                        null :  String.valueOf(spendRulesResponseModel.getMinTransactionAmount().intValue())))
                .body("identityLevelSpendRules.allowedMerchantCategories", equalTo(new ArrayList<>()))
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

        response
                .body("cardLevelSpendRules.spendLimit", equalTo(new ArrayList<>()))
                .body("profileLevelSpendRules.spendLimit", nullValue());
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
