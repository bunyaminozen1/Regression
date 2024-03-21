package opc.junit.backoffice.transfers;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.IdentityType;
import commons.enums.State;
import opc.junit.database.TransfersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetTransfersTests extends BaseTransfersSetup{
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedCardModel> corporateManagedCard;
    private static List<Pair<String, CreateManagedAccountModel>> consumerManagedAccounts;
    private static final List<Pair<String, TransferFundsModel>> corporateTransferFunds = new ArrayList<>();
    private static final List<Pair<String, TransferFundsModel>> consumerTransferFunds = new ArrayList<>();
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        corporateManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken, 2);
        corporateDepositAndTransferMaToMc(corporateAuthenticationToken);
        consumerDepositAndTransferMaToMa(consumerAuthenticationToken);

        Collections.reverse(corporateTransferFunds);
        Collections.reverse(consumerTransferFunds);
        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
    }

    @Test
    public void GetTransfers_Corporate_Success(){
        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(corporateTransferFunds.get(0).getLeft()))
                .body("transfer[1].id", equalTo(corporateTransferFunds.get(1).getLeft()))
                .body("count", equalTo(corporateTransferFunds.size()))
                .body("responseCount", equalTo(corporateTransferFunds.size()));
    }

    @Test
    public void GetTransfers_Consumer_Success(){
        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), consumerImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(consumerTransferFunds.get(0).getLeft()))
                .body("transfer[1].id", equalTo(consumerTransferFunds.get(1).getLeft()))
                .body("count", equalTo(consumerTransferFunds.size()))
                .body("responseCount", equalTo(consumerTransferFunds.size()));
    }

    @Test
    public void GetTransfers_WithAllFilters_Success(){
        final TransferFundsModel expectedFunds =
                corporateTransferFunds.get(0).getRight();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("profileId", transfersProfileId);
        filters.put("instrumentId", expectedFunds.getSource().getId());
        filters.put("state", Collections.singletonList(State.COMPLETED));
        filters.put("createdFrom", Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("tag", expectedFunds.getTag());

        BackofficeMultiService.getTransfers(secretKey, Optional.of(filters), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(corporateTransferFunds.get(0).getLeft()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetTransfers_LimitFilterCheck_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);

        BackofficeMultiService.getTransfers(secretKey, Optional.of(filters), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(corporateTransferFunds.get(0).getLeft()))
                .body("count", equalTo(corporateTransferFunds.size()))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetTransfers_FilterOutState_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.REJECTED));

        BackofficeMultiService.getTransfers(secretKey, Optional.of(filters), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetTransfers_FilterByUnknownState_ReturnAll(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.UNKNOWN));

        BackofficeMultiService.getTransfers(secretKey, Optional.of(filters), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(corporateTransferFunds.get(0).getLeft()))
                .body("transfer[1].id", equalTo(corporateTransferFunds.get(1).getLeft()))
                .body("count", equalTo(corporateTransferFunds.size()))
                .body("responseCount", equalTo(corporateTransferFunds.size()));
    }
    @Test
    public void GetTransfers_FilterByMultipleStates_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final String authCorporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporate.getLeft(), IdentityType.CORPORATE, secretKey);

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), 10000L);

        final List<State> transferStates = Arrays.asList(State.COMPLETED, State.REJECTED, State.FAILED);

        transferStates.forEach(state -> {
            final TransferFundsModel transferFundsModel =
                    TransferFundsModel.newBuilder()
                            .setProfileId(transfersProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), 100L))
                            .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                            .build();

            final String transferId = TransfersService.transferFunds(transferFundsModel, secretKey, corporate.getRight(), Optional.empty())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("id");

            TransfersDatabaseHelper.updateTransferState(state.name(), transferId);
        });

        BackofficeMultiService.getTransfers(secretKey, Optional.of(ImmutableMap.of("state", Arrays.asList(State.REJECTED, State.INITIALISED, State.FAILED))),
                        authCorporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        BackofficeMultiService.getTransfers(secretKey, Optional.of(ImmutableMap.of("state", transferStates)),
                        authCorporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(transferStates.size()))
                .body("responseCount", equalTo(transferStates.size()));
    }

    @Test
    public void GetTransfers_FilterMultipleTransfers_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("profileId", transfersProfileId);
        filters.put("instrumentId", consumerManagedAccounts.get(0).getLeft());

        BackofficeMultiService.getTransfers(secretKey, Optional.of(filters), consumerImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("transfer[0].id", equalTo(consumerTransferFunds.get(0).getLeft()))
                .body("transfer[1].id", equalTo(consumerTransferFunds.get(1).getLeft()))
                .body("count", equalTo(consumerTransferFunds.size()))
                .body("responseCount", equalTo(consumerTransferFunds.size()));
    }

    @Test
    public void GetTransfers_NoEntries_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("createdFrom", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        BackofficeMultiService.getTransfers(secretKey, Optional.of(filters), corporateImpersonateToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetTransfers_InvalidApiKey_Unauthorised(){
        BackofficeMultiService.getTransfers("abc", Optional.empty(), corporateImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetTransfers_NoApiKey_BadRequest(){
        BackofficeMultiService.getTransfers("", Optional.empty(), corporateImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetTransfers_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), corporateImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfers_RootUserLoggedOut_Forbidden(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), token)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfers_CorporateAuthToken_Forbidden(){
        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfers_ConsumerAuthToken_Forbidden(){
        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetTransfers_CorporateUserAuthToken_Forbidden(){
        BackofficeMultiService.getTransfers(secretKey, Optional.empty(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void corporateDepositAndTransferMaToMc(final String token){
        fundManagedAccount(corporateManagedAccount.getLeft(), corporateCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final TransferFundsModel transferFundsModel =
                    TransferFundsModel.newBuilder()
                            .setProfileId(transfersProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(corporateCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(corporateManagedCard.getLeft(), MANAGED_CARDS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String transferId =
                    TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            corporateTransferFunds.add(Pair.of(transferId, transferFundsModel));
        });
    }

    private static void consumerDepositAndTransferMaToMa(final String token){
        fundManagedAccount(consumerManagedAccounts.get(0).getLeft(), consumerCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final TransferFundsModel transferFundsModel =
                    TransferFundsModel.newBuilder()
                            .setProfileId(transfersProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(consumerCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(consumerManagedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(consumerManagedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String transferId =
                    TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            consumerTransferFunds.add(Pair.of(transferId, transferFundsModel));
        });
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
