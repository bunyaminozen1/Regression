package opc.junit.innovator.managedcards;

import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnblockManagedCardPinTests extends BaseManagedCardsSetup {

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
    public void UnblockPin_PrepaidCorporate_Success() throws SQLException {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsDatabaseHelper.updatePhysicalCardState("PIN_BLOCKED", managedCardId);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String physicalCardState = ManagedCardsDatabaseHelper.getPhysicalManagedCard(managedCardId).get(0).get("physical_state");
        assertEquals("ACTIVE", physicalCardState);
    }

    @Test
    public void UnblockPin_PrepaidConsumer_Success() throws SQLException {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsDatabaseHelper.updatePhysicalCardState("PIN_BLOCKED", managedCardId);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String physicalCardState = ManagedCardsDatabaseHelper.getPhysicalManagedCard(managedCardId).get(0).get("physical_state");
        assertEquals("ACTIVE", physicalCardState);
    }

    @Test
    public void UnblockPin_DebitCorporate_Success() throws SQLException {
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsDatabaseHelper.updatePhysicalCardState("PIN_BLOCKED", managedCardId);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String physicalCardState = ManagedCardsDatabaseHelper.getPhysicalManagedCard(managedCardId).get(0).get("physical_state");
        assertEquals("ACTIVE", physicalCardState);
    }

    @Test
    public void UnblockPin_DebitConsumer_Success() throws SQLException {
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsDatabaseHelper.updatePhysicalCardState("PIN_BLOCKED", managedCardId);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String physicalCardState = ManagedCardsDatabaseHelper.getPhysicalManagedCard(managedCardId).get(0).get("physical_state");
        assertEquals("ACTIVE", physicalCardState);
    }

    @Test
    public void UnblockPin_CorporateUser_Success() throws SQLException {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsDatabaseHelper.updatePhysicalCardState("PIN_BLOCKED", managedCardId);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String physicalCardState = ManagedCardsDatabaseHelper.getPhysicalManagedCard(managedCardId).get(0).get("physical_state");
        assertEquals("ACTIVE", physicalCardState);
    }

    @Test
    public void UnblockPin_CardNotUpgraded_InstrumentNotPhysical(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void UnblockPin_CardUpgradedNotActivated_PhysicalCardNotActivated(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PHYSICAL_CARD_NOT_ACTIVATED"));
    }

    @Test
    public void UnblockPin_CardBlocked_InstrumentBlocked(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_BLOCKED"));
    }

    @Test
    public void UnblockPin_CardDestroyed_InstrumentDestroyed(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void UnblockPin_CorporateUnblockUserCardPin_Success() throws SQLException {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsDatabaseHelper.updatePhysicalCardState("PIN_BLOCKED", managedCardId);

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String physicalCardState = ManagedCardsDatabaseHelper.getPhysicalManagedCard(managedCardId).get(0).get("physical_state");
        assertEquals("ACTIVE", physicalCardState);
    }

    @Test
    public void UnblockPin_UnknownManagedCardId_NotFound() {
        InnovatorService
                .unblockPhysicalCardPin(RandomStringUtils.randomNumeric(18), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UnblockPin_NoManagedCardId_NotFound() {
        InnovatorService
                .unblockPhysicalCardPin("", innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UnblockPin_CrossTenantCheck_NotFound(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        InnovatorService
                .unblockPhysicalCardPin(managedCardId, innovator.getRight())
                .then().statusCode(SC_NOT_FOUND);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }
}
