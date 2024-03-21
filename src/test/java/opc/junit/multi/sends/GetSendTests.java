package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

@Tag(MultiTags.SENDS)
public class GetSendTests extends BaseSendsSetup {

    private static String corporateAuthenticationTokenSource;
    private static String consumerAuthenticationTokenSource;
    private static String corporateCurrencySource;
    private static String consumerCurrencySource;
    private static String corporateIdSource;
    private static String consumerIdSource;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccountSource;
    private static Pair<String, CreateManagedCardModel> corporateManagedCardDestination;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountSource;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccountDestination;
    private static final List<Pair<String, SendFundsModel>> corporateSendFunds = new ArrayList<>();
    private static final List<Pair<String, SendFundsModel>> consumerSendFunds = new ArrayList<>();
    private static String corporateAuthenticationTokenDestination;
    private static String consumerAuthenticationTokenDestination;

    @BeforeAll
    public static void Setup() {
        corporateSetupSource();
        consumerSetupSource();

        corporateSetupDestination();
        consumerSetupDestination();

        corporateManagedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        corporateManagedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);
        consumerManagedAccountSource = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        consumerManagedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        corporateDepositAndSendMaToMc(corporateAuthenticationTokenSource);
        consumerDepositAndSendMaToMa(consumerAuthenticationTokenSource);
    }

    @Test
    public void GetSend_Corporate_Success(){

        final Pair<String, SendFundsModel> send = corporateSendFunds.get(0);

        SendsService.getSend(secretKey, send.getLeft(), corporateAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(send.getLeft()))
                .body("profileId", equalTo(sendsProfileId))
                .body("tag", equalTo(send.getRight().getTag()))
                .body("source.type", equalTo(send.getRight().getSource().getType()))
                .body("source.id", equalTo(send.getRight().getSource().getId()))
                .body("destination.type", equalTo(send.getRight().getDestination().getType()))
                .body("destination.id", equalTo(send.getRight().getDestination().getId()))
                .body("destinationAmount.currency", equalTo(send.getRight().getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(send.getRight().getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    public void GetSend_Consumer_Success(){

        final Pair<String, SendFundsModel> send = consumerSendFunds.get(0);

        SendsService.getSend(secretKey, send.getLeft(), consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(send.getLeft()))
                .body("profileId", equalTo(sendsProfileId))
                .body("tag", equalTo(send.getRight().getTag()))
                .body("source.type", equalTo(send.getRight().getSource().getType()))
                .body("source.id", equalTo(send.getRight().getSource().getId()))
                .body("destination.type", equalTo(send.getRight().getDestination().getType()))
                .body("destination.id", equalTo(send.getRight().getDestination().getId()))
                .body("destinationAmount.currency", equalTo(send.getRight().getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(send.getRight().getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    public void GetSend_SendInPendingState_Success(){

        final CreateCorporateModel createCorporateModelSource = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.EUR.name())
                .build();
        final Pair<String, String> corporateSource = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelSource, secretKey);

        final CreateCorporateModel createCorporateModelDestination = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.EUR.name())
                .build();
        final Pair<String, String> corporateDestination = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination, secretKey);

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModelSource.getBaseCurrency(), corporateSource.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModelSource.getBaseCurrency(), corporateDestination.getRight());

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModelSource.getBaseCurrency(), 10000L);

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
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
                        .build();

        final String sendId =
                SendsService.sendFunds(sendFundsModel, secretKey, corporateSource.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .getString("id");

        SendsService.getSend(secretKey, sendId, corporateDestination.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(sendId))
                .body("profileId", equalTo(sendsProfileId))
                .body("tag", equalTo(sendFundsModel.getTag()))
                .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("PENDING"))
                .body("creationTimestamp", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    public void GetSend_UnknownSend_NotFound(){

        SendsService.getSend(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetSend_InvalidSend_BadRequest(){

        SendsService.getSend(secretKey, RandomStringUtils.randomAlphanumeric(6), consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetSend_InvalidApiKey_Unauthorised(){
        SendsService.getSend("abc", consumerManagedAccountSource.getLeft(), consumerAuthenticationTokenSource)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetSend_NoApiKey_BadRequest(){
        SendsService.getSend("", consumerManagedAccountSource.getLeft(), consumerAuthenticationTokenSource)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetSend_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        SendsService.getSend(secretKey, corporateManagedAccountSource.getLeft(), corporateAuthenticationTokenSource)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetSend_RootUserLoggedOut_Unauthorised(){
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        SendsService.getSend(secretKey, consumerManagedAccountSource.getLeft(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetSend_CrossIdentityChecks_NotFound(){

        SendsService.getSend(secretKey, corporateSendFunds.get(0).getLeft(), consumerAuthenticationTokenSource)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetSend_BackofficeCorporateImpersonator_Forbidden(){

        SendsService.getSend(secretKey, corporateSendFunds.get(0).getLeft(), getBackofficeImpersonateToken(corporateIdSource, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetSend_BackofficeConsumerImpersonator_Forbidden(){

        SendsService.getSend(secretKey, consumerSendFunds.get(0).getLeft(), getBackofficeImpersonateToken(consumerIdSource, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void corporateDepositAndSendMaToMc(final String token){
        fundManagedAccount(corporateManagedAccountSource.getLeft(), corporateCurrencySource, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 100L))
                            .setSource(new ManagedInstrumentTypeId(corporateManagedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(corporateManagedCardDestination.getLeft(), MANAGED_CARDS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String id =
                    SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            corporateSendFunds.add(Pair.of(id, sendFundsModel));
        });
    }

    private static void consumerDepositAndSendMaToMa(final String token){
        fundManagedAccount(consumerManagedAccountSource.getLeft(), consumerCurrencySource, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final SendFundsModel sendFundsModel =
                    SendFundsModel.newBuilder()
                            .setProfileId(sendsProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, 100L))
                            .setSource(new ManagedInstrumentTypeId(consumerManagedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(consumerManagedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build();

            final String id =
                    SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            consumerSendFunds.add(Pair.of(id, sendFundsModel));
        });
    }

    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerIdSource = authenticatedConsumer.getLeft();
        consumerAuthenticationTokenSource = authenticatedConsumer.getRight();
        consumerCurrencySource = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerIdSource);
    }

    private static void corporateSetupSource() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateIdSource = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrencySource = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateIdSource);
    }
    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }
}
