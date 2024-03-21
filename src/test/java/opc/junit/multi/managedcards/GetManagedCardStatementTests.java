package opc.junit.multi.managedcards;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static opc.enums.opc.IdentityType.CONSUMER;
import static opc.enums.opc.IdentityType.CORPORATE;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class GetManagedCardStatementTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static CreateCorporateModel corporateDetails;
    private static CreateConsumerModel consumerDetails;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccount;
    private static ManagedCardDetails corporateManagedCard;
    private static ManagedCardDetails consumerManagedCard;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup() throws InterruptedException {

        corporateSetup();
        consumerSetup();

        corporateManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateDetails.getBaseCurrency(), corporateAuthenticationToken);
        consumerManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerDetails.getBaseCurrency(), consumerAuthenticationToken);
        corporateManagedAccount =
                transferFundsToCard(corporateAuthenticationToken, CORPORATE,
                        corporateManagedCard.getManagedCardId(), corporateCurrency, 50L, 2);
        consumerManagedAccount =
                transferFundsToCard(consumerAuthenticationToken, CONSUMER,
                        consumerManagedCard.getManagedCardId(), consumerCurrency, 50L, 2);
    }

    @Test
    public void GetManagedCardStatement_Corporate_Success(){
        assertSuccessfulResponse(ManagedCardsService
                        .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK),
                corporateCurrency, corporateManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_Consumer_Success(){
        assertSuccessfulResponse(ManagedCardsService
                .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK),
                consumerCurrency, consumerManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_WithAllFilters_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 2);
        filters.put("orderByTimestamp", "DESC");
        filters.put("fromTimestamp", Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("toTimestamp", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        assertSuccessfulResponse(ManagedCardsService
                        .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK),
                consumerCurrency, consumerManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_CsvAcceptedResponseCheck_Success(){
        // TODO check csv items in response
        ManagedCardsService
                .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.CSV)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardStatement_NoAcceptHeader_DefaultToJsonSuccess(){
        assertSuccessfulResponse(ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), Optional.empty())
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_JsonInAcceptHeader_JsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/plain, */*");

        assertSuccessfulResponse(ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_CsvInAcceptHeader_CsvSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/csv, text/plain, */*");

        // TODO check csv items in response
        ManagedCardsService
                .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardStatement_JsonAndCsvInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/csv");

        assertSuccessfulResponse(ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_NoMatchInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/plain, */*");

        assertSuccessfulResponse(ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount);
    }

    @Test
    public void GetManagedCardStatement_FilterFromMultipleEntries_Success() {

        final Long latestTimestamp =
                ManagedCardsService
                        .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken,
                                Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("entry[0].processedTimestamp");

        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", latestTimestamp);

        assertSingleSuccessfulResponse(ManagedCardsService
                        .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK),
                consumerCurrency, consumerManagedAccount);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ASC", "DESC"})
    public void GetManagedCardStatement_CheckOrdering_Success(final String order){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("orderByTimestamp", order);

        final List<Long> timestamps = new ArrayList<>();

        final JsonPath response = ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath();

        timestamps.add(response.get("entry[0].processedTimestamp"));
        timestamps.add(response.get("entry[1].processedTimestamp"));

        Assertions.assertTrue(order.equals("ASC")  ?
                timestamps.get(0) < timestamps.get(1) :
                timestamps.get(0) > timestamps.get(1));
    }

    @Test
    public void GetManagedCardStatement_NoEntries_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedCardStatement_InvalidApiKey_Unauthorised(){
        ManagedCardsService
                .getManagedCardStatement("abc", corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardStatement_NoApiKey_BadRequest(){
        ManagedCardsService
                .getManagedCardStatement("", corporateManagedCard.getManagedCardId(), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCardStatement_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService
                .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardStatement_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), token, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardStatement_CrossIdentityCheck_NotFound(){
        ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedCardStatement_UnknownManagedCardId_NotFound() {
        ManagedCardsService
                .getManagedCardStatement(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("GetManagedCardStatement_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void GetManagedCardStatement_NoManagedCardId_NotFound() {
        ManagedCardsService
                .getManagedCardStatement(secretKey, "", corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCardStatement_BackofficeCorporateImpersonator_Forbidden(){
        ManagedCardsService
                .getManagedCardStatement(secretKey, corporateManagedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(corporateId, CORPORATE), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardStatement_BackofficeConsumerImpersonator_Forbidden(){
        ManagedCardsService
                .getManagedCardStatement(secretKey, consumerManagedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(consumerId, CONSUMER), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final String currency,
                                          final Pair<String, CreateManagedAccountModel> managedAccount){
        response.body("entry[0].balanceAfter.amount", equalTo(100))
                .body("entry[0].availableBalanceAfter.amount", equalTo(100))
                .body("entry[0].actualBalanceAfter.amount", equalTo(100))
                .body("entry[1].balanceAfter.amount", equalTo(50))
                .body("entry[1].availableBalanceAfter.amount", equalTo(50))
                .body("entry[1].actualBalanceAfter.amount", equalTo(50))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        IntStream.range(0, 2).forEach(i ->
        response.body(String.format("entry[%s].transactionId.id", i), notNullValue())
                .body(String.format("entry[%s].transactionId.type", i), equalTo("TRANSFER"))
                .body(String.format("entry[%s].transactionAmount.currency", i), equalTo(currency))
                .body(String.format("entry[%s].transactionAmount.amount", i), equalTo(50))
                .body(String.format("entry[%s].availableBalanceAdjustment.currency", i), equalTo(currency))
                .body(String.format("entry[%s].availableBalanceAdjustment.amount", i), equalTo(50))
                .body(String.format("entry[%s].actualBalanceAdjustment.currency", i), equalTo(currency))
                .body(String.format("entry[%s].actualBalanceAdjustment.amount", i), equalTo(50))
                .body(String.format("entry[%s].balanceAfter.currency", i), equalTo(currency))
                .body(String.format("entry[%s].cardholderFee.currency", i), equalTo(currency))
                .body(String.format("entry[%s].cardholderFee.amount", i), equalTo(0))
                .body(String.format("entry[%s].processedTimestamp", i), notNullValue())
                .body(String.format("entry[%s].additionalFields.sourceInstrumentType", i), equalTo("managed_accounts"))
                .body(String.format("entry[%s].additionalFields.sourceInstrumentId", i), equalTo(managedAccount.getLeft()))
                .body(String.format("entry[%s].additionalFields.sourceInstrumentFriendlyName", i), equalTo(managedAccount.getRight().getFriendlyName())));
    }

    private void assertSingleSuccessfulResponse(final ValidatableResponse response,
                                                final String currency,
                                                final Pair<String, CreateManagedAccountModel> managedAccount){
        response.body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(currency))
                .body("entry[0].transactionAmount.amount", equalTo(50))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(50))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(50))
                .body("entry[0].balanceAfter.currency", equalTo(currency))
                .body("entry[0].balanceAfter.amount", equalTo(100))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(100))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(100))
                .body("entry[0].cardholderFee.currency", equalTo(currency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.sourceInstrumentType", equalTo("managed_accounts"))
                .body("entry[0].additionalFields.sourceInstrumentId", equalTo(managedAccount.getLeft()))
                .body("entry[0].additionalFields.sourceInstrumentFriendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    private static void consumerSetup() {
        consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = consumerDetails.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}