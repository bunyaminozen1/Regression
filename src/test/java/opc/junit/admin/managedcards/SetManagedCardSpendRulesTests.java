package opc.junit.admin.managedcards;

import commons.enums.Currency;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.shared.SpendLimitModel;
import opc.models.admin.SpendRulesModel;
import opc.models.admin.TransactionAmountModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class SetManagedCardSpendRulesTests extends BaseManagedCardsSetup {
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidCorporate_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidCorporateJustSpendRules_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .setAllowAtm(null)
                .setAllowECommerce(null)
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless() == null ? "NULL" : spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm() == null ? "NULL" : spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce() == null ? "NULL" : spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback() == null ? "NULL" : spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations() == null ? "NULL" : spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit", equalTo(new ArrayList<>()));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void SetManagedCardSpendRules_DebitCorporate_Success(final LimitInterval interval){
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
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit[0].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getCurrency()))
                .body("spendLimit[0].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getAmount().toString()))
                .body("spendLimit[0].interval", equalTo(spendRulesModel.getSpendLimit().get(0).getInterval()));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidConsumerJustSpendRules_Success(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit", equalTo(new ArrayList<>()));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidConsumer_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void SetManagedCardSpendRules_DebitConsumer_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), interval)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit[0].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getCurrency()))
                .body("spendLimit[0].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getAmount().toString()))
                .body("spendLimit[0].interval", equalTo(spendRulesModel.getSpendLimit().get(0).getInterval()));
    }

    @Test
    public void SetManagedCardSpendRules_MultipleLimits_Success(){
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
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit[0].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getCurrency()))
                .body("spendLimit[0].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getAmount().toString()))
                .body("spendLimit[0].interval", equalTo(spendRulesModel.getSpendLimit().get(0).getInterval()))
                .body("spendLimit[1].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(1).getLimitAmount().getCurrency()))
                .body("spendLimit[1].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(1).getLimitAmount().getAmount().toString()))
                .body("spendLimit[1].interval", equalTo(spendRulesModel.getSpendLimit().get(1).getInterval()));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidCorporateUserJustSpendRules_Success(){

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
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit", equalTo(new ArrayList<>()));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidCorporateUser_SpendLimitNotSupportedForPrepaidMode(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidPhysicalCardJustSpendRules_Success(){

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit", equalTo(new ArrayList<>()));
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidPhysicalCard_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void SetManagedCardSpendRules_DebitPhysicalCard_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), interval)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", equalTo(spendRulesModel.getMaxTransactionAmount().getValue().toString()))
                .body("minTransactionAmount.value", equalTo(spendRulesModel.getMinTransactionAmount().getValue().toString()))
                .body("spendLimit[0].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getCurrency()))
                .body("spendLimit[0].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getAmount().toString()))
                .body("spendLimit[0].interval", equalTo(spendRulesModel.getSpendLimit().get(0).getInterval()));
    }

    @Test
    public void SetManagedCardSpendRules_DifferentCurrency_SpendLimitCurrencyDifferentFromCardCurrency(){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();
        final String spendLimitCurrency =
                Currency.getRandomWithExcludedCurrency(Currency.valueOf(consumerCurrency)).toString();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(spendLimitCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void SetManagedCardSpendRules_DuplicateIntervals_SpendLimitDuplicateInterval(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10L), LimitInterval.DAILY)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_DUPLICATE_INTERVAL"));
    }

    @Test
    public void SetManagedCardSpendRules_InvalidLimitAmount_SpendLimitAmountInvalid(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, -100L), LimitInterval.DAILY)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_AMOUNT_INVALID"));
    }

    @Test
    public void SetManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetManagedCardSpendRules_PrepaidRequiredOnly_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless("TRUE")
                .setAllowAtm("FALSE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("FALSE")
                .setAllowCreditAuthorisations("TRUE")
                .setSpendLimit(new ArrayList<>())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(new ArrayList<>()))
                .body("blockedMerchantCountries", equalTo(new ArrayList<>()))
                .body("maxTransactionAmount.value", nullValue())
                .body("minTransactionAmount.value", nullValue())
                .body("spendLimit", equalTo(new ArrayList<>()));
    }

    @Test
    public void SetManagedCardSpendRules_DebitRequiredOnly_Success(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless("TRUE")
                .setAllowAtm("FALSE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("FALSE")
                .setAllowCreditAuthorisations("TRUE")
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(new ArrayList<>()))
                .body("blockedMerchantCountries", equalTo(new ArrayList<>()))
                .body("maxTransactionAmount.value", nullValue())
                .body("minTransactionAmount.value", nullValue())
                .body("spendLimit[0].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getCurrency()))
                .body("spendLimit[0].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getAmount().toString()))
                .body("spendLimit[0].interval", equalTo(spendRulesModel.getSpendLimit().get(0).getInterval()));
    }

    @Test
    public void SetManagedCardSpendRules_LimitAmountZero_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(0L).setHasValue(true).build())
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(0L).setHasValue(true).build())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                .body("allowContactless", equalTo(spendRulesModel.getAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.getAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.getAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.getAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.getAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.getBlockedMerchantCountries()))
                .body("maxTransactionAmount.value", nullValue())
                .body("minTransactionAmount.value", nullValue())
                .body("spendLimit[0].limitAmount.currency", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getCurrency()))
                .body("spendLimit[0].limitAmount.amount", equalTo(spendRulesModel.getSpendLimit().get(0).getLimitAmount().getAmount().toString()))
                .body("spendLimit[0].interval", equalTo(spendRulesModel.getSpendLimit().get(0).getInterval()));
    }

    @Test
    public void SetManagedCardSpendRules_InvalidMaximumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(-1L).setHasValue(true).build())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SetManagedCardSpendRules_InvalidMinimumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(-1L).setHasValue(true).build())
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void SetManagedCardSpendRules_InvalidAllowedCountry_BadRequest(final String country){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setAllowedMerchantCountries(Collections.singletonList(country))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void SetManagedCardSpendRules_InvalidBlockedCountry_BadRequest(final String country){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .setBlockedMerchantCountries(Collections.singletonList(country))
                .build();

        AdminService
                .addCardSpendRules(spendRulesModel, managedCardId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
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

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}