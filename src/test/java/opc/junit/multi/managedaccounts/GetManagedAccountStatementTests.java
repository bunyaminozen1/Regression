package opc.junit.multi.managedaccounts;

import commons.enums.Currency;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import opc.enums.opc.AcceptedResponse;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.TransfersService;
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
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedAccountStatementTests extends BaseManagedAccountsSetup {
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccount;
    private static final List<Pair<String, TransferFundsModel>> corporateTransferFunds = new ArrayList<>();

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(corporateManagedAccount.getLeft(), secretKey, corporateAuthenticationToken);
        ManagedAccountsHelper.assignManagedAccountIban(consumerManagedAccount.getLeft(), secretKey, consumerAuthenticationToken);

        deposit(corporateManagedAccount.getLeft(), corporateManagedAccount.getRight().getCurrency(), corporateAuthenticationToken);
        deposit(consumerManagedAccount.getLeft(), consumerManagedAccount.getRight().getCurrency(), consumerAuthenticationToken);
    }

    @Test
    public void GetManagedAccountStatement_Corporate_Success(){
        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_Consumer_Success(){
        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
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

        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency);
    }

    @Test
    public void GetManagedAccountStatement_showFundMovementsOnlyFilter_Success(){

        final CreateCorporateModel createCorporateModel =
            CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();
        final String corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final String adminTenantImpersonationToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());
        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(corporateCurrency, 3000L),
            Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                new CurrencyAmount(Currency.USD.name(), 2020L)),
            adminTenantImpersonationToken, corporateId);

        final String corporateManagedAccount =
            createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken).getLeft();

        ManagedAccountsHelper.assignManagedAccountIban(corporateManagedAccount, secretKey, corporateAuthenticationToken);

        TestHelper.simulateDepositInStatePending(corporateManagedAccount, new CurrencyAmount(corporateCurrency, 4000L),
             secretKey, corporateAuthenticationToken, 4000 - TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue(), 0);

        final Map<String, Object> showFundMovementsOnlyFilter = new HashMap<>();
        showFundMovementsOnlyFilter.put("showFundMovementsOnly", true);

        final Map<String, Object> doNotShowFundMovementsOnlyFilter = new HashMap<>();
        doNotShowFundMovementsOnlyFilter.put("showFundMovementsOnly", false);

        ManagedAccountsService
            .getManagedAccountStatement(secretKey, corporateManagedAccount, corporateAuthenticationToken, Optional.of(showFundMovementsOnlyFilter), AcceptedResponse.JSON)
            .then()
            .statusCode(SC_OK)
            .body("count", equalTo(0));

        ManagedAccountsService
            .getManagedAccountStatement(secretKey, corporateManagedAccount, corporateAuthenticationToken, Optional.of(doNotShowFundMovementsOnlyFilter), AcceptedResponse.JSON)
            .then()
            .statusCode(SC_OK)
            .body("count", equalTo(1));
    }

    @Test
    public void GetManagedAccountStatement_CsvAcceptedResponseCheck_Success(){
        // TODO check csv items in response
        ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.CSV)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_NoAcceptHeader_DefaultToJsonSuccess(){
        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), Optional.empty())
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_JsonInAcceptHeader_JsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/plain, */*");

        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_CsvInAcceptHeader_CsvSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/csv, text/plain, */*");

        // TODO check csv items in response
        ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_JsonAndCsvInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/csv");

        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_NoMatchInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/plain, */*");

        assertSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK), corporateCurrency);
    }

    @Test
    public void GetManagedAccountStatement_FilterFromMultipleEntries_Success() {

        final Long latestTimestamp =
                ManagedAccountsService
                        .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken,
                                Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("entry[0].processedTimestamp");

        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", latestTimestamp);

        assertSingleSuccessfulResponse(ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK), consumerCurrency);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ASC", "DESC"})
    public void GetManagedAccountStatement_CheckOrdering_Success(final String order){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("orderByTimestamp", order);

        final List<Long> timestamps = new ArrayList<>();

        final JsonPath response = ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
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

        ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedAccountStatement_AccountNotActive_Success(){

        final String managedAccountId =
                createPendingApprovalManagedAccount(consumerManagedAccountProfileId, consumerAuthenticationToken).getLeft();

        ManagedAccountsService
                .getManagedAccountStatement(secretKey, managedAccountId, consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_InvalidApiKey_Unauthorised(){
        ManagedAccountsService
                .getManagedAccountStatement("abc", corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountStatement_NoApiKey_BadRequest(){
        ManagedAccountsService
                .getManagedAccountStatement("", corporateManagedAccount.getLeft(), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccountStatement_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountStatement_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), token, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountStatement_CrossIdentityCheck_NotFound(){
        ManagedAccountsService
                .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccountStatement_InvalidManagedAccountId_BadRequest(){
        ManagedAccountsService
                .getManagedAccountStatement(secretKey, RandomStringUtils.randomAlphanumeric(50), corporateAuthenticationToken, Optional.empty(), AcceptedResponse.JSON)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccountStatement_PdfAcceptedResponseCheck_Success(){

        ManagedAccountsService
                .getManagedAccountStatement(secretKey, consumerManagedAccount.getLeft(), consumerAuthenticationToken, Optional.empty(), AcceptedResponse.PDF)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedAccountStatement_TransferDescription_Success() {
        final CreateCorporateModel createCorporateModel =
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        final String corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
            createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);

        final Pair<String, CreateManagedCardModel> corporateManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        corporateDepositAndTransferMaToMc(corporateManagedAccount.getLeft(),corporateManagedCard.getLeft(), corporateAuthenticationToken);

        ManagedAccountsService
            .getManagedAccountStatement(secretKey, corporateManagedAccount.getLeft(), corporateAuthenticationToken,Optional.empty(), AcceptedResponse.JSON)
            .then()
            .statusCode(SC_OK)
            .body("entry[0].additionalFields.description",  notNullValue())
            .body("count", equalTo(3))
            .body("responseCount", equalTo(3));
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

    private static void corporateDepositAndTransferMaToMc(final String managedAccountId, final String managedCardId, final String token){
        fundManagedAccount(managedAccountId, corporateCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                    .setProfileId(transfersProfileId)
                    .setTag(RandomStringUtils.randomAlphabetic(5))
                    .setDestinationAmount(new CurrencyAmount(corporateCurrency, 100L))
                    .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                    .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                    .setDescription(RandomStringUtils.randomAlphabetic(10))
                    .build();

            final String id =
                TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("id");

            corporateTransferFunds.add(Pair.of(id, transferFundsModel));
        });
    }
}
