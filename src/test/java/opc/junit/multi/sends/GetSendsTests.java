package opc.junit.multi.sends;

import com.google.common.collect.ImmutableMap;
import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import commons.enums.State;
import opc.junit.database.SendsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SENDS)
public class GetSendsTests extends BaseSendsSetup {

    private static String corporateAuthenticationTokenSource;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccountSource;
    private static Pair<String, CreateManagedCardModel> corporateManagedCardDestination;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountSource;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountDestination;
    private static final List<Pair<String, SendFundsModel>> corporateSendFunds = new ArrayList<>();
    private static final List<Pair<String, SendFundsModel>> consumerSendFunds = new ArrayList<>();
    private static String corporateAuthenticationTokenDestination;
    private static String consumerAuthenticationTokenDestination;
    private static String consumerCurrencyDestination;

    @BeforeAll
    public static void Setup() {
        corporateSetupSource();
        consumerSetupSource();
        corporateSetupDestination();
        consumerSetupDestination();

        corporateManagedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationTokenSource);
        corporateManagedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationTokenDestination);
        consumerManagedAccountSource = createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
        consumerManagedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencyDestination, consumerAuthenticationTokenDestination);
        corporateSends(corporateAuthenticationTokenSource, corporateAuthenticationTokenDestination);
        consumerDepositAndSendMaToMa(consumerAuthenticationToken);

        Collections.reverse(corporateSendFunds);
        Collections.reverse(consumerSendFunds);
    }

    @Test
    public void GetSends_Corporate_Success(){
        SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(corporateSendFunds.get(0).getLeft()))
                .body("send[1].id", equalTo(corporateSendFunds.get(1).getLeft()))
                .body("count", equalTo(corporateSendFunds.size()))
                .body("responseCount", equalTo(corporateSendFunds.size()));
    }

    @Test
    public void GetSends_Consumer_Success(){
        SendsService.getSends(secretKey, Optional.empty(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(consumerSendFunds.get(0).getLeft()))
                .body("send[1].id", equalTo(consumerSendFunds.get(1).getLeft()))
                .body("count", equalTo(consumerSendFunds.size()))
                .body("responseCount", equalTo(consumerSendFunds.size()));
    }

    @Test
    public void GetSends_WithAllFilters_Success(){
        final SendFundsModel expectedFunds =
                corporateSendFunds.get(0).getRight();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);
        filters.put("profileId", sendsProfileId);
        filters.put("source.id", expectedFunds.getSource().getId());
        filters.put("source.type", expectedFunds.getSource().getType());
        filters.put("state", Collections.singletonList(State.COMPLETED));
        filters.put("createdFrom", Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli());
        filters.put("createdTo", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        filters.put("tag", expectedFunds.getTag());

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(corporateSendFunds.get(0).getLeft()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetSends_LimitFilterCheck_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(corporateSendFunds.get(0).getLeft()))
                .body("count", equalTo(corporateSendFunds.size()))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetSends_FilterOutState_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.REJECTED));

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetSends_FilterByUnknownState_ReturnAll(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("state", Collections.singletonList(State.UNKNOWN));

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(corporateSendFunds.get(0).getLeft()))
                .body("send[1].id", equalTo(corporateSendFunds.get(1).getLeft()))
                .body("count", equalTo(corporateSendFunds.size()))
                .body("responseCount", equalTo(corporateSendFunds.size()));
    }

    @Test
    public void GetSends_FilterByMultipleStates_Success(){

        final CreateCorporateModel createCorporateModelSource = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporateSource = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelSource, secretKey);

        final CreateCorporateModel createCorporateModelDestination = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporateDestination = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination, secretKey);

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModelSource.getBaseCurrency(),
                corporateSource.getRight());

        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModelSource.getBaseCurrency(),
                corporateDestination.getRight());

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModelSource.getBaseCurrency(), 10000L);

        final List<State> sendStates = Arrays.asList(State.COMPLETED, State.REJECTED, State.INITIALISED, State.FAILED);

        sendStates.forEach(state -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(createCorporateModelSource.getBaseCurrency(), 100L))
                            .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                            .build();

            final String sendId = SendsService.sendFunds(sendFundsModel, secretKey, corporateSource.getRight(), Optional.empty())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("id");

            SendsDatabaseHelper.updateSendState(state.name(), sendId);
        });

        SendsService.getSends(secretKey, Optional.of(ImmutableMap.of("state", Arrays.asList(State.REJECTED, State.INITIALISED, State.FAILED))),
                        corporateSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));

        SendsService.getSends(secretKey, Optional.of(ImmutableMap.of("state", sendStates)),
                        corporateSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(sendStates.size()))
                .body("responseCount", equalTo(sendStates.size()));
    }

    @Test
    public void GetSends_FilterMultipleSends_Success(){

        final Map<String, Object> filters = new HashMap<>();
        filters.put("profileId", sendsProfileId);
        filters.put("source.id", consumerManagedAccountSource.getLeft());
        filters.put("source.type", MANAGED_ACCOUNTS.getValue());

        SendsService.getSends(secretKey, Optional.of(filters), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(consumerSendFunds.get(0).getLeft()))
                .body("send[1].id", equalTo(consumerSendFunds.get(1).getLeft()))
                .body("count", equalTo(consumerSendFunds.size()))
                .body("responseCount", equalTo(consumerSendFunds.size()));
    }

    @Test
    public void GetSends_FilterByPendingState_Success(){
        final CreateCorporateModel createCorporateModelSource = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.EUR.name())
                .build();
        final Pair<String, String> corporateSource = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelSource, secretKey);

        final CreateCorporateModel createCorporateModelDestination = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.EUR.name())
                .build();
        final Pair<String, String> corporateDestination = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination, secretKey);

        final Pair<String, CreateManagedAccountModel> managedAccount1 = createManagedAccount(corporateManagedAccountProfileId, createCorporateModelSource.getBaseCurrency(),
                corporateSource.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccount2 = createManagedAccount(corporateManagedAccountProfileId, createCorporateModelSource.getBaseCurrency(),
                corporateDestination.getRight());

        fundManagedAccount(managedAccount1.getLeft(), createCorporateModelSource.getBaseCurrency(), 10000L);

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(createCorporateModelSource.getBaseCurrency(), 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                AdminService.impersonateTenant(applicationOne.getInnovatorId(), AdminService.loginAdmin()), corporateSource.getLeft());

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(createCorporateModelSource.getBaseCurrency(), 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                AdminService.impersonateTenant(applicationOne.getInnovatorId(), AdminService.loginAdmin()), corporateDestination.getLeft());

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModelSource.getBaseCurrency(), 4000L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount1.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount2.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKey, corporateSource.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .getString("id");


        SendsService.getSends(secretKey, Optional.of(ImmutableMap.of("state", Collections.singletonList(State.PENDING))),
                        corporateSource.getRight())
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(sendId))
                .body("send[0].state", equalTo(State.PENDING.name()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetSends_FilterByAccountId_Success(){
        final List<Pair<String, SendFundsModel>> expectedSend =
                corporateSendFunds.stream().filter(x -> x.getRight().getSource().getType().equals("managed_accounts"))
                        .collect(Collectors.toList());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("source.id", expectedSend.get(0).getRight().getSource().getId());
        filters.put("source.type", MANAGED_ACCOUNTS.getValue());

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(expectedSend.get(0).getLeft()))
                .body("send[1].id", equalTo(expectedSend.get(1).getLeft()))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void GetSends_FilterByCardId_Success(){
        final Pair<String, SendFundsModel> expectedSend =
                corporateSendFunds.stream().filter(x -> x.getRight().getSource().getType().equals("managed_cards"))
                        .collect(Collectors.toList()).get(0);

        final Map<String, Object> filters = new HashMap<>();
        filters.put("source.id", expectedSend.getRight().getSource().getId());
        filters.put("source.type", MANAGED_CARDS.getValue());

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("send[0].id", equalTo(expectedSend.getLeft()))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetSends_NoEntries_Success(){
        final Map<String, Object> filters = new HashMap<>();
        filters.put("createdFrom", Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());

        SendsService.getSends(secretKey, Optional.of(filters), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetSends_InvalidApiKey_Unauthorised(){
        SendsService.getSends("abc", Optional.empty(), corporateAuthenticationTokenSource)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetSends_NoApiKey_BadRequest(){
        SendsService.getSends("", Optional.empty(), corporateAuthenticationTokenSource)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetSends_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        SendsService.getSends(secretKey, Optional.empty(), corporateAuthenticationTokenSource)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetSends_RootUserLoggedOut_Unauthorised(){
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        SendsService.getSends(secretKey, Optional.empty(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetSends_BackofficeCorporateImpersonation_Forbidden(){
        SendsService.getSends(secretKey, Optional.empty(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetSends_BackofficeConsumerImpersonation_Forbidden(){
        SendsService.getSends(secretKey, Optional.empty(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void corporateSends(final String tokenSource, String tokenDestination){

        fundManagedAccount(corporateManagedAccountSource.getLeft(), corporateCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(corporateCurrency, 300L))
                            .setSource(new ManagedInstrumentTypeId(corporateManagedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(corporateManagedCardDestination.getLeft(), MANAGED_CARDS))
                            .build();

            final String sendId = SendsService.sendFunds(sendFundsModel, secretKey, tokenSource, Optional.empty())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("id");

            corporateSendFunds.add(Pair.of(sendId, sendFundsModel));
        });

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrency, 100L))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedCardDestination.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(corporateManagedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKey, tokenDestination, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        corporateSendFunds.add(Pair.of(sendId, sendFundsModel));
    }

    private static void consumerDepositAndSendMaToMa(final String token){
        fundManagedAccount(consumerManagedAccountSource.getLeft(), consumerCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(consumerCurrency, 100L))
                            .setSource(new ManagedInstrumentTypeId(consumerManagedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(consumerManagedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String sendId = SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("id");

            consumerSendFunds.add(Pair.of(sendId, sendFundsModel));
        });
    }

    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetupSource() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(consumerCurrency)
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();
        consumerCurrencyDestination = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }
}
