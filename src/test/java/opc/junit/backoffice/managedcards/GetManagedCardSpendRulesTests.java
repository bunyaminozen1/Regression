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
import opc.models.backoffice.SpendLimitModel;
import opc.models.backoffice.SpendRulesModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.SpendRulesResponseModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetManagedCardSpendRulesTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static ManagedCardDetails corporatePrepaidManagedCard;
    private static ManagedCardDetails consumerPrepaidManagedCard;
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;
    private static SpendRulesResponseModel corporateDebitProfileRules;
    private static SpendRulesResponseModel corporatePrepaidProfileRules;
    private static SpendRulesResponseModel consumerDebitProfileRules;
    private static SpendRulesResponseModel consumerPrepaidProfileRules;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

       // corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        corporateImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(corporateId, IdentityType.CORPORATE, secretKey);
        //consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(consumerId, IdentityType.CONSUMER, secretKey);

        corporatePrepaidManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerPrepaidManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

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

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporateDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void GetManagedCardSpendRules_PrepaidCorporate_Success(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void GetManagedCardSpendRules_DebitConsumer_Success(final LimitInterval interval){

        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), interval)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), consumerImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerCurrency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedCardSpendRules_PrepaidConsumerLevelV1_Success(final Currency currency) throws InterruptedException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);
        final String consumerV1ImpersonateToken =
                BackofficeHelper.impersonateIdentity(consumer.getLeft(), IdentityType.CONSUMER, secretKey);

        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, currency.name(), consumer.getRight()).getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerV1ImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //TODO - Update these tests with post and solve timing problem
        TimeUnit.SECONDS.sleep(10);
        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCardId, consumerV1ImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, currency.name(), consumerPrepaidProfileRules, CardMode.PREPAID, true);
    }

    @Test
    public void GetManagedCardSpendRules_PrepaidConsumerLevelV2_Success(){
        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerCurrency, consumerPrepaidProfileRules, CardMode.PREPAID, false);
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
    public void GetManagedCardSpendRules_DebitPhysicalCard_Success(final LimitInterval interval){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 100L), interval)))
                .build();

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, corporateAuthenticationToken);

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
    public void GetManagedCardSpendRules_PrepaidPhysicalCard_Success(){
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(new ArrayList<>())
                .build();

        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCardId, corporateAuthenticationToken);

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey,  managedCardId, corporateImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, corporateCurrency, corporatePrepaidProfileRules, CardMode.PREPAID, false);
    }

    @Test
    public void GetManagedCardSpendRules_MultipleLimits_Success(){

        final ManagedCardDetails managedCard = createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                consumerCurrency, consumerAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 1000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), LimitInterval.DAILY)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCard.getManagedCardId(), consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse response =
                BackofficeMultiService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), consumerImpersonateToken)
                        .then()
                        .statusCode(SC_OK);

        assertSpendRules(response, spendRulesModel, consumerCurrency, consumerDebitProfileRules, CardMode.DEBIT, false);
    }

    @Test
    public void GetManagedCardSpendRules_CorporateRetrievesUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, user.getRight())
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
    public void GetManagedCardSpendRules_RequiredOnly_Success(){
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless(true)
                .setAllowAtm(false)
                .setAllowECommerce(true)
                .setAllowCashback(false)
                .setAllowCreditAuthorisations(true)
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
    public void GetManagedCardSpendRules_UnknownManagedCardId_NotFound() {
        BackofficeMultiService.getManagedCardsSpendRules(secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("GetManagedCardSpendRules_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void GetManagedCardSpendRules_NoManagedCardId_NotFound() {
        BackofficeMultiService.getManagedCardsSpendRules(secretKey, "", corporateImpersonateToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCardSpendRules_InnovatorTokenWithoutImpersonation_Forbidden(){
        BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, corporatePrepaidManagedCard.getManagedCardId(), innovatorToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardSpendRules_CorporateToken_Forbidden(){
        BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardSpendRules_ConsumerToken_Forbidden(){
        BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, corporatePrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardSpendRules_InvalidToken_Unauthorised(){

        BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, corporatePrepaidManagedCard.getManagedCardId(), RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardSpendRules_InvalidApiKey_Unauthorised(){
        BackofficeMultiService
                .getManagedCardsSpendRules("abc", corporatePrepaidManagedCard.getManagedCardId(), corporateImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardSpendRules_NoApiKey_BadRequest(){
        BackofficeMultiService
                .getManagedCardsSpendRules("", corporatePrepaidManagedCard.getManagedCardId(), corporateImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetManagedCardSpendRules_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardSpendRules_CrossIdentityImpersonation_Unauthorised() {

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
                .getManagedCardsSpendRules(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardSpendRules_OtherCorporateIdentityImpersonation_Unauthorised() {

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
                .getManagedCardsSpendRules(secretKey, managedCardId, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardSpendRules_OtherConsumerIdentityImpersonation_Unauthorised() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String newConsumerId = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey).getLeft();

        final String token = BackofficeHelper.impersonateIdentity(newConsumerId, IdentityType.CONSUMER, secretKey);

        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(consumerCurrency, 100L), LimitInterval.ALWAYS)))
                .build();

        BackofficeMultiService
                .postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService
                .getManagedCardsSpendRules(secretKey, managedCardId, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
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

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
