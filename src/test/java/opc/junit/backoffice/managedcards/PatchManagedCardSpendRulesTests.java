package opc.junit.backoffice.managedcards;

import commons.enums.Currency;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.backoffice.SpendLimitModel;
import opc.models.backoffice.SpendRulesModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatchManagedCardSpendRulesTests extends BaseManagedCardsSetup {

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

        //corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        corporateImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(corporateId, IdentityType.CORPORATE, secretKey);
        consumerV1ImpersonateToken = BackofficeHelper.impersonateIdentity(consumerV1Id, IdentityType.CONSUMER, secretKey);
        consumerV2ImpersonateToken = BackofficeHelper.impersonateIdentity(consumerV2Id, IdentityType.CONSUMER, secretKey);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporate_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        BackofficeMultiService
                        .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporateJustSpendRules_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowedMerchantCategories(Collections.singletonList("8877665544")).build();

        spendRulesModel.setAllowedMerchantCategories(Collections.singletonList("8877665544")).build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PatchManagedCardSpendRules_DebitCorporate_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 200L), randomInterval)))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 200L), randomInterval)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void PatchManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV1_Success(final Currency currency){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, currency.name(), consumerV1AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true)
                        .build();

        spendRulesModel.setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true);

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), currency.name(), consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV2_Success(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowedMerchantIds(Collections.singletonList("887766"))
                        .build();

        spendRulesModel.setAllowedMerchantIds(Collections.singletonList("887766"));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidConsumerJustSpendRulesKycLevelV1User_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumerV1AuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV1Currency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true)
                        .build();

        spendRulesModel.setBlockedMerchantCategories(Collections.singletonList("8888")).setAllowAtm(true);

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV1Currency, consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidConsumer_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)))
                        .build();

        spendRulesModel
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PatchManagedCardSpendRules_DebitConsumer_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 200L), randomInterval)))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 200L), randomInterval)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_MultipleLimits_Success(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                        .build();

        spendRulesModel.setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1000L), LimitInterval.ALWAYS),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporateUserJustSpendRules_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowContactless(false)
                        .build();

        spendRulesModel.setAllowContactless(false);

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidCorporateUser_SpendLimitNotSupportedForPrepaidMode(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidPhysicalCardJustSpendRules_Success(){

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setBlockedMerchantCategories(Collections.singletonList("88776655"))
                        .build();

        spendRulesModel.setBlockedMerchantCategories(Collections.singletonList("88776655"));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void PatchManagedCardSpendRules_PrepaidPhysicalCard_SpendLimitNotSupportedForPrepaidMode(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_NOT_SUPPORTED_FOR_PREPAID_MODE_CARD"));
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void PatchManagedCardSpendRules_DebitPhysicalCard_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, consumerV2AuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), interval)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 300L), randomInterval)))
                        .setBlockedMerchantCountries(Collections.singletonList("FR"))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 300L), randomInterval)))
                .setBlockedMerchantCountries(Collections.singletonList("FR"));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_CorporatePatchUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, user.getRight())
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LimitInterval randomInterval = LimitInterval.random();

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), randomInterval)))
                        .build();

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), randomInterval)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_DifferentCurrency_SpendLimitCurrencyDifferentFromCardCurrency(){
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();
        final String spendLimitCurrency =
                Currency.getRandomWithExcludedCurrency(Currency.valueOf(consumerV2Currency)).toString();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerV2Currency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(spendLimitCurrency, 100L), LimitInterval.ALWAYS)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void PatchManagedCardSpendRules_DuplicateIntervals_SpendLimitDuplicateInterval(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY),
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10L), LimitInterval.DAILY)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_DUPLICATE_INTERVAL"));
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidLimitAmount_SpendLimitAmountInvalid(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, -100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_AMOUNT_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "UNKNOWN" })
    public void PatchManagedCardSpendRules_InvalidInterval_BadRequest(final String interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setInterval(interval);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit"))
                .body("validationErrors[1].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[2].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[2].fieldName", equalTo("interval"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ABCD" })
    public void PatchManagedCardSpendRules_InvalidCurrency_BadRequest(final String currency){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(currency, 100L), LimitInterval.DAILY);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", anyOf(is("HAS_TEXT"), is("RANGE")))
                .body("validationErrors[0].fieldName", equalTo("spendLimit.value.currency"));

    }

    @Test
    public void PatchManagedCardSpendRules_UnknownCurrency_SpendLimitCurrencyDifferentFromCardCurrency(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount("ABC", 100L), LimitInterval.DAILY);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SPEND_LIMIT_CURRENCY_DIFFERENT_FROM_CARD_CURRENCY"));
    }

    @Test
    public void PatchManagedCardSpendRules_NullLimitValue_BadRequest(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setValue(null);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit.value"));
    }

    @Test
    public void PatchManagedCardSpendRules_NullLimitInterval_BadRequest(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendLimitModel spendLimitModel =
                new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)
                        .setInterval(null);

        spendRulesModel.setSpendLimit(Collections.singletonList(spendLimitModel));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("spendLimit.interval"));
    }

    @Test
    public void PatchManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("PatchManagedCardSpendRules_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void PatchManagedCardSpendRules_NoManagedCardId_NotFound() {
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, "", corporateImpersonateToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void PatchManagedCardSpendRules_CrossIdentityImpersonation_Unauthorised() {

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
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_OtherCorporateIdentityImpersonation_Unauthorised() {

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
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_OtherConsumerIdentityImpersonation_Unauthorised() {

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
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidToken_Unauthorised(){

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setAllowAtm(true);

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidApiKey_Unauthorised(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, "abc", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCardSpendRules_NoApiKey_BadRequest(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, "", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void PatchManagedCardSpendRules_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCardSpendRules_CorporateToken_Forbidden(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCardSpendRules_ConsumerToken_Forbidden(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, RandomStringUtils.randomNumeric(18), consumerV2AuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCardSpendRules_DebitCorporateBlockedCard_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel updatedSpendRulesModel =
                SpendRulesModel.builder().setAllowAtm(true)
                        .build();

        spendRulesModel.setAllowAtm(true);

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_LimitAmountZero_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel.build(), corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidMaximumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setMaxTransactionAmount(-1L);

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].message", equalTo("maxTransactionAmount must be greater than or equal to 0"))
                .body("validationErrors[0].error", equalTo("AT_LEAST"))
                .body("validationErrors[0].fieldName", equalTo("maxTransactionAmount"));
    }

    @Test
    public void PatchManagedCardSpendRules_InvalidMinimumAmount_BadRequest(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setMinTransactionAmount(-1L);

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].message", equalTo("minTransactionAmount must be greater than or equal to 0"))
                .body("validationErrors[0].error", equalTo("AT_LEAST"))
                .body("validationErrors[0].fieldName", equalTo("minTransactionAmount"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void PatchManagedCardSpendRules_InvalidAllowedCountry_BadRequest(final String country){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setAllowedMerchantCountries(Collections.singletonList(country));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_REQUEST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MLT", "M", "123", "mt"})
    public void PatchManagedCardSpendRules_InvalidBlockedCountry_BadRequest(final String country){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        spendRulesModel.setBlockedMerchantCountries(Collections.singletonList(country));

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_REQUEST"));
    }

    @ParameterizedTest
    @MethodSource("singleFieldUpdate")
    public void PatchManagedCardSpendRules_CheckAllFields_Success(final SpendRulesModel spendRulesModel){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel initialSpendRulesModel = SpendRulesModel.fullDefaultSpendRulesModel(corporateCurrency).build();

        BackofficeMultiService
                .postManagedCardsSpendRules(initialSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
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
    public void PatchManagedCardSpendRules_NoRulesSet_NotFound(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        BackofficeMultiService
                .patchManagedCardsSpendRules(spendRulesModel,
                        secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCardSpendRules_RulesDeleted_NotFound(){

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
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .patchManagedCardsSpendRules(SpendRulesModel.DefaultSpendRulesModel().setAllowedMerchantCountries(Collections.singletonList("ES")).build(),
                        secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCardSpendRules_EmptyListUnchanged_Success(){

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel,
                        secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.builder();

        BackofficeMultiService
                .patchManagedCardsSpendRules(updatedSpendRulesModel.setBlockedMerchantCategories(new ArrayList<>()).build(),
                        secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        updatedSpendRulesModel.setAllowedMerchantCategories(spendRulesModel.getAllowedMerchantCategories());
        updatedSpendRulesModel.setAllowedMerchantIds(spendRulesModel.getAllowedMerchantIds());
        updatedSpendRulesModel.setBlockedMerchantIds(spendRulesModel.getBlockedMerchantIds());
        updatedSpendRulesModel.setAllowedMerchantCountries(spendRulesModel.getAllowedMerchantCountries());
        updatedSpendRulesModel.setBlockedMerchantCountries(spendRulesModel.getBlockedMerchantCountries());
        updatedSpendRulesModel.setAllowContactless(spendRulesModel.isAllowContactless());
        updatedSpendRulesModel.setAllowCashback(spendRulesModel.isAllowCashback());
        updatedSpendRulesModel.setAllowAtm(spendRulesModel.isAllowAtm());
        updatedSpendRulesModel.setAllowCreditAuthorisations(spendRulesModel.isAllowCreditAuthorisations());
        updatedSpendRulesModel.setAllowECommerce(spendRulesModel.isAllowECommerce());
        updatedSpendRulesModel.setMaxTransactionAmount(spendRulesModel.getMaxTransactionAmount());
        updatedSpendRulesModel.setMinTransactionAmount(spendRulesModel.getMinTransactionAmount());

        assertDeletedSpendRulesThroughPatch(response, updatedSpendRulesModel.build(), corporateCurrency, corporateDebitProfileRules);
    }

    @Test
    public void PatchManagedCardSpendRules_NullItemsDeleteValue_Success(){

        final String managedCardId =
            createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
            .postManagedCardsSpendRules(spendRulesModel.build(),
                secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.nullableSpendRulesModel();

        BackofficeMultiService
            .patchManagedCardsSpendRules(updatedSpendRulesModel.build(),
                secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
            BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_OK);

        assertDeletedSpendRulesThroughPatch(response, updatedSpendRulesModel.build(), corporateCurrency, corporateDebitProfileRules);
    }

    @Test
    public void PatchManagedCardSpendRules_NullInListNoEffect_Success(){

        final String managedCardId =
            createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
            .postManagedCardsSpendRules(spendRulesModel.build(),
                secretKey, managedCardId, corporateImpersonateToken)
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

        spendRulesModel.setBlockedMerchantCountries(Collections.singletonList("BA"));

        BackofficeMultiService
            .patchManagedCardsSpendRules(updatedSpendRulesModel.build(),
                secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
            BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_OK);

        assertDeletedSpendRulesThroughPatch(response, updatedSpendRulesModel.build(), corporateCurrency, corporateDebitProfileRules);
    }

    @Test
    public void PatchManagedCardSpendRules_NullsDeleteValue_Success(){

        final String managedCardId =
            createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
            .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), LimitInterval.DAILY)));

        BackofficeMultiService
            .postManagedCardsSpendRules(spendRulesModel.build(),
                secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final SpendRulesModel.Builder updatedSpendRulesModel = SpendRulesModel.nullableSpendRulesModel()
            .setBlockedMerchantCategories(Collections.singletonList(null))
            .setBlockedMerchantIds(Collections.singletonList(null))
            .setAllowedMerchantCategories(Collections.singletonList(null))
            .setAllowedMerchantIds(Collections.singletonList(null))
            .setBlockedMerchantCountries(Collections.singletonList(null))
            .setAllowedMerchantCountries(Collections.singletonList(null));

        BackofficeMultiService
            .patchManagedCardsSpendRules(updatedSpendRulesModel.build(),
                secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
            BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_OK);

        assertDeletedSpendRulesThroughPatch(response, updatedSpendRulesModel.build(), corporateCurrency, corporateDebitProfileRules);
    }

    @Test
    public void PatchManagedCardSpendRules_ListChangesForSingleProperty_AllowedNullsAsItem_Success(){
        final String managedCardId =
            createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        final SpendRulesModel.Builder spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
            .setSpendLimit(new ArrayList<>());

        BackofficeMultiService
            .postManagedCardsSpendRules(spendRulesModel.build(), secretKey, managedCardId, corporateImpersonateToken)
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

        BackofficeMultiService
            .patchManagedCardsSpendRules(updatedSpendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
            .then()
            .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
            BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, managedCardId, corporateImpersonateToken)
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

    private void assertDeletedSpendRulesThroughPatch(final ValidatableResponse response,
                                                     final SpendRulesModel spendRulesModel,
                                                     final String currency,
                                                     final SpendRulesResponseModel spendRulesResponseModel) {
        response
                .body("allowedMerchantCategories", equalTo(extractNullValues(spendRulesModel.getAllowedMerchantCategories())))
                .body("blockedMerchantCategories", equalTo(extractNullValues(spendRulesModel.getBlockedMerchantCategories())))
                .body("allowedMerchantIds", equalTo(extractNullValues(spendRulesModel.getAllowedMerchantIds())))
                .body("blockedMerchantIds", equalTo(extractNullValues(spendRulesModel.getBlockedMerchantIds())))
                .body("allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                .body("allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                .body("allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                .body("allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                .body("allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()))
                .body("allowedMerchantCountries", equalTo(extractNullValues(spendRulesModel.getAllowedMerchantCountries())))
                .body("blockedMerchantCountries", equalTo(extractNullValues(spendRulesModel.getBlockedMerchantCountries())))
                .body("maxTransactionAmount", equalTo(spendRulesModel.getMaxTransactionAmount() == null ?
                        null :  spendRulesModel.getMaxTransactionAmount().intValue()))
                .body("minTransactionAmount", equalTo(spendRulesModel.getMinTransactionAmount() == null ?
                        null :  spendRulesModel.getMinTransactionAmount().intValue()))
                .body("cardLevelSpendRules.allowedMerchantCategories", equalTo(extractNullValues(spendRulesModel.getAllowedMerchantCategories())))
                .body("cardLevelSpendRules.blockedMerchantCategories", equalTo(extractNullValues(spendRulesModel.getBlockedMerchantCategories())))
                .body("cardLevelSpendRules.allowedMerchantIds", equalTo(extractNullValues(spendRulesModel.getAllowedMerchantIds())))
                .body("cardLevelSpendRules.blockedMerchantIds", equalTo(extractNullValues(spendRulesModel.getBlockedMerchantIds())))
                .body("cardLevelSpendRules.allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                .body("cardLevelSpendRules.allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                .body("cardLevelSpendRules.allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                .body("cardLevelSpendRules.allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                .body("cardLevelSpendRules.allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()))
                .body("cardLevelSpendRules.allowedMerchantCountries", equalTo(extractNullValues(spendRulesModel.getAllowedMerchantCountries())))
                .body("cardLevelSpendRules.blockedMerchantCountries", equalTo(extractNullValues(spendRulesModel.getBlockedMerchantCountries())))
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
                    .body("spendLimit[0].value.amount", equalTo(100))
                    .body("spendLimit[0].interval", equalTo(LimitInterval.DAILY.name()))
                    .body("cardLevelSpendRules.spendLimit[0].value.currency", equalTo(currency))
                    .body("cardLevelSpendRules.spendLimit[0].value.amount", equalTo(100))
                    .body("cardLevelSpendRules.spendLimit[0].interval", equalTo(LimitInterval.DAILY.name()))
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        }

        assertEquals("{}", response.extract().jsonPath().get("identityLevelSpendRules").toString());
    }

    private static List<String> extractNullValues(final List<String> list) {

        if (list != null) {
            final List<String> updatedList = list.stream().filter(Objects::nonNull).collect(Collectors.toList());

            return updatedList.size() == 0 ? null : updatedList;
        }

        return null;
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
