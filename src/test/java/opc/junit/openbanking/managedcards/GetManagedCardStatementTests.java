package opc.junit.openbanking.managedcards;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import commons.enums.Currency;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.openbanking.BaseSetup;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.CurrencyAmount;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.openbanking.AccountInformationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetManagedCardStatementTests extends BaseSetup {

    private static String corporateConsent;
    private static String consumerConsent;
    private static Map<String, String> corporateHeaders;
    private static Map<String, String> consumerHeaders;

    private static List<ManagedCardDetails> corporatePrepaidManagedCards;
    private static List<ManagedCardDetails> consumerPrepaidManagedCards;
    private static String corporateDebitManagedCard;
    private static String consumerDebitManagedCard;
    private static CreateManagedAccountModel corporateCreateManagedAccountModel;
    private static CreateManagedAccountModel consumerCreateManagedAccountModel;
    private static Pair<String, Pair<Integer, Integer>> corporateManagedAccount;
    private static Pair<String, Pair<Integer, Integer>> consumerManagedAccount;

    private static Pair<Integer, Integer> corporateTransferValues;
    private static Pair<Integer, Integer> consumerTransferValues;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {

        corporateSetup();
        consumerSetup();

        corporateCreateManagedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, corporateCurrency).build();
        corporateManagedAccount = ManagedAccountsHelper.createFundedManagedAccount(corporateCreateManagedAccountModel, secretKey, corporateAuthenticationToken);

        consumerCreateManagedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, consumerCurrency).build();
        consumerManagedAccount = ManagedAccountsHelper.createFundedManagedAccount(consumerCreateManagedAccountModel, secretKey, consumerAuthenticationToken);

        corporatePrepaidManagedCards =
                ManagedCardsHelper
                        .createPrepaidManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrency, secretKey, corporateAuthenticationToken, 1);

        consumerPrepaidManagedCards =
                ManagedCardsHelper
                        .createPrepaidManagedCards(consumerPrepaidManagedCardsProfileId, consumerCurrency, secretKey, consumerAuthenticationToken, 1);

        corporateDebitManagedCard =
                ManagedCardsHelper
                        .createDebitManagedCard(corporateDebitManagedCardsProfileId, corporateManagedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        consumerDebitManagedCard =
                ManagedCardsHelper
                        .createDebitManagedCard(consumerDebitManagedCardsProfileId, consumerManagedAccount.getLeft(), secretKey, consumerAuthenticationToken);

        corporateTransferValues =
                TestHelper.transferFundsToCards(corporateManagedAccount.getLeft(),
                        transfersProfileId, corporatePrepaidManagedCards.stream().map(ManagedCardDetails::getManagedCardId).collect(Collectors.toList()),
                        corporateCurrency, secretKey, corporateAuthenticationToken);
        consumerTransferValues =
                TestHelper.transferFundsToCards(consumerManagedAccount.getLeft(),
                        transfersProfileId, consumerPrepaidManagedCards.stream().map(ManagedCardDetails::getManagedCardId).collect(Collectors.toList()),
                        consumerCurrency, secretKey, consumerAuthenticationToken);

        SimulatorHelper.simulateCardPurchaseById(secretKey, corporateDebitManagedCard, new CurrencyAmount(corporateCurrency, 1000L));
        SimulatorHelper.simulateCardPurchaseById(secretKey, consumerDebitManagedCard, new CurrencyAmount(consumerCurrency, 1000L));

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
    public void GetManagedCardStatement_CorporatePrepaid_Success(){

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, corporateCurrency,
                Pair.of(corporateManagedAccount.getLeft(), corporateCreateManagedAccountModel), corporateTransferValues);
    }

    @Test
    public void GetManagedCardStatement_ConsumerPrepaid_Success(){

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, consumerCurrency,
                Pair.of(consumerManagedAccount.getLeft(), consumerCreateManagedAccountModel), consumerTransferValues);
    }

    @Test
    public void GetManagedCardStatement_CorporateDebit_Success(){

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, corporateHeaders, corporateDebitManagedCard, Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulDebitResponse(response, corporateCurrency);
    }

    @Test
    public void GetManagedCardStatement_ConsumerDebit_Success(){

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, consumerHeaders, consumerDebitManagedCard, Optional.empty(), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulDebitResponse(response, consumerCurrency);
    }

    @Test
    public void GetManagedCardStatement_WithAllFilters_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("orderByTimestamp", "DESC");
        filters.put("fromTimestamp", Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("toTimestamp", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId(), Optional.of(filters), AcceptedResponse.JSON)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, consumerCurrency,
                Pair.of(consumerManagedAccount.getLeft(), consumerCreateManagedAccountModel), consumerTransferValues);
    }

    @Test
    public void GetManagedCardStatement_CsvAcceptedResponseCheck_Success(){

        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.CSV)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardStatement_NoAcceptHeader_DefaultToJsonSuccess(){
        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), Optional.empty())
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, corporateCurrency,
                Pair.of(corporateManagedAccount.getLeft(), corporateCreateManagedAccountModel), corporateTransferValues);
    }

    @Test
    public void GetManagedCardStatement_JsonInAcceptHeader_JsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/plain, */*");

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), Optional.of(headers))
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, corporateCurrency,
                Pair.of(corporateManagedAccount.getLeft(), corporateCreateManagedAccountModel), corporateTransferValues);
    }

    @Test
    public void GetManagedCardStatement_CsvInAcceptHeader_CsvSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/csv, text/plain, */*");

        AccountInformationService
                .getManagedCardStatement(sharedKey, consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), Optional.of(headers))
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardStatement_JsonAndCsvInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "application/json, text/csv");

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, consumerHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), Optional.of(headers))
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, consumerCurrency,
                Pair.of(consumerManagedAccount.getLeft(), consumerCreateManagedAccountModel), consumerTransferValues);
    }

    @Test
    public void GetManagedCardStatement_NoMatchInAcceptHeader_DefaultToJsonSuccess(){

        final Map<String, Object> headers = new HashMap<>();
        headers.put("accept", "text/plain, */*");

        final ValidatableResponse response =
                AccountInformationService
                        .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), Optional.of(headers))
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaidResponse(response, corporateCurrency,
                Pair.of(corporateManagedAccount.getLeft(), corporateCreateManagedAccountModel), corporateTransferValues);
    }

    @Test
    public void GetManagedCardStatement_NoEntries_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetManagedCardStatement_InvalidSharedKey_Unauthorised(){

        AccountInformationService
                .getManagedCardStatement("abc", corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardStatement_NoApiKey_BadRequest(){
        AccountInformationService
                .getManagedCardStatement("", corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCardStatement_DifferentInnovatorSharedKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("sharedKey");

        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardStatement_OtherProgrammeSharedKey_Forbidden(){

        AccountInformationService
                .getManagedCardStatement(applicationTwo.getSharedKey(), corporateHeaders, corporatePrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardStatement_RootUserLoggedOut_Unauthorised() throws Exception {

        final Pair<String, Map<String, String>> newCorporate = createCorporateWithConsentHeaders();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardsProfileId, Currency.getRandomCurrency().name(), secretKey, newCorporate.getLeft());

        OpenBankingSecureServiceHelper.logout(sharedKey, newCorporate.getLeft(), tppId);

        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, managedCardId, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCardStatement_CrossIdentityCheck_NotFound(){
        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, consumerPrepaidManagedCards.get(0).getManagedCardId(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardStatement_UnknownManagedCardId_NotFound() {

        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, RandomStringUtils.randomNumeric(18), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCardStatement_NoManagedCardId_NotFound() {
        AccountInformationService
                .getManagedCardStatement(sharedKey, corporateHeaders, "", Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void assertSuccessfulPrepaidResponse(final ValidatableResponse response,
                                                 final String currency,
                                                 final Pair<String, CreateManagedAccountModel> managedAccount,
                                                 final Pair<Integer, Integer> transferValues){
        response.body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(currency))
                .body("entry[0].transactionAmount.amount", equalTo(transferValues.getLeft()))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(transferValues.getLeft()))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(transferValues.getLeft()))
                .body("entry[0].balanceAfter.currency", equalTo(currency))
                .body("entry[0].balanceAfter.amount", equalTo(transferValues.getLeft()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency))
                .body("entry[0].availableBalanceAfter.amount", equalTo(transferValues.getLeft()))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency))
                .body("entry[0].actualBalanceAfter.amount", equalTo(transferValues.getLeft()))
                .body("entry[0].cardholderFee.currency", equalTo(currency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].transactionFee.currency", equalTo(currency))
                .body("entry[0].transactionFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].additionalFields.sourceInstrumentType", equalTo("managed_accounts"))
                .body("entry[0].additionalFields.sourceInstrumentId", equalTo(managedAccount.getLeft()))
                .body("entry[0].additionalFields.sourceInstrumentFriendlyName", equalTo(managedAccount.getRight().getFriendlyName()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    private void assertSuccessfulDebitResponse(final ValidatableResponse response,
                                               final String currency){
        response.body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("AUTHORISATION_DECLINE"))
                .body("entry[0].transactionAmount.currency", equalTo(currency))
                .body("entry[0].transactionAmount.amount", equalTo(-1000))
                .body("entry[0].cardholderFee.currency", equalTo(currency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].transactionFee.currency", equalTo(currency))
                .body("entry[0].transactionFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.forexFeeAmount", equalTo("0"))
                .body("entry[0].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[0].additionalFields.forexFeeCurrency", equalTo(currency))
                .body("entry[0].additionalFields.forexPaddingCurrency", equalTo(currency))
                .body("entry[0].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }
}
