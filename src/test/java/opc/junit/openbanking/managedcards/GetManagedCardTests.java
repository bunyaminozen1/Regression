package opc.junit.openbanking.managedcards;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static opc.enums.opc.CardLevelClassification.CONSUMER;
import static opc.enums.opc.CardLevelClassification.CORPORATE;
import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedCardTests extends BaseSetup {

    private static String corporateConsent;
    private static String consumerConsent;
    private static Map<String, String> corporateHeaders;
    private static Map<String, String> consumerHeaders;

    private static List<ManagedCardDetails> corporatePrepaidManagedCards;
    private static List<ManagedCardDetails> consumerPrepaidManagedCards;
    private static List<ManagedCardDetails> corporateDebitManagedCards;
    private static List<ManagedCardDetails> consumerDebitManagedCards;

    private static PhysicalCardAddressModel corporateDeliveryAddress;
    private static PhysicalCardAddressModel consumerDeliveryAddress;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {

        corporateSetup();
        consumerSetup();

        corporatePrepaidManagedCards =
                ManagedCardsHelper
                        .createPrepaidManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, secretKey, corporateAuthenticationToken, 2);

        consumerPrepaidManagedCards =
                ManagedCardsHelper
                        .createPrepaidManagedCards(consumerPrepaidManagedCardsProfileId, consumerCurrency, secretKey, consumerAuthenticationToken, 2);

        final String corporateManagedAccount = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                corporateCurrency, secretKey, corporateAuthenticationToken);
        final String consumerManagedAccount = ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                consumerCurrency, secretKey, consumerAuthenticationToken);

        corporateDebitManagedCards =
                ManagedCardsHelper
                        .createDebitManagedCards(corporateDebitManagedCardsProfileId, corporateManagedAccount, secretKey, corporateAuthenticationToken, 2);

        consumerDebitManagedCards =
                ManagedCardsHelper
                        .createDebitManagedCards(consumerDebitManagedCardsProfileId, consumerManagedAccount, secretKey, consumerAuthenticationToken, 2);

        corporateDeliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        consumerDeliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

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

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_PrepaidCorporate_Success(final InstrumentType instrumentType) {

        final ManagedCardDetails managedCard =
                corporatePrepaidManagedCards.get(0);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                    corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final ValidatableResponse response =
                AccountInformationService.getManagedCard(sharedKey, corporateHeaders, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(corporateCurrency))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertSuccessfulResponse(response, managedCard,
                CORPORATE, instrumentType, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_DebitCorporate_Success(final InstrumentType instrumentType) {

        final ManagedCardDetails managedCard =
                corporateDebitManagedCards.get(1);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                    corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final ValidatableResponse response =
                AccountInformationService.getManagedCard(sharedKey, corporateHeaders, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedCard.getManagedCardModel().getParentManagedAccountId()));

        assertSuccessfulResponse(response, managedCard,
                CORPORATE, instrumentType, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_PrepaidConsumer_Success(final InstrumentType instrumentType) {

        final ManagedCardDetails managedCard =
                consumerPrepaidManagedCards.get(0);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                    consumerAuthenticationToken, consumerDeliveryAddress);
        }

        final ValidatableResponse response =
                AccountInformationService.getManagedCard(sharedKey, consumerHeaders, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(consumerCurrency))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertSuccessfulResponse(response, managedCard,
                CONSUMER, instrumentType, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_DebitConsumer_Success(final InstrumentType instrumentType) {

        final ManagedCardDetails managedCard =
                consumerDebitManagedCards.get(1);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                    consumerAuthenticationToken, consumerDeliveryAddress);
        }

        final ValidatableResponse response =
                AccountInformationService.getManagedCard(sharedKey, consumerHeaders, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedCard.getManagedCardModel().getParentManagedAccountId()));

        assertSuccessfulResponse(response, managedCard,
                CONSUMER, instrumentType, instrumentType.equals(PHYSICAL));
    }

    @Test
    public void GetManagedCard_CardUpgradedNotActivated_Success(){

        final ManagedCardDetails managedCard =
                consumerPrepaidManagedCards.get(1);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(),
                consumerAuthenticationToken, consumerDeliveryAddress);

        final ValidatableResponse response =
                AccountInformationService.getManagedCard(sharedKey, consumerHeaders, managedCard.getManagedCardId())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedCard.getManagedCardModel().getParentManagedAccountId()));

        assertSuccessfulResponse(response, managedCard,
                CONSUMER, VIRTUAL, true);
    }

    @Test
    public void GetManagedCard_UnknownManagedCardId_NotFound(){

        AccountInformationService.getManagedCard(sharedKey, consumerHeaders, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedCard_InvalidSharedKey_Unauthorised(){

        AccountInformationService.getManagedCard("abc", consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCard_NoSharedKey_BadRequest(){

        AccountInformationService.getManagedCard("", consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCard_DifferentInnovatorSharedKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("sharedKey");

        AccountInformationService.getManagedCard(sharedKey, consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCard_RootUserLoggedOut_Unauthorised() throws Exception {

        final Pair<String, Map<String, String>> newCorporate = createCorporateWithConsentHeaders();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardsProfileId,
                        Currency.getRandomCurrency().name(), secretKey, newCorporate.getLeft());

        OpenBankingSecureServiceHelper.logout(sharedKey, newCorporate.getLeft(), tppId);

        AccountInformationService.getManagedCard(sharedKey, corporateHeaders, managedCardId)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCard_CrossIdentityChecks_NotFound(){

        AccountInformationService.getManagedCard(sharedKey, corporateHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final ManagedCardDetails managedCard,
                                          final CardLevelClassification cardLevelClassification,
                                          final InstrumentType instrumentType,
                                          final boolean isPhysical) {
        response.body("id", equalTo(managedCard.getManagedCardId()))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(managedCard.getManagedCardModel().getTag()))
                .body("friendlyName", equalTo(managedCard.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo(State.ACTIVE.name()))
                .body("type", equalTo(instrumentType.name()))
                .body("cardBrand", equalTo(CardBrand.MASTERCARD.name()))
                .body("cardLevelClassification", equalTo(cardLevelClassification.name()))
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
                .body("digitalWallets.walletsEnabled", equalTo(false))
                .body("digitalWallets.artworkReference", equalTo(null));

        if (isPhysical) {
            assertPhysicalCardDetails(response,
                    cardLevelClassification.equals(CORPORATE) ? corporateDeliveryAddress : consumerDeliveryAddress);
        }

        if (managedCard.getManagedCardModel().getMode().equals(DEBIT_MODE.name())) {
            response.body("availableToSpend[0].interval", equalTo("ALWAYS"))
                    .body("availableToSpend[0].value.amount", equalTo(0));
        }
    }

    private void assertPhysicalCardDetails(final ValidatableResponse response,
                                           final PhysicalCardAddressModel physicalCardAddressModel) {
        response
                .body("physicalCardDetails.productReference", notNullValue())
                .body("physicalCardDetails.carrierType", notNullValue())
                .body("physicalCardDetails.pendingActivation", equalTo(false))
                .body("physicalCardDetails.pinBlocked", equalTo(false))
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                .body("physicalCardDetails.deliveryMethod", equalTo("STANDARD_DELIVERY"))
                .body("physicalCardDetails.deliveryAddress.name", equalTo(physicalCardAddressModel.getName()))
                .body("physicalCardDetails.deliveryAddress.surname", equalTo(physicalCardAddressModel.getSurname()))
                .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(physicalCardAddressModel.getAddressLine1()))
                .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(physicalCardAddressModel.getAddressLine2()))
                .body("physicalCardDetails.deliveryAddress.city", equalTo(physicalCardAddressModel.getCity()))
                .body("physicalCardDetails.deliveryAddress.postCode", equalTo(physicalCardAddressModel.getPostCode()))
                .body("physicalCardDetails.deliveryAddress.state", equalTo(physicalCardAddressModel.getState()))
                .body("physicalCardDetails.deliveryAddress.country", equalTo(physicalCardAddressModel.getCountry()))
                .body("physicalCardDetails.manufacturingState", equalTo(ManufacturingState.DELIVERED.name()));
    }

    protected static List<InstrumentType> getInstrumentTypes(){
        return Arrays.asList(VIRTUAL, PHYSICAL);
    }
}
