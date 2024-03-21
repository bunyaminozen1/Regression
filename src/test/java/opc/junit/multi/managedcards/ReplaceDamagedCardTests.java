package opc.junit.multi.managedcards;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendLimitResponseModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static opc.enums.opc.CardLevelClassification.CONSUMER;
import static opc.enums.opc.CardLevelClassification.CORPORATE;
import static opc.enums.opc.InstrumentType.PHYSICAL;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class ReplaceDamagedCardTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void ReplaceManagedCard_PrepaidCorporate_Success(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, corporatePrepaidManagedCardsProfileId, CORPORATE, 0);
    }

    @Test
    public void ReplaceManagedCard_PrepaidConsumer_Success(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, consumerPrepaidManagedCardsProfileId, CONSUMER, 0);
    }

    @Test
    public void ReplaceManagedCard_DebitCorporate_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, corporateDebitManagedCardsProfileId, CORPORATE, 0);
    }

    @Test
    public void ReplaceManagedCard_DebitConsumer_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, consumerDebitManagedCardsProfileId, CONSUMER, 0);
    }

    @Test
    public void ReplaceManagedCard_CorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, corporatePrepaidManagedCardsProfileId, CORPORATE, 0);
    }

    @Test
    public void ReplaceManagedCard_CardWithFunds_Success(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final Long cardBalance = 30L;
        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, cardBalance, 1);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, corporatePrepaidManagedCardsProfileId, CORPORATE, cardBalance.intValue());
    }

    @Test
    public void ReplaceManagedCard_DebitSpendLimitChecks_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final SpendRulesModel spendRulesModel = SpendRulesModel.DefaultSpendRulesModel()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setSpendLimit(Arrays.asList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, 10000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 5000L), LimitInterval.YEARLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 2500L), LimitInterval.QUARTERLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 1250L), LimitInterval.MONTHLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 750L), LimitInterval.WEEKLY),
                        new SpendLimitModel(new CurrencyAmount(corporateCurrency, 300L), LimitInterval.DAILY)))
                .build();
        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK);

        final List<SpendLimitResponseModel> cardSpendLimits =
                Arrays.asList(new ObjectMapper().convertValue(managedCardsResponse.extract().jsonPath().get("availableToSpend"), SpendLimitResponseModel[].class));

        spendRulesModel.getSpendLimit().forEach(limit -> {
            final SpendLimitResponseModel actualLimit =
                    cardSpendLimits.stream().filter(x -> x.getInterval().equals(limit.getInterval())).collect(Collectors.toList()).get(0);

            Assertions.assertEquals(limit.getInterval(), actualLimit.getInterval());
            Assertions.assertEquals(limit.getValue().getAmount().toString(), actualLimit.getValue().get("amount"));
        });

        final ValidatableResponse managedCardsSpendRulesResponse =
                ManagedCardsService
                        .getManagedCardsSpendRules(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("allowedMerchantCategories", nullValue())
                        .body("blockedMerchantCategories", equalTo(spendRulesModel.getBlockedMerchantCategories()))
                        .body("allowedMerchantIds", equalTo(spendRulesModel.getAllowedMerchantIds()))
                        .body("blockedMerchantIds", equalTo(spendRulesModel.getBlockedMerchantIds()))
                        .body("allowContactless", equalTo(spendRulesModel.isAllowContactless()))
                        .body("allowAtm", equalTo(spendRulesModel.isAllowAtm()))
                        .body("allowECommerce", equalTo(spendRulesModel.isAllowECommerce()))
                        .body("allowCashback", equalTo(spendRulesModel.isAllowCashback()))
                        .body("allowCreditAuthorisations", equalTo(spendRulesModel.isAllowCreditAuthorisations()));

        final List<SpendLimitResponseModel> cardSpendLimitsFromRules =
                Arrays.asList(new ObjectMapper().convertValue(managedCardsSpendRulesResponse.extract().jsonPath().get("spendLimit"), SpendLimitResponseModel[].class));

        spendRulesModel.getSpendLimit().forEach(limit -> {
            final SpendLimitResponseModel actualLimit =
                    cardSpendLimitsFromRules.stream().filter(x -> x.getInterval().equals(limit.getInterval())).collect(Collectors.toList()).get(0);

            Assertions.assertEquals(limit.getInterval(), actualLimit.getInterval());
            Assertions.assertEquals(limit.getValue().getAmount().toString(), actualLimit.getValue().get("amount"));
        });

        assertSuccessfulResponse(managedCardsResponse, managedCard, corporateDebitManagedCardsProfileId, CORPORATE, 0);
    }

    @Test
    public void ReplaceManagedCard_CardNotUpgraded_InstrumentNotPhysical(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void ReplaceManagedCard_CardUpgradedNotActivated_PhysicalCardNotActivated(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PHYSICAL_CARD_NOT_ACTIVATED"));

        ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"));
    }

    @Test
    public void ReplaceManagedCard_CardAlreadyReplaced_InstrumentAlreadyPendingReplacement(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_PENDING_REPLACEMENT"));

        ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("physicalCardDetails.replacement.replacementId", equalTo(managedCard.getManagedCardId()));
    }

    @Test
    public void ReplaceManagedCard_CardBlocked_Success(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo(State.BLOCKED.name()))
                .body("state.blockedReason", equalTo("USER"))
                .body("physicalCardDetails.replacement.replacementId", equalTo(managedCard.getManagedCardId()))
                .body("physicalCardDetails.replacement.replacementReason", equalTo("DAMAGED"));
    }

    @Test
    public void ReplaceManagedCard_CardDestroyed_InstrumentAlreadyDestroyed(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_DESTROYED"));
    }

    @Test
    public void ReplaceManagedCard_CardLost_InstrumentMarkedLost(){

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.reportLostCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_MARKED_AS_LOST"));

        ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"));
    }

    @Test
    public void ReplaceManagedCard_CardStolen_InstrumentAlreadyDestroyed(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.reportStolenCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_DESTROYED"));

        ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"));
    }

    @Test
    public void ReplaceManagedCard_ReplaceCardAlreadyReplacedAfterBeingLost_InstrumentAlreadyDestroyed(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.replaceLostCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_DESTROYED"));
    }

    @Test
    public void ReplaceManagedCard_CorporateReplaceUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final ValidatableResponse managedCardsResponse = ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), user.getRight())
                .then().statusCode(SC_OK);

        assertSuccessfulResponse(managedCardsResponse, managedCard, corporatePrepaidManagedCardsProfileId, CORPORATE, 0);
    }

    @Test
    public void ReplaceManagedCard_UserReplaceCorporateCard_Unauthorized(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);

        ManagedCardsService
                .getManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"));
    }

    @Test
    public void ReplaceManagedCard_InvalidCode_BadRequest(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel =
                new ReplacePhysicalCardModel(RandomStringUtils.randomNumeric(10));

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ReplaceManagedCard_UnknownManagedCardId_NotFound() {
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReplaceManagedCard_NoManagedCardId_NotFound() {
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReplaceManagedCard_CrossIdentityCheck_NotFound(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReplaceManagedCard_InvalidApiKey_Unauthorised(){
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, "abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReplaceManagedCard_NoApiKey_BadRequest(){
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, "", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ReplaceManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReplaceManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReplaceManagedCard_BackofficeCorporateImpersonator_Forbidden(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReplaceManagedCard_BackofficeConsumerImpersonator_Forbidden(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .replaceDamagedCard(replacePhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final ManagedCardDetails managedCard,
                                          final String managedCardProfileId,
                                          final CardLevelClassification cardLevelClassification,
                                          final int cardBalance){
        response
                .body("id", equalTo(managedCard.getManagedCardId()))
                .body("profileId", equalTo(managedCardProfileId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(managedCard.getManagedCardModel().getTag()))
                .body("friendlyName", equalTo(managedCard.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo(State.ACTIVE.name()))
                .body("type", equalTo(PHYSICAL.name()))
                .body("cardBrand", equalTo(CardBrand.MASTERCARD.name()))
                .body("cardLevelClassification", equalTo(cardLevelClassification.name()))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", notNullValue())
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(managedCard.getManagedCardModel().getNameOnCard()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryPeriodMonths", equalTo(36))
                .body("renewalType", equalTo("NO_RENEW"))
                .body("creationTimestamp", notNullValue())
                .body("cardholderMobileNumber", equalTo(managedCard.getManagedCardModel().getCardholderMobileNumber()))
                .body("billingAddress.addressLine1", equalTo(managedCard.getManagedCardModel().getBillingAddress().getAddressLine1()))
                .body("billingAddress.addressLine2", equalTo(managedCard.getManagedCardModel().getBillingAddress().getAddressLine2()))
                .body("billingAddress.city", equalTo(managedCard.getManagedCardModel().getBillingAddress().getCity()))
                .body("billingAddress.postCode", equalTo(managedCard.getManagedCardModel().getBillingAddress().getPostCode()))
                .body("billingAddress.state", equalTo(managedCard.getManagedCardModel().getBillingAddress().getState()))
                .body("billingAddress.country", equalTo(managedCard.getManagedCardModel().getBillingAddress().getCountry()))
                .body("physicalCardDetails.productReference", notNullValue())
                .body("physicalCardDetails.carrierType", notNullValue())
                .body("physicalCardDetails.pendingActivation", equalTo(true))
                .body("physicalCardDetails.pinBlocked", equalTo(false))
                .body("physicalCardDetails.replacement.replacementId", equalTo(managedCard.getManagedCardId()))
                .body("physicalCardDetails.replacement.replacementReason", equalTo("DAMAGED"))
                .body("physicalCardDetails.deliveryMethod", equalTo("STANDARD_DELIVERY"))
                .body("physicalCardDetails.deliveryAddress.name", equalTo(managedCard.getPhysicalCardAddressModel().getName()))
                .body("physicalCardDetails.deliveryAddress.surname", equalTo(managedCard.getPhysicalCardAddressModel().getSurname()))
                .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(managedCard.getPhysicalCardAddressModel().getAddressLine1()))
                .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(managedCard.getPhysicalCardAddressModel().getAddressLine2()))
                .body("physicalCardDetails.deliveryAddress.city", equalTo(managedCard.getPhysicalCardAddressModel().getCity()))
                .body("physicalCardDetails.deliveryAddress.postCode", equalTo(managedCard.getPhysicalCardAddressModel().getPostCode()))
                .body("physicalCardDetails.deliveryAddress.state", equalTo(managedCard.getPhysicalCardAddressModel().getState()))
                .body("physicalCardDetails.deliveryAddress.country", equalTo(managedCard.getPhysicalCardAddressModel().getCountry()))
                .body("physicalCardDetails.manufacturingState", equalTo(ManufacturingState.REQUESTED.name()))
                .body("mode", equalTo(managedCard.getManagedCardModel().getMode()));

        if (managedCard.getManagedCardMode() == ManagedCardMode.PREPAID_MODE) {
            response
                    .body("currency", equalTo(managedCard.getManagedCardModel().getCurrency()))
                    .body("balances.availableBalance", equalTo(cardBalance))
                    .body("balances.actualBalance", equalTo(cardBalance));
        } else {
            response
                    .body("parentManagedAccountId", equalTo(managedCard.getManagedCardModel().getParentManagedAccountId()));
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
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}