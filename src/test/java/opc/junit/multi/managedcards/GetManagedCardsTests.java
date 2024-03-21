package opc.junit.multi.managedcards;

import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class GetManagedCardsTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static List<ManagedCardDetails> corporateManagedCards;
    private static List<ManagedCardDetails> consumerManagedCards;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();

        final String corporateManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, corporateCurrency).build(),
                        secretKey, corporateAuthenticationToken);
        final String consumerManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, consumerCurrency).build(),
                        secretKey, consumerAuthenticationToken);

        corporateManagedCards =
                createVirtualAndPhysicalCards(corporatePrepaidManagedCardsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateManagedAccountId, corporateAuthenticationToken);

        consumerManagedCards =
                createVirtualAndPhysicalCards(consumerPrepaidManagedCardsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerManagedAccountId, consumerAuthenticationToken);

        Collections.reverse(corporateManagedCards);
        Collections.reverse(consumerManagedCards);
    }

    @Test
    public void GetManagedCards_Corporate_Success(){

        final ValidatableResponse response = ManagedCardsService.getManagedCards(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulCorporateResponse(response);

        response.body("count", equalTo(corporateManagedCards.size()))
                .body("responseCount", equalTo(corporateManagedCards.size()));
    }

    @Test
    public void GetManagedCards_Consumer_Success(){
        final ValidatableResponse response = ManagedCardsService.getManagedCards(secretKey, Optional.empty(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulConsumerResponse(response);

        response.body("count", equalTo(consumerManagedCards.size()))
                .body("responseCount", equalTo(consumerManagedCards.size()));
    }

    @Test
    public void GetManagedCards_WithFilters_Success(){
        final ManagedCardDetails managedCard =
                corporateManagedCards.stream().filter(x -> x.getInstrumentType().equals(VIRTUAL) && x.getManagedCardMode().equals(PREPAID_MODE))
                        .findFirst().orElseThrow();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("profileId", managedCard.getManagedCardModel().getProfileId());
        filters.put("friendlyName", managedCard.getManagedCardModel().getFriendlyName());
        filters.put("state", Collections.singletonList(State.ACTIVE));
        filters.put("currency", managedCard.getManagedCardModel().getCurrency());
        filters.put("type", InstrumentType.VIRTUAL.name());
        filters.put("createdFrom", Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("tag", managedCard.getManagedCardModel().getTag());
        filters.put("mode", PREPAID_MODE.name());

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCard.getManagedCardId()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_LimitFilterCheck_Success(){

        final ManagedCardDetails managedCard = corporateManagedCards.get(0);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCard.getManagedCardId()))
                .body("count", equalTo(corporateManagedCards.size()))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_FilterByTypeAndMode_Success(){
        final ManagedCardDetails managedCard =
                corporateManagedCards.stream().filter(x -> x.getInstrumentType().equals(PHYSICAL) && x.getManagedCardMode().equals(DEBIT_MODE))
                        .findFirst().orElseThrow();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("type", PHYSICAL.name());
        filters.put("mode", DEBIT_MODE.name());

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCard.getManagedCardId()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_FilterByState_NoEntries(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.BLOCKED));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedCards_FilterByMode_NoEntries(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("mode", DEBIT_MODE.name());

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void GetManagedCards_NoEntries_NoEntries(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("friendlyName", RandomStringUtils.randomAlphabetic(10));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedCards_FilterByDifferentStates_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final List<ManagedCardDetails> managedCards = new ArrayList<>();
        IntStream.range(0, 3).forEach(i ->
                managedCards.add(createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), corporate.getRight())));

        ManagedCardsHelper.blockManagedCard(secretKey, managedCards.get(0).getManagedCardId(), corporate.getRight());
        ManagedCardsHelper.removeManagedCard(secretKey, managedCards.get(1).getManagedCardId(), corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Arrays.asList(State.ACTIVE, State.BLOCKED, State.DESTROYED));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));
        filters.put("state.destroyedReason", Collections.singletonList(DestroyedReason.USER));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(managedCards.size()))
                .body("responseCount", equalTo(managedCards.size()));
    }

    @Test
    public void GetManagedCards_FilterByDifferentNonActiveReasons_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final List<ManagedCardDetails> managedCards = new ArrayList<>();
        IntStream.range(0, 3).forEach(i ->
                managedCards.add(createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), corporate.getRight())));

        ManagedCardsHelper.reportLostCard(secretKey, managedCards.get(0).getManagedCardId(), corporate.getRight());
        ManagedCardsHelper.reportStolenCard(secretKey, managedCards.get(1).getManagedCardId(), corporate.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Arrays.asList(State.BLOCKED, State.DESTROYED));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.LOST));
        filters.put("state.destroyedReason", Collections.singletonList(DestroyedReason.STOLEN));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names =  { "ACTIVE", "DESTROYED" })
    public void GetManagedCards_FilterByBlockReasonAndWrongState_BadRequest(final State state){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names =  { "ACTIVE", "BLOCKED" })
    public void GetManagedCards_FilterByDestroyedReasonAndWrongState_BadRequest(final State state){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.destroyedReason", Collections.singletonList(DestroyedReason.USER));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCards_CardBlocked_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), authenticatedCorporate.getRight());

        ManagedCardsService.getManagedCards(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.DESTROYED))),
                authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.BLOCKED))),
                authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCard.getManagedCardId()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @ParameterizedTest
    @EnumSource(BlockType.class)
    public void GetManagedCards_CardBlockedByAdmin_Success(final BlockType  blockType){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        AdminHelper.blockManagedCard(managedCard.getManagedCardId(), blockType, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

        ManagedCardsService.getManagedCards(secretKey, Optional.empty(),
                        authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("cards[0].state.state", equalTo("BLOCKED"))
                .body("cards[0].state.blockedReason", equalTo(blockType.equals(BlockType.USER) ? "USER" : "SYSTEM"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @ParameterizedTest
    @EnumSource(BlockType.class)
    public void GetManagedCards_CardBlockedByAdminFilterByState_Success(final BlockType  blockType){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        AdminHelper.blockManagedCard(managedCard.getManagedCardId(), blockType, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", List.of(State.BLOCKED));
        filters.put("state.blockedReason", Collections.singletonList(blockType));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(filters),
                        authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("cards[0].state.state", equalTo("BLOCKED"))
                .body("cards[0].state.blockedReason", equalTo(blockType.equals(BlockType.USER) ? "USER" : "SYSTEM"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_CardDestroyed_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), authenticatedCorporate.getRight());

        ManagedCardsService.getManagedCards(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.BLOCKED))),
                authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.DESTROYED))),
                authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCard.getManagedCardId()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_CardBlockedAndDestroyed_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), authenticatedCorporate.getRight());
        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), authenticatedCorporate.getRight());

        ManagedCardsService.getManagedCards(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.BLOCKED))),
                authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        ManagedCardsService.getManagedCards(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.DESTROYED))),
                authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCard.getManagedCardId()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_InvalidApiKey_Unauthorised(){

        ManagedCardsService.getManagedCards("abc", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCards_NoApiKey_BadRequest(){

        ManagedCardsService.getManagedCards("", Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCards_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService.getManagedCards(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCards_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService.getManagedCards(secretKey, Optional.empty(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCards_BackofficeCorporateImpersonator_Forbidden(){
        ManagedCardsService.getManagedCards(secretKey, Optional.empty(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCards_BackofficeConsumerImpersonator_Forbidden(){
        ManagedCardsService.getManagedCards(secretKey, Optional.empty(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void assertSuccessfulCorporateResponse(final ValidatableResponse response){

        for (int i = 0; i < corporateManagedCards.size(); i++) {
            if (corporateManagedCards.get(i).getManagedCardMode().equals(PREPAID_MODE)) {
                response
                        .body(String.format("cards[%s].profileId", i), equalTo(corporatePrepaidManagedCardsProfileId))
                        .body(String.format("cards[%s].balances.availableBalance", i), equalTo(0))
                        .body(String.format("cards[%s].balances.actualBalance", i), equalTo(0));
            }else {
                response
                        .body(String.format("cards[%s].profileId", i), equalTo(corporateDebitManagedCardsProfileId))
                        .body(String.format("cards[%s].parentManagedAccountId", i),
                                equalTo(corporateManagedCards.get(i).getManagedCardModel().getParentManagedAccountId()));
            }

            response
                    .body(String.format("cards[%s].currency", i), equalTo(corporateCurrency))
                    .body(String.format("cards[%s].cardLevelClassification", i), equalTo(CardLevelClassification.CORPORATE.name()));

            assertCommonDetails(response, i, corporateManagedCards.get(i), corporateCurrency);
        }
    }

    private void assertSuccessfulConsumerResponse(final ValidatableResponse response){

        for (int i = 0; i < consumerManagedCards.size(); i++) {
            if (consumerManagedCards.get(i).getManagedCardMode().equals(PREPAID_MODE)) {
                response
                        .body(String.format("cards[%s].profileId", i), equalTo(consumerPrepaidManagedCardsProfileId))
                        .body(String.format("cards[%s].balances.availableBalance", i), equalTo(0))
                        .body(String.format("cards[%s].balances.actualBalance", i), equalTo(0));
            }else {
                response
                        .body(String.format("cards[%s].profileId", i), equalTo(consumerDebitManagedCardsProfileId))
                        .body(String.format("cards[%s].parentManagedAccountId", i),
                                equalTo(consumerManagedCards.get(i).getManagedCardModel().getParentManagedAccountId()));
            }

            response
                    .body(String.format("cards[%s].currency", i), equalTo(consumerCurrency))
                    .body(String.format("cards[%s].cardLevelClassification", i), equalTo(CardLevelClassification.CONSUMER.name()));

            assertCommonDetails(response, i, consumerManagedCards.get(i), consumerCurrency);
        }
    }

    private void assertCommonDetails(final ValidatableResponse response,
                                     final int itemNumber,
                                     final ManagedCardDetails managedCardDetails,
                                     final String currency){
        response
                .body(String.format("cards[%s].id", itemNumber), equalTo(managedCardDetails.getManagedCardId()))
                .body(String.format("cards[%s].externalHandle", itemNumber), notNullValue())
                .body(String.format("cards[%s].tag", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getTag()))
                .body(String.format("cards[%s].friendlyName", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getFriendlyName()))
                .body(String.format("cards[%s].state.state", itemNumber), equalTo(State.ACTIVE.name()))
                .body(String.format("cards[%s].type", itemNumber), equalTo(managedCardDetails.getInstrumentType().name()))
                .body(String.format("cards[%s].cardBrand", itemNumber), equalTo(CardBrand.MASTERCARD.name()))
                .body(String.format("cards[%s].cardNumber.value", itemNumber), notNullValue())
                .body(String.format("cards[%s].cvv.value", itemNumber), notNullValue())
                .body(String.format("cards[%s].cardNumberFirstSix", itemNumber), notNullValue())
                .body(String.format("cards[%s].cardNumberLastFour", itemNumber), notNullValue())
                .body(String.format("cards[%s].nameOnCard", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getNameOnCard()))
                .body(String.format("cards[%s].startMmyy", itemNumber), equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body(String.format("cards[%s].expiryMmyy", itemNumber), equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
                .body(String.format("cards[%s].expiryPeriodMonths", itemNumber), equalTo(36))
                .body(String.format("cards[%s].renewalType", itemNumber), equalTo("NO_RENEW"))
                .body(String.format("cards[%s].creationTimestamp", itemNumber), notNullValue())
                .body(String.format("cards[%s].cardholderMobileNumber", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getCardholderMobileNumber()))
                .body(String.format("cards[%s].billingAddress.addressLine1", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getAddressLine1()))
                .body(String.format("cards[%s].billingAddress.addressLine2", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getAddressLine2()))
                .body(String.format("cards[%s].billingAddress.city", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getCity()))
                .body(String.format("cards[%s].billingAddress.postCode", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getPostCode()))
                .body(String.format("cards[%s].billingAddress.state", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getState()))
                .body(String.format("cards[%s].billingAddress.country", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getCountry()))
                .body(String.format("cards[%s].mode", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getMode()))
                .body(String.format("cards[%s].digitalWallets.walletsEnabled", itemNumber), equalTo(false))
                .body(String.format("cards[%s].digitalWallets.artworkReference", itemNumber), equalTo(null));

        if(managedCardDetails.getManufacturingState() != null){
            assertPhysicalCardDetails(response, itemNumber, managedCardDetails);
        }

        if(managedCardDetails.getManagedCardMode() == DEBIT_MODE){
            response.body(String.format("cards[%s].availableToSpend[0].interval", itemNumber), equalTo("ALWAYS"))
                    .body(String.format("cards[%s].availableToSpend[0].value.amount", itemNumber), equalTo(0))
                    .body(String.format("cards[%s].availableToSpend[0].value.currency", itemNumber), equalTo(currency));
        }
    }

    private void assertPhysicalCardDetails(final ValidatableResponse response, final int itemNumber,
                                           final ManagedCardDetails managedCardDetails){
        response
                .body(String.format("cards[%s].physicalCardDetails.productReference", itemNumber), notNullValue())
                .body(String.format("cards[%s].physicalCardDetails.carrierType", itemNumber), notNullValue())
                .body(String.format("cards[%s].physicalCardDetails.pendingActivation", itemNumber), equalTo(managedCardDetails.getManufacturingState().equals(ManufacturingState.REQUESTED)))
                .body(String.format("cards[%s].physicalCardDetails.pinBlocked", itemNumber), equalTo(false))
                .body(String.format("cards[%s].physicalCardDetails.replacement.replacementId", itemNumber), equalTo("0"))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.name", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getName()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.surname", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getSurname()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.addressLine1", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getAddressLine1()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.addressLine2", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getAddressLine2()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.city", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getCity()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.postCode", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getPostCode()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.state", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getState()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryAddress.country", itemNumber), equalTo(managedCardDetails.getPhysicalCardAddressModel().getCountry()))
                .body(String.format("cards[%s].physicalCardDetails.deliveryMethod", itemNumber), equalTo("STANDARD_DELIVERY"))
                .body(String.format("cards[%s].physicalCardDetails.manufacturingState", itemNumber), equalTo(managedCardDetails.getManufacturingState().name()));
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
