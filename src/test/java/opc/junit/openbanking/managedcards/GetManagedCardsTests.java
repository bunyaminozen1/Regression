package opc.junit.openbanking.managedcards;

import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.openbanking.AccountInformationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedCardsTests extends BaseSetup {

    private static String corporateConsent;
    private static String consumerConsent;
    private static Map<String, String> corporateHeaders;
    private static Map<String, String> consumerHeaders;

    private static List<ManagedCardDetails> corporateManagedCards;
    private static List<ManagedCardDetails> consumerManagedCards;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {

        corporateSetup();
        consumerSetup();

        final String corporateManagedAccount = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                corporateCurrency, secretKey, corporateAuthenticationToken);
        final String consumerManagedAccount = ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                consumerCurrency, secretKey, consumerAuthenticationToken);

        corporateManagedCards =
                ManagedCardsHelper
                        .createVirtualAndPhysicalCards(corporatePrepaidManagedCardsProfileId,
                                corporateDebitManagedCardsProfileId, corporateCurrency, corporateManagedAccount, corporateAuthenticationToken, secretKey);

        consumerManagedCards =
                ManagedCardsHelper
                        .createVirtualAndPhysicalCards(consumerPrepaidManagedCardsProfileId,
                                consumerDebitManagedCardsProfileId, consumerCurrency, consumerManagedAccount, consumerAuthenticationToken, secretKey);

        Collections.reverse(corporateManagedCards);
        Collections.reverse(consumerManagedCards);

        corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);
    }

    @BeforeEach
    public void Setup() throws Exception {
        corporateHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(corporateConsent)));
        consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsent)));
    }

    @Test
    public void GetManagedCards_Corporate_Success(){

        final ValidatableResponse response = AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.empty())
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(response, corporateManagedCards, corporateCurrency, CardLevelClassification.CORPORATE);

        response.body("count", equalTo(corporateManagedCards.size()))
                .body("responseCount", equalTo(corporateManagedCards.size()));
    }

    @Test
    public void GetManagedCards_Consumer_Success(){
        final ValidatableResponse response = AccountInformationService.getManagedCards(sharedKey, consumerHeaders, Optional.empty())
                .then()
                .statusCode(SC_OK);

        assertSuccessfulResponse(response, consumerManagedCards, consumerCurrency, CardLevelClassification.CONSUMER);

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

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
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

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
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

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
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

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedCards_FilterByMode_NoEntries(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("mode", DEBIT_MODE.name());

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void GetManagedCards_NoEntries_NoEntries(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("friendlyName", RandomStringUtils.randomAlphabetic(10));

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedCards_FilterByDifferentStates_Success() throws Exception {

        final Pair<String, Map<String, String>> corporate = createCorporateWithConsentHeaders();

        final List<ManagedCardDetails> managedCards =
                ManagedCardsHelper.createPrepaidManagedCards(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), secretKey, corporate.getLeft(), 3);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCards.get(0).getManagedCardId(), corporate.getLeft());
        ManagedCardsHelper.removeManagedCard(secretKey, managedCards.get(1).getManagedCardId(), corporate.getLeft());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Arrays.asList(State.ACTIVE, State.BLOCKED, State.DESTROYED));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.USER));
        filters.put("state.destroyedReason", Collections.singletonList(DestroyedReason.USER));

        AccountInformationService.getManagedCards(sharedKey, corporate.getRight(), Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(managedCards.size()))
                .body("responseCount", equalTo(managedCards.size()));
    }

    @Test
    public void GetManagedCards_FilterByDifferentNonActiveReasons_Success() throws Exception {

        final Pair<String, Map<String, String>> corporate = createCorporateWithConsentHeaders();

        final List<ManagedCardDetails> managedCards =
                ManagedCardsHelper.createPhysicalPrepaidManagedCards(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), secretKey, corporate.getLeft(), 3);

        ManagedCardsHelper.reportLostCard(secretKey, managedCards.get(0).getManagedCardId(), corporate.getLeft());
        ManagedCardsHelper.reportStolenCard(secretKey, managedCards.get(1).getManagedCardId(), corporate.getLeft());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Arrays.asList(State.BLOCKED, State.DESTROYED));
        filters.put("state.blockedReason", Collections.singletonList(BlockedReason.LOST));
        filters.put("state.destroyedReason", Collections.singletonList(DestroyedReason.STOLEN));

        AccountInformationService.getManagedCards(sharedKey, corporate.getRight(), Optional.of(filters))
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

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names =  { "ACTIVE", "BLOCKED" })
    public void GetManagedCards_FilterByDestroyedReasonAndWrongState_BadRequest(final State state){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(state));
        filters.put("state.destroyedReason", Collections.singletonList(DestroyedReason.USER));

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(filters))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCards_CardBlocked_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), secretKey, authenticatedCorporate.getRight());

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, authenticatedCorporate.getRight());

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.DESTROYED))))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.BLOCKED))))
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCardId))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_CardDestroyed_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), secretKey, authenticatedCorporate.getRight());

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, authenticatedCorporate.getRight());

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.BLOCKED))))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.DESTROYED))))
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCardId))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_CardBlockedAndDestroyed_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), secretKey, authenticatedCorporate.getRight());

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, authenticatedCorporate.getRight());
        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, authenticatedCorporate.getRight());

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.BLOCKED))))
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.DESTROYED))))
                .then()
                .statusCode(SC_OK)
                .body("cards[0].id", equalTo(managedCardId))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCards_InvalidSharedKey_Unauthorised(){

        AccountInformationService.getManagedCards("abc", corporateHeaders, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCards_NoSharedKey_BadRequest(){

        AccountInformationService.getManagedCards("", corporateHeaders, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCards_DifferentInnovatorSharedKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("sharedKey");

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCards_DifferentProgrammeSharedKey_Forbidden(){

        AccountInformationService.getManagedCards(applicationTwo.getSharedKey(), corporateHeaders, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCards_RootUserLoggedOut_Unauthorised() throws Exception {

        final Pair<String, Map<String, String>> newCorporate = createCorporateWithConsentHeaders();

        ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardsProfileId, Currency.getRandomCurrency().name(), secretKey, newCorporate.getLeft());

        OpenBankingSecureServiceHelper.logout(sharedKey, newCorporate.getLeft(), tppId);

        AccountInformationService.getManagedCards(sharedKey, corporateHeaders, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final List<ManagedCardDetails> managedCards,
                                          final String currency,
                                          final CardLevelClassification cardLevelClassification){

        for (int i = 0; i < managedCards.size(); i++) {

            if (managedCards.get(i).getManagedCardMode().equals(PREPAID_MODE)) {
                response
                        .body(String.format("cards[%s].balances.availableBalance", i), equalTo(0))
                        .body(String.format("cards[%s].balances.actualBalance", i), equalTo(0));
            }else {
                response
                        .body(String.format("cards[%s].parentManagedAccountId", i),
                                equalTo(managedCards.get(i).getManagedCardModel().getParentManagedAccountId()));
            }

            response
                    .body(String.format("cards[%s].currency", i), equalTo(currency))
                    .body(String.format("cards[%s].cardLevelClassification", i), equalTo(cardLevelClassification.name()));

            assertCommonDetails(response, i, managedCards.get(i));
        }
    }

    private void assertCommonDetails(final ValidatableResponse response,
                                     final int itemNumber,
                                     final ManagedCardDetails managedCardDetails){
        response
                .body(String.format("cards[%s].id", itemNumber), equalTo(managedCardDetails.getManagedCardId()))
                .body(String.format("cards[%s].externalHandle", itemNumber), notNullValue())
                .body(String.format("cards[%s].tag", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getTag()))
                .body(String.format("cards[%s].friendlyName", itemNumber), equalTo(managedCardDetails.getManagedCardModel().getFriendlyName()))
                .body(String.format("cards[%s].state.state", itemNumber), equalTo(State.ACTIVE.name()))
                .body(String.format("cards[%s].type", itemNumber), equalTo(managedCardDetails.getInstrumentType().name()))
                .body(String.format("cards[%s].cardBrand", itemNumber), equalTo(CardBrand.MASTERCARD.name()))
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
                    .body(String.format("cards[%s].availableToSpend[0].value.amount", itemNumber), equalTo(0));
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
}
