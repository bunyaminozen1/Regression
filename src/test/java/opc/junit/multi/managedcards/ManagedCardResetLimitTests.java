package opc.junit.multi.managedcards;

import opc.enums.opc.CardBureau;
import commons.enums.Currency;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static opc.enums.opc.IdentityType.CONSUMER;
import static opc.enums.opc.IdentityType.CORPORATE;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class ManagedCardResetLimitTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;

    private static String corporateDigiseqPrepaidManagedCardsProfileId;
    private static String consumerDigiseqPrepaidManagedCardsProfileId;
    private static String corporateDigiseqDebitManagedCardsProfileId;
    private static String consumerDigiseqDebitManagedCardsProfileId;

    @BeforeAll
    public static void IndividualSetup(){
        corporateSetup();
        consumerSetup();

        corporateDigiseqPrepaidManagedCardsProfileId = applicationOne.getCorporateDigiseqEeaPrepaidManagedCardsProfileId();
        consumerDigiseqPrepaidManagedCardsProfileId = applicationOne.getConsumerDigiseqEeaPrepaidManagedCardsProfileId();
        corporateDigiseqDebitManagedCardsProfileId = applicationOne.getCorporateDigiseqEeaDebitManagedCardsProfileId();
        consumerDigiseqDebitManagedCardsProfileId = applicationOne.getConsumerDigiseqEeaDebitManagedCardsProfileId();
    }

    @Test
    public void ResetContactlessLimit_PrepaidCorporate_Success() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporateDigiseqPrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ResetContactlessLimit_PrepaidConsumer_Success() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerDigiseqPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ResetContactlessLimit_DebitCorporate_Success() {
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDigiseqDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ResetContactlessLimit_DebitConsumer_Success() {
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDigiseqDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ResetContactlessLimit_InstrumentNotDigiseq_PhysicalCardTypeNotSupported() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, CardBureau.NITECREST)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PHYSICAL_CARD_TYPE_NOT_SUPPORTED"));
    }

    @Test
    public void ResetContactlessLimit_InstrumentNotPhysical_InstrumentNotPhysical() {
        final String managedCardId =
                createPrepaidManagedCard(consumerDigiseqPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void ResetContactlessLimit_InstrumentUpgradedNotActivated_PhysicalCardNotActivated() {
        final String managedCardId =
                createPhysicalNonActivatedManagedCard(consumerDigiseqPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PHYSICAL_CARD_NOT_ACTIVATED"));
    }

    @Test
    public void ResetContactlessLimit_InstrumentBlocked_InstrumentBlocked() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerDigiseqPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken);

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_BLOCKED"));
    }

    @Test
    public void ResetContactlessLimit_InstrumentDestroyed_InstrumentDestroyed() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporateDigiseqPrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void ResetContactlessLimit_InstrumentLost_InstrumentBlocked() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporateDigiseqPrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_BLOCKED"));
    }

    @Test
    public void ResetContactlessLimit_InstrumentStolen_InstrumentDestroyed() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporateDigiseqPrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsHelper.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void ResetContactlessLimit_UnknownManagedCardId_NotFound() {
        ManagedCardsService.resetContactlessLimit(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ResetContactlessLimit_NoManagedCardId_NotFound() {
        ManagedCardsService.resetContactlessLimit(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ResetContactlessLimit_CrossIdentityCheck_NotFound() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporateDigiseqPrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ResetContactlessLimit_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService.resetContactlessLimit(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ResetContactlessLimit_InvalidApiKey_Unauthorised(){
        ManagedCardsService.resetContactlessLimit("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ResetContactlessLimit_NoApiKey_BadRequest(){
        ManagedCardsService.resetContactlessLimit("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ResetContactlessLimit_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService.resetContactlessLimit(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ResetContactlessLimit_BackofficeCorporateImpersonator_Forbidden() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporateDigiseqPrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ResetContactlessLimit_BackofficeConsumerImpersonator_Forbidden() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerDigiseqPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken, CardBureau.DIGISEQ)
                        .getManagedCardId();

        ManagedCardsService.resetContactlessLimit(secretKey, managedCardId, getBackofficeImpersonateToken(consumerId, CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.USD.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
