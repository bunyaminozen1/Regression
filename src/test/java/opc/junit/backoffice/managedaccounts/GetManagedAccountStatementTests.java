package opc.junit.backoffice.managedaccounts;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedAccountStatementTests extends BaseManagedAccountsSetup{
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccount;
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
        deposit(corporateManagedAccount.getLeft(), corporateManagedAccount.getRight().getCurrency(), corporateAuthenticationToken);
        deposit(consumerManagedAccount.getLeft(), consumerManagedAccount.getRight().getCurrency(), consumerAuthenticationToken);
        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
//        consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(consumerId, IdentityType.CONSUMER, secretKey);
    }

    @Test
    public void GetManagedAccountStatement_Corporate_Success(){
        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_Consumer_Success(){
        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency);
    }

    @Test
    public void GetManagedAccountStatement_WithAllFilters_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 2);
        filters.put("orderByTimestamp", "DESC");
        filters.put("fromTimestamp", Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("toTimestamp", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("showFundMovementsOnly", false);

        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency);
    }

    @Test
    public void GetManagedAccountStatement_CsvAcceptedResponseCheck_Success(){
        // TODO check csv items in response
        BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.empty(), AcceptedResponse.CSV)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_NoAcceptHeader_DefaultToJsonSuccess(){
        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), Optional.empty())
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_JsonInAcceptHeader_JsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/plain, */*");

        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_CsvInAcceptHeader_CsvSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/csv, text/plain, */*");

        // TODO check csv items in response
        BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_JsonAndCsvInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/csv");

        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_NoMatchInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/plain, */*");

        assertSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_FilterFromMultipleEntries_Success() {

        final Long latestTimestamp =
                BackofficeMultiService
                        .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken,
                                Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("entry[0].processedTimestamp");

        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", latestTimestamp);

        assertSingleSuccessfulResponse(BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ASC", "DESC"})
    public void GetManagedAccountStatement_CheckOrdering_Success(final String order){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("orderByTimestamp", order);

        final List<Long> timestamps = new ArrayList<>();

        final JsonPath response = BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.of(filters), AcceptedResponse.JSON)
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
    public void GetManagedAccountStatement_NoEntries_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccountStatement_AccountNotActive_Success(){

        final String managedAccountId =
                createPendingApprovalManagedAccount(consumerManagedAccountProfileId, consumerAuthenticationToken).getLeft();

        BackofficeMultiService
                .getManagedAccountStatement(secretKey, managedAccountId, consumerImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_InvalidApiKey_Unauthorised(){
        BackofficeMultiService
                .getManagedAccountStatement("abc", corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountStatement_NoApiKey_BadRequest(){
        BackofficeMultiService
                .getManagedAccountStatement("", corporateManagedAccount.getLeft(), corporateImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetManagedAccountStatement_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_FORBIDDEN);
    }


    @Test
    public void GetManagedAccountStatement_CrossIdentityCheck_NotFound(){
        BackofficeMultiService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), consumerImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccountStatement_InvalidManagedAccountId_BadRequest(){
        BackofficeMultiService
                .getManagedAccountStatement(secretKey, RandomStringUtils.randomAlphanumeric(50), corporateImpersonateToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error",equalTo("REGEX"))
                .body("validationErrors[0].fieldName",equalTo("id"));
    }

    @Test
    public void GetManagedAccountStatement_PdfAcceptedResponseCheck_Success(){

        BackofficeMultiService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerImpersonateToken, Optional.empty(), AcceptedResponse.PDF)
                .then()
                .statusCode(SC_OK);
    }

    private static void deposit(final String managedAccountId, final String currency, final String token){
        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency,
                1000L, secretKey, token, 2);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final String currency){

        final int depositFeeAmount = TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        response.body("entry[0].balanceAfter.amount", equalTo((1000 - depositFeeAmount) * 2))
                .body("entry[0].availableBalanceAfter.amount", equalTo((1000 - depositFeeAmount) * 2))
                .body("entry[0].actualBalanceAfter.amount", equalTo((1000 - depositFeeAmount) * 2))
                .body("entry[1].balanceAfter.amount", equalTo((1000 - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount",  equalTo((1000 - depositFeeAmount)))
                .body("entry[1].actualBalanceAfter.amount",  equalTo((1000 - depositFeeAmount)))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));


        IntStream.range(0, 2).forEach(i ->
                response.body(String.format("entry[%s].transactionId.id", i), notNullValue())
                        .body(String.format("entry[%s].transactionId.type", i), equalTo("DEPOSIT"))
                        .body(String.format("entry[%s].transactionAmount.currency", i), equalTo(currency))
                        .body(String.format("entry[%s].transactionAmount.amount", i), equalTo((1000)))
                        .body(String.format("entry[%s].availableBalanceAdjustment.currency", i), equalTo(currency))
                        .body(String.format("entry[%s].availableBalanceAdjustment.amount", i), equalTo((1000 - depositFeeAmount)))
                        .body(String.format("entry[%s].actualBalanceAdjustment.currency", i), equalTo(currency))
                        .body(String.format("entry[%s].actualBalanceAdjustment.amount", i), equalTo((1000 - depositFeeAmount)))
                        .body(String.format("entry[%s].balanceAfter.currency", i), equalTo(currency))
                        .body(String.format("entry[%s].cardholderFee.currency", i), equalTo(currency))
                        .body(String.format("entry[%s].cardholderFee.amount", i), equalTo(TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                        .body(String.format("entry[%s].transactionFee.currency", i), equalTo(currency))
                        .body(String.format("entry[%s].transactionFee.amount", i), equalTo(TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                        .body(String.format("entry[%s].processedTimestamp", i), notNullValue()));
    }

    private void assertSingleSuccessfulResponse(final ValidatableResponse response,
                                                final String currency){
        final int depositFeeAmount = TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        response.body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[0].transactionAmount.currency", equalTo(currency))
                .body("entry[0].transactionAmount.amount", equalTo((1000)))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(1000 - depositFeeAmount))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(1000 - depositFeeAmount))
                .body("entry[0].balanceAfter.currency", equalTo(currency))
                .body("entry[0].balanceAfter.amount", equalTo((1000 - depositFeeAmount) * 2))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency))
                .body("entry[0].availableBalanceAfter.amount", equalTo((1000 - depositFeeAmount) * 2))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((1000 - depositFeeAmount) * 2))
                .body("entry[0].cardholderFee.currency", equalTo(currency))
                .body("entry[0].cardholderFee.amount", equalTo(depositFeeAmount))
                .body("entry[0].transactionFee.currency", equalTo(currency))
                .body("entry[0].transactionFee.amount", equalTo(depositFeeAmount))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccountStatement_BackofficeCorporateImpersonator_Forbidden(){
        ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE),
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountStatement_BackofficeConsumerImpersonator_Forbidden(){
        ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER),
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
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
