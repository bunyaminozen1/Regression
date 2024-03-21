package opc.junit.openbanking.managedaccounts;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.services.innovator.InnovatorService;
import opc.services.openbanking.AccountInformationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedAccountStatementTests extends BaseSetup {

    private static final int DEFAULT_DEPOSIT_AMOUNT = 10000;

    private static String corporateConsent;
    private static String consumerConsent;
    private static Map<String, String> corporateHeaders;
    private static Map<String, String> consumerHeaders;
    private static Pair<String, Pair<Integer, Integer>> corporateManagedAccount;
    private static Pair<String, Pair<Integer, Integer>> consumerManagedAccount;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {

        corporateSetup();
        consumerSetup();

        corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);

        corporateManagedAccount = ManagedAccountsHelper.createFundedManagedAccount(corporateManagedAccountProfileId,
                createCorporateModel.getBaseCurrency(), secretKey, corporateAuthenticationToken);
        consumerManagedAccount = ManagedAccountsHelper.createFundedManagedAccount(consumerManagedAccountProfileId,
                createConsumerModel.getBaseCurrency(), secretKey, consumerAuthenticationToken);
    }

    @BeforeEach
    public void Setup() throws Exception {
        corporateHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(corporateConsent)));
        consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsent)));
    }

    @Test
    public void GetManagedAccountStatement_Corporate_Success(){

        final ValidatableResponse response =
                AccountInformationService.getManagedAccountStatement(sharedKey, corporateHeaders,
                        corporateManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, corporateCurrency, corporateManagedAccount.getRight());
    }

    @Test
    public void GetManagedAccountStatement_Consumer_Success(){

        final ValidatableResponse response =
                AccountInformationService.getManagedAccountStatement(sharedKey, consumerHeaders,
                        consumerManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, consumerCurrency, consumerManagedAccount.getRight());
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

        assertSuccessfulResponse(AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(), Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency, consumerManagedAccount.getRight());
    }

    @Test
    public void GetManagedAccountStatement_CsvAcceptedResponseCheck_Success(){
        // TODO check csv items in response
        AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.CSV)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_NoAcceptHeader_DefaultToJsonSuccess(){
        assertSuccessfulResponse(AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, corporateManagedAccount.getLeft(), Optional.empty(), Optional.empty())
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount.getRight());
    }

    @Test
    public void GetManagedAccountStatement_JsonInAcceptHeader_JsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/plain, */*");

        assertSuccessfulResponse(AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, corporateManagedAccount.getLeft(), Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount.getRight());
    }

    @Test
    public void GetManagedAccountStatement_CsvInAcceptHeader_CsvSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/csv, text/plain, */*");

        // TODO check csv items in response
        AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(), Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_JsonAndCsvInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/csv");

        assertSuccessfulResponse(AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, corporateManagedAccount.getLeft(), Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount.getRight());
    }

    @Test
    public void GetManagedAccountStatement_NoMatchInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/plain, */*");

        assertSuccessfulResponse(AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, corporateManagedAccount.getLeft(), Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency, corporateManagedAccount.getRight());
    }

    @Test
    public void GetManagedAccountStatement_FilterFromMultipleEntries_Success() {

        final Long latestTimestamp =
                AccountInformationService
                        .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(),
                                Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("entry[0].processedTimestamp");

        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", latestTimestamp);

        assertSingleSuccessfulResponse(AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(), Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ASC", "DESC"})
    public void GetManagedAccountStatement_CheckOrdering_Success(final String order){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("orderByTimestamp", order);

        final List<Long> timestamps = new ArrayList<>();

        final JsonPath response = AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, corporateManagedAccount.getLeft(), Optional.of(filters), AcceptedResponse.JSON)
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

        AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, corporateManagedAccount.getLeft(), Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccountStatement_AccountNotActive_Success(){

        final String managedAccountId =
                ManagedAccountsHelper.createPendingApprovalManagedAccount(consumerManagedAccountProfileId, secretKey, consumerAuthenticationToken);

        AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, managedAccountId, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_InvalidSharedKey_Unauthorised(){
        AccountInformationService
                .getManagedAccountStatement("abc", corporateHeaders, corporateManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountStatement_NoSharedKey_BadRequest(){
        AccountInformationService
                .getManagedAccountStatement("", corporateHeaders, corporateManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccountStatement_DifferentInnovatorSharedKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("sharedKey");

        AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountStatement_OtherProgrammeSharedKey_Forbidden(){

        AccountInformationService
                .getManagedAccountStatement(applicationTwo.getSharedKey(), consumerHeaders, consumerManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountStatement_RootUserLoggedOut_Unauthorised() throws Exception {

        final Pair<String, Map<String, String>> newCorporate = createCorporateWithConsentHeaders();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, Currency.getRandomCurrency().name(), secretKey, newCorporate.getLeft());

        OpenBankingSecureServiceHelper.logout(sharedKey, newCorporate.getLeft(), tppId);

        AccountInformationService
                .getManagedAccountStatement(sharedKey, newCorporate.getRight(), managedAccountId, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountStatement_CrossIdentityCheck_NotFound(){
        AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, corporateManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccountStatement_InvalidManagedAccountId_BadRequest(){
        AccountInformationService
                .getManagedAccountStatement(sharedKey, corporateHeaders, RandomStringUtils.randomAlphanumeric(50), Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccountStatement_PdfAcceptedResponseCheck_Success(){

        AccountInformationService
                .getManagedAccountStatement(sharedKey, consumerHeaders, consumerManagedAccount.getLeft(), Optional.empty(), AcceptedResponse.PDF)
                .then()
                .statusCode(SC_OK);
    }

    private void assertSingleSuccessfulResponse(final ValidatableResponse response,
                                                final String currency){
        final int depositFeeAmount = TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        response.body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[0].transactionAmount.currency", equalTo(currency))
                .body("entry[0].transactionAmount.amount", equalTo((DEFAULT_DEPOSIT_AMOUNT)))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(DEFAULT_DEPOSIT_AMOUNT - depositFeeAmount))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(DEFAULT_DEPOSIT_AMOUNT - depositFeeAmount))
                .body("entry[0].balanceAfter.currency", equalTo(currency))
                .body("entry[0].balanceAfter.amount", equalTo(DEFAULT_DEPOSIT_AMOUNT - depositFeeAmount))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(DEFAULT_DEPOSIT_AMOUNT - depositFeeAmount))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(DEFAULT_DEPOSIT_AMOUNT - depositFeeAmount))
                .body("entry[0].cardholderFee.currency", equalTo(currency))
                .body("entry[0].cardholderFee.amount", equalTo(depositFeeAmount))
                .body("entry[0].transactionFee.currency", equalTo(currency))
                .body("entry[0].transactionFee.amount", equalTo(depositFeeAmount))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final String currency,
                                          final Pair<Integer, Integer> depositDetails){
        
        response.body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[0].transactionAmount.currency", equalTo(currency))
                .body("entry[0].transactionAmount.amount", equalTo((depositDetails.getLeft() + depositDetails.getRight())))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(depositDetails.getLeft()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(depositDetails.getLeft()))
                .body("entry[0].balanceAfter.currency", equalTo(currency))
                .body("entry[0].balanceAfter.amount", equalTo(depositDetails.getLeft()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(depositDetails.getLeft()))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(depositDetails.getLeft()))
                .body("entry[0].cardholderFee.currency", equalTo(currency))
                .body("entry[0].cardholderFee.amount", equalTo(depositDetails.getRight()))
                .body("entry[0].transactionFee.currency", equalTo(currency))
                .body("entry[0].transactionFee.amount", equalTo(depositDetails.getRight()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }
}
