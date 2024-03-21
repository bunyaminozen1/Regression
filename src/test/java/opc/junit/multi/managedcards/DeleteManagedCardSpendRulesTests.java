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
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.MANAGED_CARDS_SPEND_RULES)
public class DeleteManagedCardSpendRulesTests extends BaseManagedCardsSetup {

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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV1AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, consumerV1AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1AuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, consumerV2Currency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_CorporateDeleteUserCard_Success(){

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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, user.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void DeleteManagedCardSpendRules_UnknownManagedCardId_NotFound() {

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("DeleteManagedCardSpendRules_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void DeleteManagedCardSpendRules_NoManagedCardId_NotFound() {

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void DeleteManagedCardSpendRules_CrossIdentityCheck_NotFound(){

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then().statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerPrepaidProfileRules);
    }

    @Test
    public void DeleteManagedCardSpendRules_CrossSameIdentityTypeCheck_NotFound(){

        final Pair<String, String> newConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then().statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, newConsumer.getRight())
                .then().statusCode(SC_NOT_FOUND);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV2AuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerPrepaidProfileRules);
    }

    @Test
    public void DeleteManagedCardSpendRules_InvalidToken_Unauthorised(){

        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, RandomStringUtils.randomAlphanumeric(7))
                .then()
                .statusCode(SC_UNAUTHORIZED);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporatePrepaidProfileRules);
    }

    @Test
    public void DeleteManagedCardSpendRules_InvalidApiKey_Unauthorised(){

        ManagedCardsService
                .deleteManagedCardsSpendRules("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeleteManagedCardSpendRules_NoApiKey_BadRequest(){

        ManagedCardsService
                .deleteManagedCardsSpendRules("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void DeleteManagedCardSpendRules_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeleteManagedCardSpendRules_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeleteManagedCardSpendRules_BackofficeCorporateImpersonator_Forbidden(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeleteManagedCardSpendRules_BackofficeConsumerImpersonator_Forbidden(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerV2Currency, consumerV2AuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV2AuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, getBackofficeImpersonateToken(consumerV2Id, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
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
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
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

        ManagedCardsService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertEmptySpendRules(response, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);

        ManagedCardsService
                .deleteManagedCardsSpendRules(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private void assertEmptySpendRules(final ValidatableResponse response,
                                       final String currency,
                                       final SpendRulesResponseModel spendRulesResponseModel,
                                       final CardMode cardMode,
                                       final Boolean isLevel1) {
        response
                .body("allowedMerchantCategories", nullValue())
                .body("blockedMerchantCategories", nullValue())
                .body("allowedMerchantIds", nullValue())
                .body("blockedMerchantIds", nullValue())
                .body("allowContactless", nullValue())
                .body("allowAtm", nullValue())
                .body("allowECommerce", nullValue())
                .body("allowCashback", nullValue())
                .body("allowCreditAuthorisations", nullValue())
                .body("allowedMerchantCountries", nullValue())
                .body("blockedMerchantCountries", nullValue())
                .body("maxTransactionAmount", nullValue())
                .body("minTransactionAmount", nullValue())
                .body("cardLevelSpendRules.allowedMerchantCategories", nullValue())
                .body("cardLevelSpendRules.blockedMerchantCategories", nullValue())
                .body("cardLevelSpendRules.allowedMerchantIds", nullValue())
                .body("cardLevelSpendRules.blockedMerchantIds", nullValue())
                .body("cardLevelSpendRules.allowContactless", nullValue())
                .body("cardLevelSpendRules.allowAtm", nullValue())
                .body("cardLevelSpendRules.allowECommerce", nullValue())
                .body("cardLevelSpendRules.allowCashback", nullValue())
                .body("cardLevelSpendRules.allowCreditAuthorisations", nullValue())
                .body("cardLevelSpendRules.allowedMerchantCountries", nullValue())
                .body("cardLevelSpendRules.blockedMerchantCountries", nullValue())
                .body("cardLevelSpendRules.maxTransactionAmount", nullValue())
                .body("cardLevelSpendRules.minTransactionAmount", nullValue())
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

        if (cardMode.equals(CardMode.DEBIT)){
            response
                    .body("spendLimit[0].value.currency", equalTo(currency))
                    .body("spendLimit[0].value.amount", equalTo(0))
                    .body("spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                    .body("cardLevelSpendRules.spendLimit[0].value.currency", equalTo(currency))
                    .body("cardLevelSpendRules.spendLimit[0].value.amount", equalTo(0))
                    .body("cardLevelSpendRules.spendLimit[0].interval", equalTo(LimitInterval.ALWAYS.name()))
                    .body("profileLevelSpendRules.spendLimit", nullValue());
        } else {
            response
                    .body("spendLimit", nullValue())
                    .body("profileLevelSpendRules.spendLimit", nullValue());

            assertEquals("{}", response.extract().jsonPath().get("cardLevelSpendRules").toString());
        }
    }

    private void assertSpendRules(final ValidatableResponse response,
                                  final SpendRulesModel spendRulesModel,
                                  final SpendRulesResponseModel spendRulesResponseModel) {
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

        response
                .body("spendLimit", nullValue())
                .body("cardLevelSpendRules.spendLimit", nullValue())
                .body("profileLevelSpendRules.spendLimit", nullValue());

        assertEquals("{}", response.extract().jsonPath().get("identityLevelSpendRules").toString());
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
