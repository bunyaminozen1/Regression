package opc.junit.multi.managedcards;

import commons.enums.Currency;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.MANAGED_CARDS_SPEND_RULES)
public class PatchManagedCardSpendRulesTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerV1AuthenticationToken;
    private static String consumerV2AuthenticationToken;
    private static String corporateCurrency;
    private static String consumerV1Currency;
    private static String consumerV2Currency;
    private static String corporateId;
    private static String consumerV2Id;
    private static SpendRulesResponseModel corporateDebitProfileRules;
    private static SpendRulesResponseModel corporatePrepaidProfileRules;
    private static SpendRulesResponseModel consumerDebitProfileRules;
    private static SpendRulesResponseModel consumerPrepaidProfileRules;

    @BeforeAll
    public static void Setup() {
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
    public void PatchManagedCardSpendRules_PrepaidCorporate_SpendLimitNotSupportedForPrepaidMode() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporateJustSpendRules_Success() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowedMerchantCategories(Collections.singletonList("8877665544")).build();

        spendRulesModel.setAllowedMerchantCategories(Collections.singletonList("8877665544")).build();

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PatchManagedCardSpendRules_DebitCorporate_Success(final LimitInterval interval) {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 200L), randomInterval)))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 200L), randomInterval)));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV1_Success(final Currency currency) {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, currency.name(), consumerV1AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV1AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true)
                        .build();

        spendRulesModel.setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true);

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV1AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), currency.name(), consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV2_Success() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowedMerchantIds(Collections.singletonList("887766"))
                        .build();

        spendRulesModel.setAllowedMerchantIds(Collections.singletonList("887766"));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV1User_Success() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumerV1AuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV1Currency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true)
                        .build();

        spendRulesModel.setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true);

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV1Currency, consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidConsumer_SpendLimitNotSupportedForPrepaidMode() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                        .build();

        spendRulesModel
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PatchManagedCardSpendRules_DebitConsumer_Success(final LimitInterval interval) {
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 200L), randomInterval)))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 200L), randomInterval)));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_MultipleLimits_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                        .build();

        spendRulesModel.setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporateUserJustSpendRules_Success() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowContactless(false)
                        .build();

        spendRulesModel.setAllowContactless(false);

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporateUser_SpendLimitNotSupportedForPrepaidMode() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidPhysicalCardJustSpendRules_Success() {

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList("88776655"))
                        .build();

        spendRulesModel.setBlockedMerchantCategories(Collections.singletonList("88776655"));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidPhysicalCard_SpendLimitNotSupportedForPrepaidMode() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PatchManagedCardSpendRules_DebitPhysicalCard_Success(final LimitInterval interval) {
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 300L), randomInterval)))
                        .setBlockedMerchantCountries(Collections.singletonList("FR"))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 300L), randomInterval)))
                .setBlockedMerchantCountries(Collections.singletonList("FR"));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_CorporatePatchUserCard_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), randomInterval)))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), randomInterval)));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_DifferentCurrency_SpendLimitCurrencyDifferentFromCardCurrency() {
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();
        final String spendLimitCurrency =
                Currency.getRandomWithExcludedCurrency(Currency.valueOf(consumerV2Currency)).toString();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(spendLimitCurrency, 100L), LimitInterval.ALWAYS)));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void PatchManagedCardSpendRules_DuplicateIntervals_SpendLimitDuplicateInterval() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10L), LimitInterval.DAILY)));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_DUPLICATE_INTERVAL"));
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidLimitAmount_SpendLimitAmountInvalid() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, -100L), LimitInterval.DAILY)));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_AMOUNT_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UNKNOWN"})
    public void PatchManagedCardSpendRules_InvalidInterval_BadRequest(final String interval) {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setInterval(interval);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ABCD"})
    public void PatchManagedCardSpendRules_InvalidCurrency_BadRequest(final String currency) {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(currency, 100L), LimitInterval.DAILY);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCardSpendRules_UnknownCurrency_SpendLimitCurrencyDifferentFromCardCurrency() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount("ABC", 100L), LimitInterval.DAILY);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void PatchManagedCardSpendRules_NullLimitValue_BadRequest() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setValue(null);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCardSpendRules_NullLimitInterval_BadRequest() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setInterval(null);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("PatchManagedCardSpendRules_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void PatchManagedCardSpendRules_NoManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCardSpendRules_CrossIdentityCheck_NotFound() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then().statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCardSpendRules_CrossSameIdentityTypeCheck_NotFound() {

        final Pair<String, String> newConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then().statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, newConsumer.getRight())
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidToken_Unauthorised() {

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setAllowAtm(true);

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidApiKey_Unauthorised() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, "abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_NoApiKey_BadRequest() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, "", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCardSpendRules_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCardSpendRules_RootUserLoggedOut_Unauthorised() {
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_BackofficeCorporateImpersonator_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCardSpendRules_BackofficeConsumerImpersonator_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, getBackofficeImpersonateToken(consumerV2Id, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCardSpendRules_DebitCorporateBlockedCard_Success() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowAtm(true)
                        .build();

        spendRulesModel.setAllowAtm(true);

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_LimitAmountZero_Success() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder()
                        .setMaxTransactionAmount(0L)
                        .setMinTransactionAmount(0L)
                        .build();

        spendRulesModel
                .setMaxTransactionAmount(0L)
                .setMinTransactionAmount(0L);

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidMaximumAmount_BadRequest() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setMaxTransactionAmount(-1L);

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.maxTransactionAmount: must be greater than or equal to 0"));
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidMinimumAmount_BadRequest() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setMinTransactionAmount(-1L);

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.minTransactionAmount: must be greater than or equal to 0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void PatchManagedCardSpendRules_InvalidAllowedCountry_BadRequest(final String country) {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setAllowedMerchantCountries(Collections.singletonList(country));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void PatchManagedCardSpendRules_InvalidBlockedCountry_BadRequest(final String country) {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setBlockedMerchantCountries(Collections.singletonList(country));

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("singleFieldUpdate")
    public void PatchManagedCardSpendRules_CheckAllFields_Success(final SpendRulesModel spendRulesModel) {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel initialSpendRulesModel = SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).build();

        ManagedCardsService
                .postManagedCardsSpendRules(initialSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_NoRulesSet_NotFound() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        ManagedCardsService
                .patchManagedCardsSpendRules(spendRulesModel,
                        secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCardSpendRules_RulesDeleted_NotFound() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .patchManagedCardsSpendRules(SpendRulesModel.DefaultSpendRulesModel().setAllowedMerchantCountries(Collections.singletonList("ES")).build(),
                        secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCardSpendRules_EmptyListRuleDeleted_Success() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(),
                        secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.build().getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.build().getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.build().getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.build().getBlockedMerchantIds()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.build().getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.build().getBlockedMerchantCountries()));

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.builder();

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel.setBlockedMerchantCategories(new ArrayList<>()).build(),
                        secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.build().getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", nullValue())
                .body("allowedMerchantIds", equalTo(spendRulesModel.build().getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.build().getBlockedMerchantIds()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.build().getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.build().getBlockedMerchantCountries()));
    }

    @Test
    public void PatchManagedCardSpendRules_ListNoChanges_AllowedNullsAsItem_Success() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(),
                        secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.nullableSpendRulesModel();

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel.build(),
                        secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.build().getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.build().getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.build().getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.build().getBlockedMerchantIds()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.build().getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.build().getBlockedMerchantCountries()));
    }

    @Test
    public void PatchManagedCardSpendRules_ListChanges_AllowedNullsAsItem_Success() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
            .postManagedCardsSpendRules(spendRulesModel.build(),
                secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_NO_CONTENT);

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.nullableSpendRulesModel()
            .setBlockedMerchantIds(Collections.singletonList(null))
            .setBlockedMerchantCountries(new ArrayList<>() {
                {
                    add("BA");
                    add(null);
                }
            });

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel.build(),
                        secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        List <String> expectedResultBlockedMerchantCountries = new ArrayList<>() {
            {
                add("BA");
            }};
        ManagedCardsService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("blockedMerchantCategories", equalTo(spendRulesModel.build().getBlockedMerchantCategories()))
                .body("blockedMerchantIds", nullValue())
                .body("allowedMerchantCategories", equalTo(spendRulesModel.build().getAllowedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.build().getAllowedMerchantIds()))
                .body("blockedMerchantCountries", equalTo(expectedResultBlockedMerchantCountries))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.build().getAllowedMerchantCountries()));
    }

    @Test
    public void PatchManagedCardSpendRules_ListChangesOnlyNullAsValueRulesDeleted_Success() {

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(),
                        secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("allowedMerchantCategories", equalTo(spendRulesModel.build().getAllowedMerchantCategories()))
                .body("blockedMerchantCategories", equalTo(spendRulesModel.build().getBlockedMerchantCategories()))
                .body("allowedMerchantIds", equalTo(spendRulesModel.build().getAllowedMerchantIds()))
                .body("blockedMerchantIds", equalTo(spendRulesModel.build().getBlockedMerchantIds()))
                .body("allowedMerchantCountries", equalTo(spendRulesModel.build().getAllowedMerchantCountries()))
                .body("blockedMerchantCountries", equalTo(spendRulesModel.build().getBlockedMerchantCountries()));

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.nullableSpendRulesModel()
            .setBlockedMerchantCategories(Collections.singletonList(null))
            .setBlockedMerchantIds(Collections.singletonList(null))
            .setAllowedMerchantCategories(Collections.singletonList(null))
            .setAllowedMerchantIds(Collections.singletonList(null))
            .setBlockedMerchantCountries(Collections.singletonList(null))
            .setAllowedMerchantCountries(Collections.singletonList(null));

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel.build(),
                        secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("blockedMerchantCategories", nullValue())
                .body("blockedMerchantIds", nullValue())
                .body("allowedMerchantCategories", nullValue())
                .body("allowedMerchantIds", nullValue())
                .body("blockedMerchantCountries", nullValue())
                .body("allowedMerchantCountries", nullValue());
    }

    @Test
    public void PatchManagedCardSpendRules_ListChangesForSingleProperty_AllowedNullsAsItem_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowedMerchantIds(new ArrayList<>() {
                    {
                        add("firstId");
                        add(null);
                        add("secondId");
                        add(null);
                    }
                }).build();

        spendRulesModel.setAllowedMerchantIds(new ArrayList<>(List.of("firstId", "secondId"))).build();

        ManagedCardsService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
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
                        null : spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount() == null ?
                        null : spendRulesModel.getMinTransactionAmount().intValue()))
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
                        null : spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("cardLevelSpendRules.minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount() == null ?
                        null : spendRulesModel.getMinTransactionAmount().intValue()))
                .body("profileLevelSpendRules.allowedMerchantCategories", equalTo(spendRulesResponseModel.getAllowedMerchantCategories()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantCategories()))
                .body("profileLevelSpendRules.blockedMerchantCategories", equalTo(spendRulesResponseModel.getBlockedMerchantCategories()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantCategories()))
                .body("profileLevelSpendRules.allowedMerchantIds", equalTo(spendRulesResponseModel.getAllowedMerchantIds()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantIds()))
                .body("profileLevelSpendRules.blockedMerchantIds", equalTo(spendRulesResponseModel.getBlockedMerchantIds()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantIds()))
                .body("profileLevelSpendRules.allowContactless", equalTo(spendRulesResponseModel.isAllowContactless().equals("NULL") ? null :
                        Boolean.valueOf(spendRulesResponseModel.isAllowContactless())))
                .body("profileLevelSpendRules.allowAtm", equalTo(spendRulesResponseModel.isAllowAtm().equals("NULL") ? null :
                        Boolean.valueOf(spendRulesResponseModel.isAllowAtm())))
                .body("profileLevelSpendRules.allowECommerce", equalTo(spendRulesResponseModel.isAllowECommerce().equals("NULL") ? null :
                        Boolean.valueOf(spendRulesResponseModel.isAllowECommerce())))
                .body("profileLevelSpendRules.allowCashback", equalTo(spendRulesResponseModel.isAllowCashback().equals("NULL") ? null :
                        Boolean.valueOf(spendRulesResponseModel.isAllowCashback())))
                .body("profileLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesResponseModel.isAllowCreditAuthorisations().equals("NULL") ? null :
                        Boolean.valueOf(spendRulesResponseModel.isAllowCreditAuthorisations())))
                .body("profileLevelSpendRules.allowedMerchantCountries", equalTo(spendRulesResponseModel.getAllowedMerchantCountries()
                        .size() == 0 ? null : spendRulesResponseModel.getAllowedMerchantCountries()))
                .body("profileLevelSpendRules.blockedMerchantCountries", equalTo(spendRulesResponseModel.getBlockedMerchantCountries()
                        .size() == 0 ? null : spendRulesResponseModel.getBlockedMerchantCountries()))
                .body("profileLevelSpendRules.maxTransactionAmount", equalTo(spendRulesResponseModel.getMaxTransactionAmount() == null ?
                        null : spendRulesResponseModel.getMaxTransactionAmount().intValue()))
                .body("profileLevelSpendRules.minTransactionAmount", equalTo(spendRulesResponseModel.getMinTransactionAmount() == null ?
                        null : spendRulesResponseModel.getMinTransactionAmount().intValue()));

        if (cardMode.equals(CardMode.PREPAID)) {
            response
                    .body("spendLimit", nullValue())
                    .body("cardLevelSpendRules.spendLimit", nullValue())
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        } else {

            if (spendRulesModel.getSpendLimit() != null) {
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

        if (isLevel1) {

            final int maxTransactionAmount;
            switch (currency) {
                case "EUR":
                    maxTransactionAmount = 5000;
                    break;
                case "GBP":
                    maxTransactionAmount = 4200;
                    break;
                case "USD":
                    maxTransactionAmount = 5600;
                    break;
                default:
                    throw new IllegalArgumentException("Currency not supported.");

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
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowedMerchantCategories(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setBlockedMerchantCategories(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowedMerchantIds(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setBlockedMerchantIds(Collections.singletonList(RandomStringUtils.randomAlphabetic(5))).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowedMerchantCountries(Collections.singletonList("MT")).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setBlockedMerchantCountries(Collections.singletonList("IT")).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowAtm(true).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowCashback(true).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowContactless(true).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowCreditAuthorisations(true).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowECommerce(true).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowAtm(false).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowCashback(false).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowContactless(false).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowCreditAuthorisations(false).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setAllowECommerce(false).build()),
                Arguments.of(SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS))).build()));
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
